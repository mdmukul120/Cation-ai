package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AudioSummary
import com.example.model.VoiceAnalysis

@Composable
fun AiSummaryAndVoiceInsights(
    summary: AudioSummary?,
    voiceAnalysis: VoiceAnalysis?,
    isProcessing: Boolean,
    processingMessage: String,
    onReAnalyzeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showEnglishSummary by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF141220))
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // AI Processing Banner if active
        AnimatedVisibility(visible = isProcessing) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF261D45)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color(0xFF00E5FF),
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = processingMessage.ifBlank { "কৃত্রিম বুদ্ধিমত্তা বিশ্লেষণ করছে..." },
                        fontSize = 13.sp,
                        color = Color.White
                    )
                }
            }
        }

        // --- 1. Grok Voice & Audio Intelligence Card ---
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1D1832)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("grok_analysis_card")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFF5722)
                        ) {
                            Text(
                                text = "GROK",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ভয়েস ও অডিও বিশ্লেষণ",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Speaking Pace Badge
                    val wpm = voiceAnalysis?.speakingRateWpm ?: 140
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF2B254A)
                    ) {
                        Text(
                            text = "$wpm WPM",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E5FF),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Vocal Energy & Pace metrics
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Metric 1: Tone
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF241F3D),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(text = "ভয়েস টোন", fontSize = 10.sp, color = Color(0xFFAAA5C2))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = voiceAnalysis?.tone ?: "Inspiring & Energetic",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFD600)
                            )
                        }
                    }

                    // Metric 2: Emotion
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF241F3D),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(text = "আবেগ ও অনুভূতি", fontSize = 10.sp, color = Color(0xFFAAA5C2))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = voiceAnalysis?.emotion ?: "Motivated & Confident",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF3D71)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Vocal Energy Progress Bar
                val energy = voiceAnalysis?.vocalEnergyScore ?: 88
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "ভোকাল এনার্জি স্কোর", fontSize = 11.sp, color = Color(0xFFCCC8E0))
                        Text(text = "$energy%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { energy / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFF00E5FF),
                        trackColor = Color(0xFF332D52)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Punch Keywords
                Text(text = "গুরুত্বপূর্ণ পাঞ্চ শব্দ (Punch Keywords):", fontSize = 11.sp, color = Color(0xFFAAA5C2))
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    (voiceAnalysis?.punchKeywords ?: listOf("স্বপ্ন", "সাফল্য", "অদম্য")).forEach { kw ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF2C254B)
                        ) {
                            Text(
                                text = "#$kw",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFFFD600),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                if (!voiceAnalysis?.recommendations.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF161326),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "💡 ${voiceAnalysis?.recommendations}",
                            fontSize = 11.sp,
                            color = Color(0xFFB3E5FC),
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 2. Google Gemini Summary Card ---
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1D36)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("gemini_summary_card")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1A73E8)
                        ) {
                            Text(
                                text = "GEMINI",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "বক্তব্যের সারসংক্ষেপ",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Language toggle
                    TextButton(
                        onClick = { showEnglishSummary = !showEnglishSummary },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (showEnglishSummary) "বাংলা দেখুন" else "English",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E5FF)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Headline
                Text(
                    text = summary?.headline ?: "স্বপ্নের পথে অবিচল যাত্রা",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFD600)
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Main summary text
                Text(
                    text = if (showEnglishSummary) {
                        summary?.summaryEn?.ifBlank { summary.summaryBn } ?: ""
                    } else {
                        summary?.summaryBn ?: ""
                    },
                    fontSize = 12.sp,
                    color = Color(0xFFE0E0E0),
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Key bullet points
                Text(
                    text = "মূল শিক্ষণীয় পয়েন্টসমূহ:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF00E5FF)
                )
                Spacer(modifier = Modifier.height(4.dp))

                val points = summary?.keyPoints ?: listOf(
                    "দৃঢ় সংকল্প ও স্পষ্ট লক্ষ্যের গুরুত্ব",
                    "ধারাবাহিক প্রচেষ্টা এবং বাধা অতিক্রমের মানসিকতা"
                )
                points.forEach { pt ->
                    Row(
                        modifier = Modifier.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(text = "• ", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
                        Text(text = pt, fontSize = 11.sp, color = Color(0xFFCCCCCC), lineHeight = 16.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Re-analyze trigger
        FilledTonalButton(
            onClick = onReAnalyzeClick,
            enabled = !isProcessing,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("reanalyze_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = Color(0xFF26213F),
                contentColor = Color(0xFF00E5FF)
            )
        ) {
            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "পুনরায় এআই দিয়ে বিশ্লেষণ করুন", fontWeight = FontWeight.Bold)
        }
    }
}
