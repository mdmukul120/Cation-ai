package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CaptionSegment
import com.example.model.ProjectState
import java.util.Locale

@Composable
fun TimelineTrack(
    project: ProjectState,
    currentPositionMs: Long,
    isPlaying: Boolean,
    selectedSegmentId: String?,
    onSeek: (Long) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSelectSegment: (String) -> Unit,
    onAddSegmentClick: () -> Unit,
    onOpenSpeedDuration: () -> Unit,
    onAdjustDelta: (Long) -> Unit,
    onResetToAudio: () -> Unit,
    onRazorCut: (Long) -> Unit,
    onMarkIn: (Long) -> Unit,
    onMarkOut: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalMs = project.effectiveDurationMs.coerceAtLeast(1000L)
    val voiceMs = project.audioDurationMs.coerceAtLeast(1000L)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF141220))
            .padding(10.dp)
    ) {
        // --- 1. Premiere Pro Sequence Bar (Timecode & Quick Duration Indicators) ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // SMPTE Timecode
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF1C192E),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF332D50))
                ) {
                    Text(
                        text = "${formatSmpte(currentPositionMs)} / ${formatSmpte(totalMs)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF00E5FF),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                // Duration summary badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF26203D),
                    modifier = Modifier.clickable { onOpenSpeedDuration() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ভিডিও: ${String.format(Locale.US, "%.1fs", totalMs / 1000f)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFFFD600)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "(ভয়েস: ${String.format(Locale.US, "%.1fs", voiceMs / 1000f)})",
                            fontSize = 10.sp,
                            color = Color(0xFFAAA5C2)
                        )
                    }
                }
            }

            // Transport Playback Controls
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onSeek((currentPositionMs - 3000L).coerceAtLeast(0L)) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay5,
                        contentDescription = "Rewind 5s",
                        tint = Color(0xFFCCCCCC),
                        modifier = Modifier.size(18.dp)
                    )
                }

                FilledIconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("play_pause_button"),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color(0xFF00E5FF),
                        contentColor = Color(0xFF0C0A14)
                    )
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = { onSeek((currentPositionMs + 3000L).coerceAtMost(totalMs)) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Forward5,
                        contentDescription = "Forward 5s",
                        tint = Color(0xFFCCCCCC),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- 2. Premiere Pro Editing Toolbar ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Speed & Duration Button
            FilledTonalButton(
                onClick = onOpenSpeedDuration,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = Color(0xFF2B2248),
                    contentColor = Color(0xFFFFD600)
                ),
                modifier = Modifier.testTag("speed_duration_button")
            ) {
                Icon(imageVector = Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "ডিউরেশন", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            // Sync with voice 1-tap reset
            FilledTonalButton(
                onClick = onResetToAudio,
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = Color(0xFF1B3138),
                    contentColor = Color(0xFF00E5FF)
                ),
                modifier = Modifier.testTag("reset_voice_button")
            ) {
                Icon(imageVector = Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(3.dp))
                Text(text = "ভয়েস সমান", fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }

            // Razor Cut Tool
            FilledTonalButton(
                onClick = { onRazorCut(currentPositionMs) },
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = Color(0xFF28243E),
                    contentColor = Color.White
                ),
                modifier = Modifier.testTag("razor_tool_button")
            ) {
                Icon(imageVector = Icons.Default.ContentCut, contentDescription = null, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(3.dp))
                Text(text = "কাট", fontSize = 11.sp)
            }

            // Mark In Point
            FilledTonalButton(
                onClick = { onMarkIn(currentPositionMs) },
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = Color(0xFF221F36),
                    contentColor = Color(0xFFAAA5C2)
                )
            ) {
                Text(text = "[ In", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            // Mark Out Point
            FilledTonalButton(
                onClick = { onMarkOut(currentPositionMs) },
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = Color(0xFF221F36),
                    contentColor = Color(0xFFAAA5C2)
                )
            ) {
                Text(text = "Out ]", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.weight(1f))

            // Add Segment button
            FilledTonalButton(
                onClick = onAddSegmentClick,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = Color(0xFF00E5FF),
                    contentColor = Color(0xFF0C0A14)
                ),
                modifier = Modifier.testTag("add_caption_button")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(2.dp))
                Text(text = "ক্যাপশন", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Quick Delta Chips row (-5s, -1s, +1s, +5s)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = "দৈর্ঘ্য টিউনিং:", fontSize = 10.sp, color = Color(0xFF88849E))
            listOf(-5000L to "-5s", -1000L to "-1s", 1000L to "+1s", 5000L to "+5s").forEach { (delta, lbl) ->
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (delta > 0) Color(0xFF1E3224) else Color(0xFF321E24),
                    modifier = Modifier.clickable { onAdjustDelta(delta) }
                ) {
                    Text(
                        text = lbl,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (delta > 0) Color(0xFF00E676) else Color(0xFFFF5252),
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- 3. Multi-Track Sequence View (Premiere Pro Timeline Layout) ---
        // Track Header & Waveform Scrubber
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Track label A1 (Audio Waveform)
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color(0xFF2A2246),
                modifier = Modifier.width(24.dp).height(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = "A1", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFF00E5FF))
                }
            }
            Spacer(modifier = Modifier.width(6.dp))

            // Interactive Waveform Track & Playhead
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF1E1A30))
                    .testTag("waveform_scrubber")
                    .pointerInput(totalMs) {
                        detectTapGestures { offset ->
                            val ratio = (offset.x / size.width).coerceIn(0f, 1f)
                            val targetMs = (ratio * totalMs).toLong()
                            onSeek(targetMs)
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val barCount = 75
                    val barSpacing = w / barCount
                    val midY = h * 0.5f

                    // Trimmed active area background
                    val trimStartRatio = (project.trimStartMs.toFloat() / totalMs).coerceIn(0f, 1f)
                    val trimEndRatio = (project.trimEndMs.toFloat() / totalMs).coerceIn(0f, 1f)
                    drawRect(
                        color = Color(0x2200E5FF),
                        topLeft = Offset(trimStartRatio * w, 0f),
                        size = Size((trimEndRatio - trimStartRatio) * w, h)
                    )

                    // Audio waveform bars
                    for (i in 0 until barCount) {
                        val barX = i * barSpacing + barSpacing * 0.5f
                        val progressRatio = barX / w
                        val barTimeMs = (progressRatio * totalMs).toLong()

                        val waveHeight = (h * 0.22f) + (kotlin.math.sin(i * 0.38).toFloat() * kotlin.math.cos(i * 0.15).toFloat() * (h * 0.38f)).coerceAtLeast(4f)
                        val isPast = barTimeMs <= currentPositionMs
                        val barColor = if (isPast) Color(0xFF00E5FF) else Color(0xFF4B4468)

                        drawLine(
                            color = barColor,
                            start = Offset(barX, midY - waveHeight * 0.5f),
                            end = Offset(barX, midY + waveHeight * 0.5f),
                            strokeWidth = (barSpacing * 0.65f).coerceIn(2f, 6f)
                        )
                    }

                    // Caption block markers on timeline
                    project.captions.forEach { seg ->
                        val startX = (seg.startMs.toFloat() / totalMs) * w
                        val endX = (seg.endMs.toFloat() / totalMs) * w
                        drawRoundRect(
                            color = Color(0x33FFD600),
                            topLeft = Offset(startX, 0f),
                            size = Size((endX - startX).coerceAtLeast(3f), h),
                            cornerRadius = CornerRadius(3f, 3f)
                        )
                    }

                    // Red Playhead Needle
                    val playheadX = (currentPositionMs.toFloat() / totalMs) * w
                    drawLine(
                        color = Color(0xFFFF3D71),
                        start = Offset(playheadX, 0f),
                        end = Offset(playheadX, h),
                        strokeWidth = 3f
                    )
                    drawCircle(
                        color = Color(0xFFFF3D71),
                        radius = 4f,
                        center = Offset(playheadX, 3f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- 4. Track T1 (Subtitle / Text Track) ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color(0xFF332A1C),
                modifier = Modifier.width(24.dp).height(50.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = "T1", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFFFFD600))
                }
            }
            Spacer(modifier = Modifier.width(6.dp))

            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(project.captions, key = { it.id }) { seg ->
                    val isSelected = seg.id == selectedSegmentId
                    val isCurrent = currentPositionMs in seg.startMs..seg.endMs

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when {
                            isSelected -> Color(0xFF3B2F63)
                            isCurrent -> Color(0xFF282245)
                            else -> Color(0xFF1B182B)
                        },
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isSelected || isCurrent) 1.5.dp else 1.dp,
                            color = when {
                                isSelected -> Color(0xFFFFD600)
                                isCurrent -> Color(0xFF00E5FF)
                                else -> Color(0xFF2E2948)
                            }
                        ),
                        modifier = Modifier
                            .widthIn(min = 110.dp, max = 200.dp)
                            .clickable {
                                onSeek(seg.startMs)
                                onSelectSegment(seg.id)
                            }
                    ) {
                        Column(modifier = Modifier.padding(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = formatSmpte(seg.startMs),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCurrent) Color(0xFF00E5FF) else Color(0xFF88849E)
                                )
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit segment",
                                    tint = Color(0xFFAAAAAA),
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = seg.text,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatSmpte(ms: Long): String {
    val totalSec = ms / 1000
    val mins = totalSec / 60
    val secs = totalSec % 60
    val frames = ((ms % 1000) * 30 / 1000).toInt() // 30 fps SMPTE frames
    return String.format(Locale.US, "%02d:%02d:%02d", mins, secs, frames)
}
