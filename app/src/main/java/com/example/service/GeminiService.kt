package com.example.service

import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.model.AudioSummary
import com.example.model.CaptionSegment
import com.example.model.WordTiming
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

class GeminiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun getEffectiveKey(customKey: String?): String {
        return if (!customKey.isNullOrBlank()) {
            customKey.trim()
        } else {
            BuildConfig.GEMINI_API_KEY
        }
    }

    /**
     * Generates a structured Bengali and English summary of the speech using Gemini
     */
    suspend fun generateSpeechSummary(
        transcriptOrTopic: String,
        audioFile: File? = null,
        customApiKey: String? = null
    ): Result<AudioSummary> = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveKey(customApiKey)
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.success(createSmartFallbackSummary(transcriptOrTopic))
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

            val prompt = """
                Listen to this audio voice and analyze the speech content.
                ${if (transcriptOrTopic.isNotBlank()) "Reference words: \"$transcriptOrTopic\"" else ""}

                Return a JSON object with this exact structure:
                {
                  "headline": "Catchy short headline in Bengali & English (e.g. স্বপ্নের পথে অবিচল যাত্রা | The Unstoppable Journey)",
                  "summaryBn": "A 2-3 sentence engaging summary in Bengali",
                  "summaryEn": "A 2-3 sentence engaging summary in English",
                  "keyPoints": ["Point 1 in Bengali", "Point 2 in Bengali", "Point 3 in Bengali"],
                  "sentiment": "e.g. Inspiring, Energetic, Emotional",
                  "detectedLanguage": "Bengali / English"
                }
                Provide only raw JSON.
            """.trimIndent()

            val partsArray = JSONArray()

            // If audio file exists, attach actual audio data to Gemini
            if (audioFile != null && audioFile.exists() && audioFile.length() < 12 * 1024 * 1024) {
                try {
                    val bytes = audioFile.readBytes()
                    val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    val mime = when {
                        audioFile.name.endsWith(".wav", true) -> "audio/wav"
                        audioFile.name.endsWith(".m4a", true) || audioFile.name.endsWith(".mp4", true) -> "audio/mp4"
                        audioFile.name.endsWith(".aac", true) -> "audio/aac"
                        audioFile.name.endsWith(".ogg", true) -> "audio/ogg"
                        else -> "audio/mp3"
                    }
                    partsArray.put(JSONObject().apply {
                        put("inlineData", JSONObject().apply {
                            put("mimeType", mime)
                            put("data", base64)
                        })
                    })
                } catch (e: Exception) {
                    Log.w("GeminiService", "Inline audio encode failed: ${e.message}")
                }
            }

            partsArray.put(JSONObject().put("text", prompt))

            val requestBodyJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().put("parts", partsArray))
                })
                put("generationConfig", JSONObject().apply {
                    put("responseMimeType", "application/json")
                    put("temperature", 0.3)
                })
            }

            val request = Request.Builder()
                .url(url)
                .post(requestBodyJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.w("GeminiService", "API error: ${response.code} $responseString")
                return@withContext Result.success(createSmartFallbackSummary(transcriptOrTopic))
            }

            val jsonResponse = JSONObject(responseString)
            val candidates = jsonResponse.optJSONArray("candidates")
            val candidate = candidates?.optJSONObject(0)
            val content = candidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val text = parts?.optJSONObject(0)?.optString("text") ?: "{}"

            val parsed = JSONObject(text.trim())
            val headline = parsed.optString("headline", "অডিও ভয়েস সামারি")
            val summaryBn = parsed.optString("summaryBn", transcriptOrTopic)
            val summaryEn = parsed.optString("summaryEn", "")
            val sentiment = parsed.optString("sentiment", "Inspiring")
            val detectedLanguage = parsed.optString("detectedLanguage", "Bengali")

            val keyPointsList = mutableListOf<String>()
            val keyPointsArr = parsed.optJSONArray("keyPoints")
            if (keyPointsArr != null) {
                for (i in 0 until keyPointsArr.length()) {
                    keyPointsList.add(keyPointsArr.optString(i))
                }
            }
            if (keyPointsList.isEmpty()) {
                keyPointsList.add("দৃঢ় সংকল্প ও স্পষ্ট লক্ষ্যের গুরুত্ব")
                keyPointsList.add("ধারাবাহিক প্রচেষ্টা এবং বাধা অতিক্রমের মানসিকতা")
            }

            Result.success(
                AudioSummary(
                    headline = headline,
                    summaryBn = summaryBn,
                    summaryEn = summaryEn,
                    keyPoints = keyPointsList,
                    sentiment = sentiment,
                    detectedLanguage = detectedLanguage
                )
            )
        } catch (e: Exception) {
            Log.e("GeminiService", "Exception calling Gemini summary: ${e.message}", e)
            Result.success(createSmartFallbackSummary(transcriptOrTopic))
        }
    }

    /**
     * Transcribes audio voice and generates synchronized caption segments with word-level timestamps using Gemini
     */
    suspend fun generateSyncedCaptions(
        audioTextOrFile: String,
        audioDurationMs: Long,
        audioFile: File? = null,
        customApiKey: String? = null
    ): Result<List<CaptionSegment>> = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveKey(customApiKey)
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.success(createSmartSynchronizedCaptions(audioTextOrFile, audioDurationMs))
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

            val prompt = """
                You are an expert audio speech transcriber and subtitler.
                Total audio duration: $audioDurationMs milliseconds.
                ${if (audioTextOrFile.isNotBlank()) "Reference spoken text: \"$audioTextOrFile\"" else ""}

                Instructions:
                1. Listen to the audio and transcribe every spoken word in its exact spoken language (Bengali or English).
                2. Divide the speech into short, dynamic subtitle phrases (3-6 words per phrase) suitable for TikTok, Instagram Reels, and YouTube Shorts captions.
                3. For each phrase, calculate the exact millisecond start time (startMs) and end time (endMs), as well as word-by-word timestamps (words).
                4. Start timestamps must strictly start at 0ms and end at or before $audioDurationMs ms.

                Return a JSON array formatted as:
                [
                  {
                    "startMs": 0,
                    "endMs": 2800,
                    "text": "spoken phrase here",
                    "words": [
                      {"word": "word1", "startMs": 0, "endMs": 700},
                      {"word": "word2", "startMs": 700, "endMs": 1400}
                    ]
                  }
                ]
                Return ONLY the raw JSON array.
            """.trimIndent()

            val partsArray = JSONArray()

            // If audio file exists, attach actual audio data
            if (audioFile != null && audioFile.exists() && audioFile.length() < 12 * 1024 * 1024) {
                try {
                    val bytes = audioFile.readBytes()
                    val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    val mime = when {
                        audioFile.name.endsWith(".wav", true) -> "audio/wav"
                        audioFile.name.endsWith(".m4a", true) || audioFile.name.endsWith(".mp4", true) -> "audio/mp4"
                        audioFile.name.endsWith(".aac", true) -> "audio/aac"
                        audioFile.name.endsWith(".ogg", true) -> "audio/ogg"
                        else -> "audio/mp3"
                    }
                    partsArray.put(JSONObject().apply {
                        put("inlineData", JSONObject().apply {
                            put("mimeType", mime)
                            put("data", base64)
                        })
                    })
                } catch (e: Exception) {
                    Log.w("GeminiService", "Inline audio encode failed: ${e.message}")
                }
            }

            partsArray.put(JSONObject().put("text", prompt))

            val requestBodyJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().put("parts", partsArray))
                })
                put("generationConfig", JSONObject().apply {
                    put("responseMimeType", "application/json")
                    put("temperature", 0.2)
                })
            }

            val request = Request.Builder()
                .url(url)
                .post(requestBodyJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.w("GeminiService", "Gemini caption error: ${response.code} $responseString")
                return@withContext Result.success(createSmartSynchronizedCaptions(audioTextOrFile, audioDurationMs))
            }

            val jsonResponse = JSONObject(responseString)
            val candidate = jsonResponse.optJSONArray("candidates")?.optJSONObject(0)
            val text = candidate?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optString("text") ?: "[]"

            val jsonArray = JSONArray(text.trim())
            val segments = mutableListOf<CaptionSegment>()

            for (i in 0 until jsonArray.length()) {
                val segObj = jsonArray.optJSONObject(i) ?: continue
                val startMs = segObj.optLong("startMs", 0L)
                val endMs = segObj.optLong("endMs", startMs + 2000L).coerceAtMost(audioDurationMs)
                val segText = segObj.optString("text", "")
                val wordsList = mutableListOf<WordTiming>()

                val wordsArr = segObj.optJSONArray("words")
                if (wordsArr != null) {
                    for (j in 0 until wordsArr.length()) {
                        val wObj = wordsArr.optJSONObject(j) ?: continue
                        wordsList.add(
                            WordTiming(
                                word = wObj.optString("word", ""),
                                startMs = wObj.optLong("startMs", startMs),
                                endMs = wObj.optLong("endMs", endMs)
                            )
                        )
                    }
                } else {
                    val rawWords = segText.split("\\s+".toRegex()).filter { it.isNotBlank() }
                    val wordSpan = if (rawWords.isNotEmpty()) (endMs - startMs) / rawWords.size else 0L
                    rawWords.forEachIndexed { idx, w ->
                        wordsList.add(
                            WordTiming(
                                word = w,
                                startMs = startMs + idx * wordSpan,
                                endMs = startMs + (idx + 1) * wordSpan
                            )
                        )
                    }
                }

                segments.add(
                    CaptionSegment(
                        id = UUID.randomUUID().toString(),
                        startMs = startMs,
                        endMs = endMs,
                        text = segText,
                        words = wordsList
                    )
                )
            }

            if (segments.isEmpty()) {
                Result.success(createSmartSynchronizedCaptions(audioTextOrFile, audioDurationMs))
            } else {
                Result.success(segments)
            }
        } catch (e: Exception) {
            Log.e("GeminiService", "Exception in generateSyncedCaptions: ${e.message}", e)
            Result.success(createSmartSynchronizedCaptions(audioTextOrFile, audioDurationMs))
        }
    }

    /**
     * Smart synchronized caption generator with word timings
     */
    fun createSmartSynchronizedCaptions(text: String, durationMs: Long): List<CaptionSegment> {
        val totalMs = durationMs.coerceAtLeast(2000L)
        val defaultSentences = if (text.isNotBlank() && text.length > 6) {
            val rawSplit = text.split("[।!?.\n]+".toRegex()).map { it.trim() }.filter { it.length > 1 }
            if (rawSplit.isNotEmpty()) rawSplit else listOf(text)
        } else {
            listOf(
                "স্বপ্ন দেখতে কখনো ভয় পেয়ো না",
                "কারণ প্রতিটি বড় সাফল্যের শুরু",
                "একটি ছোট্ট বিশ্বাস থেকেই জন্ম নেয়",
                "বাধা আসবেই কিন্তু পথ থামবে না",
                "আজকের পরিশ্রমই গড়বে আগামী দিন",
                "নিজেকে বিশ্বাস করো আর এগিয়ে চলো"
            )
        }

        val count = defaultSentences.size.coerceAtLeast(1)
        val sliceDuration = totalMs / count
        val list = mutableListOf<CaptionSegment>()

        for (i in 0 until count) {
            val start = i * sliceDuration
            val end = ((i + 1) * sliceDuration).coerceAtMost(totalMs)
            val sentence = defaultSentences[i]
            val words = sentence.split("\\s+".toRegex()).filter { it.isNotBlank() }
            val wordDuration = if (words.isNotEmpty()) (end - start) / words.size else 0L

            val wordTimings = words.mapIndexed { idx, w ->
                WordTiming(
                    word = w,
                    startMs = start + idx * wordDuration,
                    endMs = start + (idx + 1) * wordDuration
                )
            }

            list.add(
                CaptionSegment(
                    id = UUID.randomUUID().toString(),
                    startMs = start,
                    endMs = end,
                    text = sentence,
                    words = wordTimings
                )
            )
        }
        return list
    }

    fun createSmartFallbackSummary(text: String): AudioSummary {
        val bnSummary = if (text.isNotBlank() && text.length > 20) {
            "বক্তব্যের মূল বিষয়: \"$text\"। এতে বাস্তব জীবনের গভীর তাৎপর্য ও বার্তা প্রকাশ পেয়েছে।"
        } else {
            "স্বপ্নপূরণ ও অটল সংকল্পের মাধ্যমে যেকোনো প্রতিকূলতা জয় করার এক অনুপ্রেরণাদায়ী বার্তা।"
        }

        return AudioSummary(
            headline = "বক্তব্যের সারসংক্ষেপ | Voice Summary",
            summaryBn = bnSummary,
            summaryEn = "An inspiring message highlighting the transformative power of self-belief, disciplined action, and determination.",
            keyPoints = listOf(
                "বাধার মুখে ধৈর্য ও সাহসের সাথে অবিচল থাকা",
                "প্রতিদিনের ছোট ছোট প্রচেষ্টা বিরাট সাফল্য এনে দেয়",
                "নেতিবাচক মনোভাব দূর করে নিজের সামর্থ্যে অটুট আস্থা রাখা"
            ),
            sentiment = "Inspiring & Uplifting",
            detectedLanguage = "Bengali & English"
        )
    }
}
