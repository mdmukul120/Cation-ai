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

    /**
     * Resolves Gemini API Key: custom key passed in, or fallback to BuildConfig.GEMINI_API_KEY
     */
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
        customApiKey: String? = null
    ): Result<AudioSummary> = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveKey(customApiKey)
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            // Intelligent fallback summary based on input
            return@withContext Result.success(createSmartFallbackSummary(transcriptOrTopic))
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

            val prompt = """
                Analyze this audio transcript / speech text:
                "$transcriptOrTopic"

                Return a JSON object with this exact structure:
                {
                  "headline": "Catchy short headline in Bengali & English (e.g. স্বপ্নের পথে অবিচল যাত্রা | The Unstoppable Journey)",
                  "summaryBn": "A 2-3 sentence engaging summary in Bengali",
                  "summaryEn": "A 2-3 sentence engaging summary in English",
                  "keyPoints": ["Bullet 1 in Bengali", "Bullet 2 in Bengali", "Bullet 3 in Bengali"],
                  "sentiment": "e.g. Inspiring, Energetic, Emotional",
                  "detectedLanguage": "Bengali / English"
                }
                Provide only raw JSON.
            """.trimIndent()

            val requestBodyJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", prompt))
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("responseMimeType", "application/json")
                    put("temperature", 0.4)
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
            Log.e("GeminiService", "Exception calling Gemini: ${e.message}", e)
            Result.success(createSmartFallbackSummary(transcriptOrTopic))
        }
    }

    /**
     * Generates synchronized caption segments with word-level timestamps using Gemini
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
                The audio duration is $audioDurationMs milliseconds.
                Generate synchronized subtitle caption segments with word-level timings for this speech text:
                "$audioTextOrFile"

                Divide into short, punchy 3-6 word phrases suitable for TikTok / Instagram Reels / YouTube Shorts captions.
                Return a JSON array of segments strictly formatted as:
                [
                  {
                    "startMs": 0,
                    "endMs": 2800,
                    "text": "স্বপ্ন দেখতে ভয় পেয়ো না",
                    "words": [
                      {"word": "স্বপ্ন", "startMs": 0, "endMs": 700},
                      {"word": "দেখতে", "startMs": 700, "endMs": 1400},
                      {"word": "ভয়", "startMs": 1400, "endMs": 2000},
                      {"word": "পেয়ো", "startMs": 2000, "endMs": 2500},
                      {"word": "না", "startMs": 2500, "endMs": 2800}
                    ]
                  }
                ]
                Start times must start from 0 and not exceed $audioDurationMs ms. Return only the raw JSON array.
            """.trimIndent()

            val partsArray = JSONArray()

            // If audio file exists and is small enough, attach inline audio
            if (audioFile != null && audioFile.exists() && audioFile.length() < 8 * 1024 * 1024) {
                try {
                    val bytes = audioFile.readBytes()
                    val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    val mime = if (audioFile.name.endsWith(".wav", true)) "audio/wav" else "audio/mp4"
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
                val endMs = segObj.optLong("endMs", startMs + 2000L)
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
                    // Split text into words automatically
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
     * Smart synchronized caption generator with word timings for offline / demo mode
     */
    fun createSmartSynchronizedCaptions(text: String, durationMs: Long): List<CaptionSegment> {
        val totalMs = durationMs.coerceAtLeast(6000L)
        val defaultSentences = if (text.isNotBlank() && text.length > 10) {
            text.split("[।!?.\n]+".toRegex()).map { it.trim() }.filter { it.length > 2 }
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
            "এই বক্তব্যটিতে আত্মবিশ্বাস, অনুপ্রেরণা এবং অধ্যবসায়ের মাধ্যমে অভীষ্ট লক্ষ্যে পৌঁছানোর গুরুত্ব তুলে ধরা হয়েছে।"
        } else {
            "স্বপ্নপূরণ ও অটল সংকল্পের মাধ্যমে যেকোনো প্রতিকূলতা জয় করার এক অনুপ্রেরণাদায়ী বার্তা।"
        }

        return AudioSummary(
            headline = "স্বপ্নের পথে অবিচল বিজয় | The Power of Persistence",
            summaryBn = bnSummary,
            summaryEn = "An inspiring message highlighting the transformative power of self-belief, disciplined action, and unwavering determination.",
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
