package com.example.model

import java.util.UUID

/**
 * Word-level timing for dynamic active word highlighting
 */
data class WordTiming(
    val word: String,
    val startMs: Long,
    val endMs: Long
)

/**
 * A caption segment (one subtitle block) on the timeline
 */
data class CaptionSegment(
    val id: String = UUID.randomUUID().toString(),
    val startMs: Long,
    val endMs: Long,
    val text: String,
    val words: List<WordTiming> = emptyList()
)

/**
 * Grok Voice & Audio Analysis insights
 */
data class VoiceAnalysis(
    val tone: String = "Inspiring & Energetic",
    val speakingRateWpm: Int = 142,
    val cadence: String = "Dynamic & Engaging",
    val emotion: String = "Motivated / Confident",
    val vocalEnergyScore: Int = 88, // 0 - 100
    val punchKeywords: List<String> = listOf("সাফল্য", "স্বপ্ন", "Revolution", "Future", "Believe"),
    val pausesDetected: Int = 4,
    val recommendations: String = "Pacing is optimal for short-form video engagement (TikTok / Reels / Shorts)."
)

/**
 * Gemini Text Summary & Key Highlights
 */
data class AudioSummary(
    val headline: String,
    val summaryBn: String,
    val summaryEn: String,
    val keyPoints: List<String>,
    val sentiment: String = "Positive & Uplifting",
    val detectedLanguage: String = "Bengali & English"
)

/**
 * Preset Subtitle & Caption Visual Templates (CapCut-inspired)
 */
enum class CaptionStyleTemplate(val displayName: String, val description: String) {
    HORMOZI_PUNCH(
        displayName = "Hormozi Punch",
        description = "Bold uppercase, heavy black stroke, neon yellow active word highlight"
    ),
    CAPCUT_BOUNCE(
        displayName = "CapCut Bounce",
        description = "Modern sans-serif with smooth scaling bounce on active spoken word"
    ),
    KARAOKE_FLOW(
        displayName = "Karaoke Flow",
        description = "Progressive vibrant cyan color flow matching speech pace"
    ),
    MINIMAL_PILL(
        displayName = "Minimal Pill",
        description = "Sleek translucent glass pill background with clean elegant typography"
    ),
    NEON_CYBER(
        displayName = "Neon Cyber",
        description = "Futuristic glowing neon stroke and electric aura"
    ),
    CINEMATIC(
        displayName = "Cinematic Classic",
        description = "Subtle letterboxed serif with understated elegance"
    ),
    BOXED_HIGHLIGHT(
        displayName = "Boxed Accent",
        description = "Active word has a vibrant solid contrast highlight badge"
    )
}

/**
 * Video Aspect Ratios
 */
enum class VideoAspectRatio(val label: String, val ratio: Float, val widthDp: Int, val heightDp: Int) {
    NINE_SIXTEEN("9:16 (Shorts/Reels)", 9f / 16f, 216, 384),
    ONE_ONE("1:1 (Square Post)", 1f, 280, 280),
    SIXTEEN_NINE("16:9 (Landscape)", 16f / 9f, 320, 180)
}

/**
 * Video Background Style
 */
enum class BackgroundPreset(val title: String) {
    GRADIENT_NEON("Cyber Neon"),
    GRADIENT_SUNSET("Sunset Velvet"),
    DARK_STUDIO("Dark Studio"),
    CYBER_PULSE("Deep Purple"),
    MINIMAL_BLACK("OLED Black"),
    SOLID_EMERALD("Emerald"),
    SOLID_CUSTOM("Solid Color"),
    CUSTOM_MEDIA("Custom Image")
}

/**
 * Typography Font Presets
 */
enum class FontFamilyPreset(val title: String) {
    SANS_BOLD("Bold Headline"),
    BANGLA_CALLIGRAPHIC("Stylized Bangla (লিপি)"),
    BANGLA_MODERN("Modern Bangla (বাংলা)"),
    MODERN_SANS("Modern Sans"),
    ELEGANT_SERIF("Elegant Serif"),
    MONOSPACE("Monospace Tech"),
    HEAVY_IMPACT("Heavy Impact")
}

/**
 * Comprehensive visual style configuration for caption rendering
 */
data class VideoStyle(
    val template: CaptionStyleTemplate = CaptionStyleTemplate.HORMOZI_PUNCH,
    val aspectRatio: VideoAspectRatio = VideoAspectRatio.NINE_SIXTEEN,
    val backgroundPreset: BackgroundPreset = BackgroundPreset.DARK_STUDIO,
    val customSolidBgColor: Long = 0xFF121028,
    val customMediaUri: String? = null,
    val fontFamily: FontFamilyPreset = FontFamilyPreset.SANS_BOLD,
    val fontSizeSp: Int = 26,
    val textColor: Long = 0xFFFFFFFF,
    val highlightColor: Long = 0xFFFFEB3B, // Bright yellow
    val strokeColor: Long = 0xFF000000,
    val strokeWidth: Float = 6f,
    val backgroundColor: Long = 0x88000000,
    val verticalOffset: Float = 0.55f, // 0 is middle, 0.55 is lower third, -0.5 is top
    val allCaps: Boolean = true,
    val showWordHighlight: Boolean = true
)

/**
 * Complete project containing audio, captions, AI analyses, and video styling
 */
data class ProjectState(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "Untitled AI Video",
    val audioUri: String? = null,
    val audioDurationMs: Long = 18000L,
    val captions: List<CaptionSegment> = emptyList(),
    val summary: AudioSummary? = null,
    val voiceAnalysis: VoiceAnalysis? = null,
    val style: VideoStyle = VideoStyle(),
    val isDemo: Boolean = false,
    val lastModified: Long = System.currentTimeMillis()
)
