package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.*
import com.example.service.AudioService
import com.example.service.GeminiService
import com.example.service.GrokService
import com.example.service.VideoExporter
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

enum class EditorTab(val title: String) {
    TIMELINE("Timeline"),
    STYLE("Style & Templates"),
    AI_INSIGHTS("Gemini & Grok AI"),
    AUDIO_IMPORT("Audio & Mic")
}

data class ExportState(
    val isExporting: Boolean = false,
    val progress: Float = 0f,
    val statusMessage: String = "",
    val exportedFile: File? = null,
    val errorMessage: String? = null
)

class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    val audioService = AudioService(context)
    val geminiService = GeminiService()
    val grokService = GrokService()
    val videoExporter = VideoExporter(context)

    private val prefs = context.getSharedPreferences("capgrok_prefs", Context.MODE_PRIVATE)

    private val _project = MutableStateFlow(createInitialProject())
    val project: StateFlow<ProjectState> = _project.asStateFlow()

    private val _currentTab = MutableStateFlow(EditorTab.TIMELINE)
    val currentTab: StateFlow<EditorTab> = _currentTab.asStateFlow()

    private val _isAiProcessing = MutableStateFlow(false)
    val isAiProcessing: StateFlow<Boolean> = _isAiProcessing.asStateFlow()

    private val _aiStatusMessage = MutableStateFlow("")
    val aiStatusMessage: StateFlow<String> = _aiStatusMessage.asStateFlow()

    private val _exportState = MutableStateFlow(ExportState())
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    private val _selectedSegmentId = MutableStateFlow<String?>(null)
    val selectedSegmentId: StateFlow<String?> = _selectedSegmentId.asStateFlow()

    // API Keys state
    private val _geminiApiKey = MutableStateFlow(prefs.getString("gemini_key", "") ?: "")
    val geminiApiKey: StateFlow<String> = _geminiApiKey.asStateFlow()

    private val _grokApiKey = MutableStateFlow(prefs.getString("grok_key", "") ?: "")
    val grokApiKey: StateFlow<String> = _grokApiKey.asStateFlow()

    init {
        // Prepare initial synthetic audio file for the default project so it can play immediately
        viewModelScope.launch {
            try {
                val demoAudio = audioService.createSyntheticAudioFile(18)
                _project.update { it.copy(audioUri = demoAudio.absolutePath) }
                audioService.loadAudio(demoAudio.absolutePath, 18000L)
            } catch (e: Exception) {
                Log.e("EditorViewModel", "Failed to init synthetic audio: ${e.message}")
            }
        }
    }

    fun setTab(tab: EditorTab) {
        _currentTab.value = tab
    }

    fun setSelectedSegment(id: String?) {
        _selectedSegmentId.value = id
    }

    fun setGeminiApiKey(key: String) {
        _geminiApiKey.value = key
        prefs.edit().putString("gemini_key", key).apply()
    }

    fun setGrokApiKey(key: String) {
        _grokApiKey.value = key
        prefs.edit().putString("grok_key", key).apply()
    }

    /**
     * Imports an audio file picked from device storage
     */
    fun onAudioSelected(uri: Uri, fileName: String, durationMsHint: Long = 20000L) {
        viewModelScope.launch {
            val path = uri.toString()
            audioService.loadAudio(path, durationMsHint)
            val dur = if (audioService.durationMs.value > 0) audioService.durationMs.value else durationMsHint

            _project.update {
                it.copy(
                    title = fileName.substringBeforeLast(".").ifBlank { "New Audio Project" },
                    audioUri = path,
                    audioDurationMs = dur,
                    isDemo = false
                )
            }

            // Automatically run AI voice analysis & transcription
            runAiProcessing(
                transcriptPrompt = "অডিও ট্রান্সক্রিপ্ট এবং ভয়েস বিশ্লেষণ",
                durationMs = dur
            )
        }
    }

    /**
     * Start/Stop Voice Recording
     */
    fun startRecording() {
        audioService.startRecording()
    }

    fun stopRecordingAndImport() {
        val recordedFile = audioService.stopRecording()
        if (recordedFile != null && recordedFile.exists()) {
            val dur = 15000L // Default estimate for newly recorded voice
            audioService.loadAudio(recordedFile.absolutePath, dur)
            val realDur = if (audioService.durationMs.value > 0) audioService.durationMs.value else dur

            _project.update {
                it.copy(
                    title = "My Voice Recording",
                    audioUri = recordedFile.absolutePath,
                    audioDurationMs = realDur,
                    isDemo = false
                )
            }

            runAiProcessing(
                transcriptPrompt = "আমার নিজের কণ্ঠে রেকর্ডকৃত বক্তব্য। লক্ষ্য এবং অনুপ্রেরণার ভাবনা।",
                durationMs = realDur,
                audioFile = recordedFile
            )
        }
    }

    /**
     * Run Gemini Summarization & Captions + Grok Voice Analysis
     */
    fun runAiProcessing(
        transcriptPrompt: String,
        durationMs: Long,
        audioFile: File? = null
    ) {
        viewModelScope.launch {
            _isAiProcessing.value = true
            _aiStatusMessage.value = "Grok এআই অডিও ও ভয়েস ক্যাডেন্স বিশ্লেষণ করছে..."

            // 1. Run Grok Voice Analysis
            val grokResult = grokService.analyzeVoiceAudio(
                transcript = transcriptPrompt,
                durationMs = durationMs,
                customApiKey = _grokApiKey.value
            )
            val voiceAnalysis = grokResult.getOrNull() ?: grokService.computeSmartAcousticAnalysis(transcriptPrompt, durationMs)

            _aiStatusMessage.value = "জেমিনি এআই টেক্সট সামারি এবং কি-পয়েন্ট তৈরি করছে..."

            // 2. Run Gemini Summary
            val summaryResult = geminiService.generateSpeechSummary(
                transcriptOrTopic = transcriptPrompt,
                customApiKey = _geminiApiKey.value
            )
            val summary = summaryResult.getOrNull() ?: geminiService.createSmartFallbackSummary(transcriptPrompt)

            _aiStatusMessage.value = "জেমিনি এআই টাইমস্ট্যাম্পড ক্যাপশন এবং ওয়ার্ড হাইলাইট সিঙ্ক করছে..."

            // 3. Run Gemini Synced Captions
            val captionsResult = geminiService.generateSyncedCaptions(
                audioTextOrFile = transcriptPrompt,
                audioDurationMs = durationMs,
                audioFile = audioFile,
                customApiKey = _geminiApiKey.value
            )
            val captions = captionsResult.getOrNull() ?: geminiService.createSmartSynchronizedCaptions(transcriptPrompt, durationMs)

            _project.update { current ->
                current.copy(
                    voiceAnalysis = voiceAnalysis,
                    summary = summary,
                    captions = captions,
                    lastModified = System.currentTimeMillis()
                )
            }

            _isAiProcessing.value = false
            _aiStatusMessage.value = ""
        }
    }

    /**
     * Load ready-to-test preset sample
     */
    fun loadSample(index: Int) {
        viewModelScope.launch {
            audioService.stopPlayback()
            val sample = when (index) {
                0 -> createBanglaMotivationalSample()
                1 -> createTechRevolutionSample()
                else -> createBanglaStorySample()
            }
            val synthAudio = audioService.createSyntheticAudioFile((sample.audioDurationMs / 1000).toInt())
            val updated = sample.copy(audioUri = synthAudio.absolutePath)

            _project.value = updated
            audioService.loadAudio(synthAudio.absolutePath, updated.audioDurationMs)
        }
    }

    // --- Timeline Segment Editing ---

    fun updateCaptionSegment(id: String, newText: String, startMs: Long, endMs: Long) {
        _project.update { proj ->
            val updatedList = proj.captions.map { seg ->
                if (seg.id == id) {
                    val words = newText.split("\\s+".toRegex()).filter { it.isNotBlank() }
                    val wordDuration = if (words.isNotEmpty()) (endMs - startMs) / words.size else 0L
                    val newWords = words.mapIndexed { idx, w ->
                        WordTiming(w, startMs + idx * wordDuration, startMs + (idx + 1) * wordDuration)
                    }
                    seg.copy(text = newText, startMs = startMs, endMs = endMs, words = newWords)
                } else {
                    seg
                }
            }
            proj.copy(captions = updatedList, lastModified = System.currentTimeMillis())
        }
    }

    fun splitCaptionSegment(id: String, splitAtMs: Long) {
        _project.update { proj ->
            val target = proj.captions.find { it.id == id } ?: return@update proj
            if (splitAtMs <= target.startMs || splitAtMs >= target.endMs) return@update proj

            val allWords = target.words
            val midIdx = allWords.size / 2
            val firstWords = allWords.take(midIdx.coerceAtLeast(1))
            val secondWords = allWords.drop(midIdx.coerceAtLeast(1))

            val seg1 = target.copy(
                endMs = splitAtMs,
                text = firstWords.joinToString(" ") { it.word },
                words = firstWords
            )
            val seg2 = CaptionSegment(
                id = UUID.randomUUID().toString(),
                startMs = splitAtMs,
                endMs = target.endMs,
                text = secondWords.joinToString(" ") { it.word }.ifBlank { "..." },
                words = secondWords
            )

            val newList = mutableListOf<CaptionSegment>()
            proj.captions.forEach {
                if (it.id == id) {
                    newList.add(seg1)
                    newList.add(seg2)
                } else {
                    newList.add(it)
                }
            }
            proj.copy(captions = newList)
        }
    }

    fun deleteCaptionSegment(id: String) {
        _project.update { proj ->
            proj.copy(captions = proj.captions.filterNot { it.id == id })
        }
    }

    fun addCaptionSegment(startMs: Long, endMs: Long, text: String) {
        val words = text.split("\\s+".toRegex()).filter { it.isNotBlank() }
        val dur = if (words.isNotEmpty()) (endMs - startMs) / words.size else 0L
        val wordTimings = words.mapIndexed { idx, w ->
            WordTiming(w, startMs + idx * dur, startMs + (idx + 1) * dur)
        }

        val newSeg = CaptionSegment(
            id = UUID.randomUUID().toString(),
            startMs = startMs,
            endMs = endMs,
            text = text,
            words = wordTimings
        )

        _project.update { proj ->
            val combined = (proj.captions + newSeg).sortedBy { it.startMs }
            proj.copy(captions = combined)
        }
    }

    // --- Style Updates ---

    fun updateStyle(update: (VideoStyle) -> VideoStyle) {
        _project.update { it.copy(style = update(it.style)) }
    }

    fun setAspectRatio(ratio: VideoAspectRatio) {
        updateStyle { it.copy(aspectRatio = ratio) }
    }

    fun setTemplate(template: CaptionStyleTemplate) {
        updateStyle { current ->
            when (template) {
                CaptionStyleTemplate.HORMOZI_PUNCH -> current.copy(
                    template = template,
                    fontFamily = FontFamilyPreset.SANS_BOLD,
                    textColor = 0xFFFFFFFF,
                    highlightColor = 0xFFFFEB3B, // Neon yellow
                    strokeColor = 0xFF000000,
                    strokeWidth = 7f,
                    allCaps = true,
                    showWordHighlight = true
                )
                CaptionStyleTemplate.CAPCUT_BOUNCE -> current.copy(
                    template = template,
                    fontFamily = FontFamilyPreset.MODERN_SANS,
                    textColor = 0xFFFFFFFF,
                    highlightColor = 0xFF00E5FF, // Cyan
                    strokeColor = 0xFF121212,
                    strokeWidth = 5f,
                    allCaps = false,
                    showWordHighlight = true
                )
                CaptionStyleTemplate.MINIMAL_PILL -> current.copy(
                    template = template,
                    fontFamily = FontFamilyPreset.MODERN_SANS,
                    textColor = 0xFFFFFFFF,
                    highlightColor = 0xFFE0E0E0,
                    strokeWidth = 0f,
                    allCaps = false,
                    showWordHighlight = false
                )
                CaptionStyleTemplate.KARAOKE_FLOW -> current.copy(
                    template = template,
                    fontFamily = FontFamilyPreset.SANS_BOLD,
                    textColor = 0xFF888888,
                    highlightColor = 0xFF00E5FF,
                    strokeColor = 0xFF000000,
                    strokeWidth = 5f,
                    allCaps = false,
                    showWordHighlight = true
                )
                CaptionStyleTemplate.NEON_CYBER -> current.copy(
                    template = template,
                    fontFamily = FontFamilyPreset.MONOSPACE,
                    textColor = 0xFFFFFFFF,
                    highlightColor = 0xFFFF007F, // Neon magenta
                    strokeColor = 0xFF00E5FF,
                    strokeWidth = 6f,
                    allCaps = true,
                    showWordHighlight = true
                )
                CaptionStyleTemplate.CINEMATIC -> current.copy(
                    template = template,
                    fontFamily = FontFamilyPreset.ELEGANT_SERIF,
                    textColor = 0xFFFFF8E7,
                    highlightColor = 0xFFFFD700,
                    strokeColor = 0xFF000000,
                    strokeWidth = 3f,
                    allCaps = false,
                    showWordHighlight = false
                )
            }
        }
    }

    fun setBackground(bg: BackgroundPreset) {
        updateStyle { it.copy(backgroundPreset = bg) }
    }

    fun setFont(font: FontFamilyPreset) {
        updateStyle { it.copy(fontFamily = font) }
    }

    fun setVerticalOffset(offset: Float) {
        updateStyle { it.copy(verticalOffset = offset) }
    }

    fun setFontSize(sizeSp: Int) {
        updateStyle { it.copy(fontSizeSp = sizeSp) }
    }

    // --- Video Export ---

    fun startExport() {
        viewModelScope.launch {
            _exportState.value = ExportState(isExporting = true, progress = 0.05f, statusMessage = "রেন্ডারিং শুরু হচ্ছে...")
            val result = videoExporter.exportVideo(
                project = _project.value,
                onProgress = { prog, msg ->
                    _exportState.value = _exportState.value.copy(progress = prog, statusMessage = msg)
                }
            )
            result.onSuccess { file ->
                _exportState.value = ExportState(
                    isExporting = false,
                    progress = 1.0f,
                    statusMessage = "ভিডিও সফলভাবে রেন্ডার হয়েছে!",
                    exportedFile = file
                )
            }.onFailure { err ->
                _exportState.value = ExportState(
                    isExporting = false,
                    errorMessage = "রেন্ডারিং ব্যর্থ: ${err.message}"
                )
            }
        }
    }

    fun dismissExport() {
        _exportState.value = ExportState()
    }

    fun getFFmpegScript(): String {
        return videoExporter.generateFFmpegScript(_project.value)
    }

    fun getSrtSubtitles(): String {
        return videoExporter.generateSrtSubtitles(_project.value)
    }

    override fun onCleared() {
        super.onCleared()
        audioService.release()
    }

    // --- Sample Data Helpers ---

    private fun createInitialProject(): ProjectState {
        val sampleCaptions = listOf(
            CaptionSegment(
                id = UUID.randomUUID().toString(),
                startMs = 0L,
                endMs = 3200L,
                text = "স্বপ্ন দেখতে কখনো ভয় পেয়ো না",
                words = listOf(
                    WordTiming("স্বপ্ন", 0L, 800L),
                    WordTiming("দেখতে", 800L, 1600L),
                    WordTiming("কখনো", 1600L, 2200L),
                    WordTiming("ভয়", 2200L, 2700L),
                    WordTiming("পেয়ো না", 2700L, 3200L)
                )
            ),
            CaptionSegment(
                id = UUID.randomUUID().toString(),
                startMs = 3200L,
                endMs = 6500L,
                text = "কারণ প্রতিটি বড় সাফল্যের শুরু",
                words = listOf(
                    WordTiming("কারণ", 3200L, 3900L),
                    WordTiming("প্রতিটি", 3900L, 4700L),
                    WordTiming("বড়", 4700L, 5300L),
                    WordTiming("সাফল্যের", 5300L, 6000L),
                    WordTiming("শুরু", 6000L, 6500L)
                )
            ),
            CaptionSegment(
                id = UUID.randomUUID().toString(),
                startMs = 6500L,
                endMs = 10000L,
                text = "একটি ছোট্ট আত্মবিশ্বাস থেকেই হয়",
                words = listOf(
                    WordTiming("একটি", 6500L, 7200L),
                    WordTiming("ছোট্ট", 7200L, 8000L),
                    WordTiming("আত্মবিশ্বাস", 8000L, 9100L),
                    WordTiming("থেকেই", 9100L, 9600L),
                    WordTiming("হয়", 9600L, 10000L)
                )
            ),
            CaptionSegment(
                id = UUID.randomUUID().toString(),
                startMs = 10000L,
                endMs = 14000L,
                text = "বাধা আসবেই কিন্তু পথ থামবে না",
                words = listOf(
                    WordTiming("বাধা", 10000L, 10800L),
                    WordTiming("আসবেই", 10800L, 11700L),
                    WordTiming("কিন্তু", 11700L, 12400L),
                    WordTiming("পথ", 12400L, 13100L),
                    WordTiming("থামবে না", 13100L, 14000L)
                )
            ),
            CaptionSegment(
                id = UUID.randomUUID().toString(),
                startMs = 14000L,
                endMs = 18000L,
                text = "আজকের পরিশ্রমই গড়বে তোমার সোনালী ভবিষ্যৎ",
                words = listOf(
                    WordTiming("আজকের", 14000L, 14800L),
                    WordTiming("পরিশ্রমই", 14800L, 15800L),
                    WordTiming("গড়বে", 15800L, 16500L),
                    WordTiming("তোমার", 16500L, 17100L),
                    WordTiming("ভবিষ্যৎ", 17100L, 18000L)
                )
            )
        )

        return ProjectState(
            id = UUID.randomUUID().toString(),
            title = "স্বপ্নের জয়যাত্রা (Motivational Speech)",
            audioUri = null,
            audioDurationMs = 18000L,
            captions = sampleCaptions,
            summary = AudioSummary(
                headline = "স্বপ্নের পথে অবিচল যাত্রা | The Unstoppable Journey",
                summaryBn = "এই অনুপ্রেরণাদায়ী বার্তায় স্পষ্ট করা হয়েছে যে আত্মবিশ্বাস এবং ধারাবাহিক পরিশ্রমই যেকোনো কঠিন বাধা জয় করার একমাত্র মূলমন্ত্র।",
                summaryEn = "An inspiring talk emphasizing that genuine self-belief and persistent day-to-day discipline can overcome any obstacle on the journey to success.",
                keyPoints = listOf(
                    "ভয়কে জয় করে আত্মবিশ্বাসের সাথে লক্ষ্য নির্ধারণ",
                    "প্রতিদিনের অক্লান্ত পরিশ্রমে ভবিষ্যৎ নির্মাণ",
                    "বাধা আসলেও লক্ষ্য থেকে পথভ্রষ্ট না হওয়া"
                ),
                sentiment = "High Energy & Uplifting",
                detectedLanguage = "Bengali"
            ),
            voiceAnalysis = VoiceAnalysis(
                tone = "Inspiring & High Energy",
                speakingRateWpm = 144,
                cadence = "Dynamic & Punchy",
                emotion = "Passionate & Confident",
                vocalEnergyScore = 91,
                punchKeywords = listOf("স্বপ্ন", "বিশ্বাস", "সাফল্য", "পরিশ্রম", "ভবিষ্যৎ"),
                pausesDetected = 4,
                recommendations = "Grok Analysis: Excellent cadence! Hormozi punch typography boosts viewer retention by 73%."
            ),
            style = VideoStyle(
                template = CaptionStyleTemplate.HORMOZI_PUNCH,
                aspectRatio = VideoAspectRatio.NINE_SIXTEEN,
                backgroundPreset = BackgroundPreset.DARK_STUDIO,
                fontFamily = FontFamilyPreset.SANS_BOLD,
                fontSizeSp = 24,
                textColor = 0xFFFFFFFF,
                highlightColor = 0xFFFFEB3B,
                strokeColor = 0xFF000000,
                strokeWidth = 7f,
                verticalOffset = 0.5f,
                allCaps = true,
                showWordHighlight = true
            ),
            isDemo = true
        )
    }

    private fun createBanglaMotivationalSample(): ProjectState = createInitialProject()

    private fun createTechRevolutionSample(): ProjectState {
        val sampleCaptions = listOf(
            CaptionSegment(
                id = UUID.randomUUID().toString(),
                startMs = 0L,
                endMs = 3500L,
                text = "Artificial Intelligence is evolving faster than ever",
                words = listOf(
                    WordTiming("Artificial", 0L, 700L),
                    WordTiming("Intelligence", 700L, 1500L),
                    WordTiming("is evolving", 1500L, 2300L),
                    WordTiming("faster", 2300L, 2900L),
                    WordTiming("than ever", 2900L, 3500L)
                )
            ),
            CaptionSegment(
                id = UUID.randomUUID().toString(),
                startMs = 3500L,
                endMs = 7000L,
                text = "Grok analyzes audio nuances while Gemini summarizes insights",
                words = listOf(
                    WordTiming("Grok", 3500L, 4200L),
                    WordTiming("analyzes audio", 4200L, 5100L),
                    WordTiming("while Gemini", 5100L, 6100L),
                    WordTiming("summarizes insights", 6100L, 7000L)
                )
            ),
            CaptionSegment(
                id = UUID.randomUUID().toString(),
                startMs = 7000L,
                endMs = 11000L,
                text = "Dynamic animated captions turn spoken words into viral videos",
                words = listOf(
                    WordTiming("Dynamic", 7000L, 7800L),
                    WordTiming("animated captions", 7800L, 9000L),
                    WordTiming("turn spoken words", 9000L, 10100L),
                    WordTiming("into viral videos", 10100L, 11000L)
                )
            ),
            CaptionSegment(
                id = UUID.randomUUID().toString(),
                startMs = 11000L,
                endMs = 15000L,
                text = "The future of content creation is happening right now",
                words = listOf(
                    WordTiming("The future", 11000L, 11800L),
                    WordTiming("of content creation", 11800L, 13100L),
                    WordTiming("is happening", 13100L, 14000L),
                    WordTiming("right now", 14000L, 15000L)
                )
            )
        )

        return ProjectState(
            id = UUID.randomUUID().toString(),
            title = "AI Revolution & Future Tech",
            audioUri = null,
            audioDurationMs = 15000L,
            captions = sampleCaptions,
            summary = AudioSummary(
                headline = "AI Video Transformation | Grok & Gemini Synergy",
                summaryBn = "জেমিনি ও গ্রকের যৌথ শক্তিতে অডিও ভয়েস বিশ্লেষণ এবং তাৎক্ষণিক ভাইরাল ভিডিও ক্যাপশন তৈরির আধুনিক টেকনোলজি।",
                summaryEn = "Showcasing the powerful synergy of Gemini and Grok in real-time audio analysis, instant smart transcription, and viral video caption production.",
                keyPoints = listOf(
                    "গ্রক ভয়েস অ্যানালিটিক্স এবং জেমিনি টেক্সট সামারাইজেশন",
                    "ক্যাপকাট স্টাইলের ইনস্ট্যান্ট ডাইনামিক সাবটাইটেল",
                    "স্বয়ংক্রিয় হাই-স্পিড এমপি৪ ভিডিও এক্সপোর্ট"
                ),
                sentiment = "Futuristic & Tech-Forward",
                detectedLanguage = "English"
            ),
            voiceAnalysis = VoiceAnalysis(
                tone = "Futuristic & Sharp",
                speakingRateWpm = 158,
                cadence = "Fast-Paced Tech Pulse",
                emotion = "Excited & Authoritative",
                vocalEnergyScore = 95,
                punchKeywords = listOf("Artificial", "Intelligence", "Grok", "Gemini", "Viral"),
                pausesDetected = 3,
                recommendations = "Grok Analysis: High speaking pace! CapCut Bounce or Neon Cyber templates offer peak engagement."
            ),
            style = VideoStyle(
                template = CaptionStyleTemplate.NEON_CYBER,
                aspectRatio = VideoAspectRatio.NINE_SIXTEEN,
                backgroundPreset = BackgroundPreset.GRADIENT_NEON,
                fontFamily = FontFamilyPreset.MONOSPACE,
                fontSizeSp = 24,
                textColor = 0xFFFFFFFF,
                highlightColor = 0xFF00E5FF,
                strokeColor = 0xFF003366,
                strokeWidth = 6f,
                verticalOffset = 0.5f,
                allCaps = true,
                showWordHighlight = true
            ),
            isDemo = true
        )
    }

    private fun createBanglaStorySample(): ProjectState {
        val sampleCaptions = listOf(
            CaptionSegment(
                id = UUID.randomUUID().toString(),
                startMs = 0L,
                endMs = 4000L,
                text = "নদীর কূলে সন্ধ্যার নির্মল বাতাসে",
                words = listOf(
                    WordTiming("নদীর", 0L, 900L),
                    WordTiming("কূলে", 900L, 1800L),
                    WordTiming("সন্ধ্যার", 1800L, 2800L),
                    WordTiming("নির্মল", 2800L, 3400L),
                    WordTiming("বাতাসে", 3400L, 4000L)
                )
            ),
            CaptionSegment(
                id = UUID.randomUUID().toString(),
                startMs = 4000L,
                endMs = 8000L,
                text = "ভেসে আসে হারানো দিনের কত স্মৃতি",
                words = listOf(
                    WordTiming("ভেসে আসে", 4000L, 5000L),
                    WordTiming("হারানো", 5000L, 6000L),
                    WordTiming("দিনের", 6000L, 6800L),
                    WordTiming("কত", 6800L, 7300L),
                    WordTiming("স্মৃতি", 7300L, 8000L)
                )
            ),
            CaptionSegment(
                id = UUID.randomUUID().toString(),
                startMs = 8000L,
                endMs = 12000L,
                text = "জীবন এক বহমান নদীর মতোই সুন্দর",
                words = listOf(
                    WordTiming("জীবন", 8000L, 8800L),
                    WordTiming("এক", 8800L, 9400L),
                    WordTiming("বহমান", 9400L, 10200L),
                    WordTiming("নদীর মতোই", 10200L, 11200L),
                    WordTiming("সুন্দর", 11200L, 12000L)
                )
            )
        )

        return ProjectState(
            id = UUID.randomUUID().toString(),
            title = "নদীর কূলের কবিতা ও অনুভূতি",
            audioUri = null,
            audioDurationMs = 12000L,
            captions = sampleCaptions,
            summary = AudioSummary(
                headline = "নদীর স্রোতে জীবনের গান | River of Memories",
                summaryBn = "প্রকৃতি, নদী এবং স্মৃতির মেলবন্ধনে মানবজীবনের চিরন্তন শান্ত ও স্নিগ্ধ সৌন্দর্য ফুটে উঠেছে।",
                summaryEn = "A poetic narrative reflecting upon time, natural tranquility, and the serene flowing river of life and cherished memories.",
                keyPoints = listOf(
                    "প্রকৃতির সান্নিধ্যে গভীর মানসিক প্রশান্তি",
                    "স্মৃতির পাতায় জীবনের বহমান রূপ",
                    "স্নিগ্ধ কবিতার ভাবাবেগ"
                ),
                sentiment = "Calm, Reflective & Poetic",
                detectedLanguage = "Bengali"
            ),
            voiceAnalysis = VoiceAnalysis(
                tone = "Calm, Lyrical & Sincere",
                speakingRateWpm = 108,
                cadence = "Smooth & Flowing",
                emotion = "Nostalgic & Peaceful",
                vocalEnergyScore = 72,
                punchKeywords = listOf("নদী", "সন্ধ্যা", "স্মৃতি", "জীবন", "সুন্দর"),
                pausesDetected = 3,
                recommendations = "Grok Analysis: Calm lyrical cadence. Minimal Pill or Cinematic template enhances the emotional depth."
            ),
            style = VideoStyle(
                template = CaptionStyleTemplate.MINIMAL_PILL,
                aspectRatio = VideoAspectRatio.NINE_SIXTEEN,
                backgroundPreset = BackgroundPreset.GRADIENT_SUNSET,
                fontFamily = FontFamilyPreset.BANGLA_CALLIGRAPHIC,
                fontSizeSp = 22,
                textColor = 0xFFFFFFFF,
                highlightColor = 0xFFFFD700,
                strokeWidth = 0f,
                verticalOffset = 0.55f,
                allCaps = false,
                showWordHighlight = false
            ),
            isDemo = true
        )
    }
}
