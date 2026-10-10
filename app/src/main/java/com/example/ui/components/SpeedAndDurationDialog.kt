package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.ProjectState
import java.util.Locale

@Composable
fun SpeedAndDurationDialog(
    project: ProjectState,
    onSetDuration: (Long) -> Unit,
    onAdjustDelta: (Long) -> Unit,
    onResetToAudio: () -> Unit,
    onSetSpeed: (Float) -> Unit,
    onSetTrimStart: (Long) -> Unit,
    onSetTrimEnd: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val effectiveDurSec = project.effectiveDurationMs / 1000f
    val audioDurSec = project.audioDurationMs / 1000f
    var sliderValue by remember(project.effectiveDurationMs) {
        mutableFloatStateOf(effectiveDurSec)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF181528),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF332D52)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF00E5FF),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = Color(0xFF0C0A14),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(6.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ভিডিও ডিউরেশন ও স্পিড",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Premiere Pro Duration / Time Remap",
                                fontSize = 11.sp,
                                color = Color(0xFFFFD600)
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Duration Comparison Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Original Audio Card
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF221D38))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = "ভয়েস ফাইল দৈর্ঘ্য", fontSize = 11.sp, color = Color(0xFFAAA5C2))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = String.format(Locale.US, "%.1f সে.", audioDurSec),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00E5FF)
                            )
                        }
                    }

                    // Target Video Duration Card
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2046)),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFFD600))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = "রেন্ডার ভিডিও দৈর্ঘ্য", fontSize = 11.sp, color = Color(0xFFFFD600))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = String.format(Locale.US, "%.1f সে.", effectiveDurSec),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // One-tap Sync with Voice Duration Button
                Button(
                    onClick = {
                        onResetToAudio()
                        sliderValue = audioDurSec
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sync_voice_duration_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00E5FF),
                        contentColor = Color(0xFF0C0A14)
                    )
                ) {
                    Icon(imageVector = Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ভয়েস ফাইলের সমান করুন (${String.format(Locale.US, "%.1fs", audioDurSec)})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Duration Slider Control
                Text(
                    text = "ভিডিও ডিউরেশন ম্যানুয়ালি কমান বা বাড়ান:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "১ সে.",
                        fontSize = 11.sp,
                        color = Color(0xFFAAA5C2)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Slider(
                        value = sliderValue,
                        onValueChange = {
                            sliderValue = it
                            onSetDuration((it * 1000L).toLong())
                        },
                        valueRange = 1f..((audioDurSec * 2.5f).coerceAtLeast(30f)),
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFFFD600),
                            activeTrackColor = Color(0xFFFFD600)
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = String.format(Locale.US, "%.1fs", sliderValue),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD600)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Quick Increment / Decrement Buttons
                Text(text = "দ্রুত সময় যোগ/বিয়োগ করুন:", fontSize = 12.sp, color = Color(0xFFAAA5C2))
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(-5000L to "-5s", -1000L to "-1s", 1000L to "+1s", 5000L to "+5s").forEach { (delta, label) ->
                        FilledTonalButton(
                            onClick = { onAdjustDelta(delta) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (delta > 0) Color(0xFF2C382A) else Color(0xFF38242A),
                                contentColor = if (delta > 0) Color(0xFF00E676) else Color(0xFFFF5252)
                            )
                        ) {
                            Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Premiere Pro Speed Multiplier (Time Remapping)
                Text(
                    text = "প্লেব্যাক স্পিড (Premiere Pro Speed / Time Remap):",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))

                val speedOptions = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    speedOptions.forEach { spd ->
                        val isSelected = kotlin.math.abs(project.playbackSpeed - spd) < 0.05f
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFF00E5FF) else Color(0xFF221E36))
                                .clickable { onSetSpeed(spd) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${spd}x",
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal,
                                color = if (isSelected) Color(0xFF0C0A14) else Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // In / Out Point Trim Info
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1E1A30),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "ইন-পয়েন্ট (Mark In): ${(project.trimStartMs / 1000f)}s", fontSize = 11.sp, color = Color(0xFFAAA5C2))
                            Text(text = "আউট-পয়েন্ট (Mark Out): ${(project.trimEndMs / 1000f)}s", fontSize = 11.sp, color = Color(0xFFAAA5C2))
                        }
                        TextButton(
                            onClick = {
                                onSetTrimStart(0L)
                                onSetTrimEnd(project.audioDurationMs)
                            }
                        ) {
                            Text("ট্রিম রিসেট", fontSize = 12.sp, color = Color(0xFF00E5FF))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Done Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2D254C),
                        contentColor = Color.White
                    )
                ) {
                    Text(text = "সম্পন্ন", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
