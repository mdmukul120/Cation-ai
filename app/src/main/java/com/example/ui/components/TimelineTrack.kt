package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CaptionSegment
import java.util.Locale

@Composable
fun TimelineTrack(
    durationMs: Long,
    currentPositionMs: Long,
    isPlaying: Boolean,
    captions: List<CaptionSegment>,
    selectedSegmentId: String?,
    onSeek: (Long) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSelectSegment: (String) -> Unit,
    onAddSegmentClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalMs = durationMs.coerceAtLeast(1000L)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF141220))
            .padding(12.dp)
    ) {
        // 1. Playback controls & Timecode row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Timecode
            Text(
                text = "${formatTimecode(currentPositionMs)} / ${formatTimecode(totalMs)}",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF00E5FF)
            )

            // Playback buttons
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onSeek((currentPositionMs - 3000L).coerceAtLeast(0L)) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay5,
                        contentDescription = "Rewind 5s",
                        tint = Color(0xFFCCCCCC),
                        modifier = Modifier.size(20.dp)
                    )
                }

                FilledIconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("play_pause_button"),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color(0xFF00E5FF),
                        contentColor = Color(0xFF0C0A14)
                    )
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(
                    onClick = { onSeek((currentPositionMs + 3000L).coerceAtMost(totalMs)) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Forward5,
                        contentDescription = "Forward 5s",
                        tint = Color(0xFFCCCCCC),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Add Segment button
            FilledTonalButton(
                onClick = onAddSegmentClick,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = Color(0xFF26213B),
                    contentColor = Color(0xFFFFD600)
                ),
                modifier = Modifier.testTag("add_caption_button")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "ক্যাপশন যোগ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Interactive Waveform Track & Playhead
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(8.dp))
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
                val barCount = 70
                val barSpacing = w / barCount
                val midY = h * 0.5f

                // Audio waveform bars
                for (i in 0 until barCount) {
                    val barX = i * barSpacing + barSpacing * 0.5f
                    val progressRatio = barX / w
                    val barTimeMs = (progressRatio * totalMs).toLong()

                    // Pseudo-natural speech waveform shape
                    val waveHeight = (h * 0.2f) + (kotlin.math.sin(i * 0.35).toFloat() * kotlin.math.cos(i * 0.12).toFloat() * (h * 0.35f)).coerceAtLeast(4f)

                    val isPast = barTimeMs <= currentPositionMs
                    val barColor = if (isPast) Color(0xFF00E5FF) else Color(0xFF4A4462)

                    drawLine(
                        color = barColor,
                        start = Offset(barX, midY - waveHeight * 0.5f),
                        end = Offset(barX, midY + waveHeight * 0.5f),
                        strokeWidth = (barSpacing * 0.6f).coerceIn(2f, 6f)
                    )
                }

                // Subtitle block overlays on waveform
                captions.forEach { seg ->
                    val startX = (seg.startMs.toFloat() / totalMs) * w
                    val endX = (seg.endMs.toFloat() / totalMs) * w
                    drawRoundRect(
                        color = Color(0x33FFD600),
                        topLeft = Offset(startX, 0f),
                        size = Size(endX - startX, h),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                }

                // Red/Cyan Playhead Needle
                val playheadX = (currentPositionMs.toFloat() / totalMs) * w
                drawLine(
                    color = Color(0xFFFF3D71),
                    start = Offset(playheadX, 0f),
                    end = Offset(playheadX, h),
                    strokeWidth = 3f
                )
                drawCircle(
                    color = Color(0xFFFF3D71),
                    radius = 5f,
                    center = Offset(playheadX, 4f)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Caption Segments Track (Horizontal list of subtitle clips)
        Text(
            text = "সাবটাইটেল ট্র্যাক (${captions.size} টি বাক্য)",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFFAAA5C2),
            modifier = Modifier.padding(bottom = 6.dp)
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(captions, key = { it.id }) { seg ->
                val isSelected = seg.id == selectedSegmentId
                val isCurrent = currentPositionMs in seg.startMs..seg.endMs

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when {
                        isSelected -> Color(0xFF3B2F63)
                        isCurrent -> Color(0xFF25203D)
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
                        .widthIn(min = 120.dp, max = 220.dp)
                        .clickable {
                            onSeek(seg.startMs)
                            onSelectSegment(seg.id)
                        }
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${formatTimecode(seg.startMs)}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCurrent) Color(0xFF00E5FF) else Color(0xFF88849E)
                            )
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit segment",
                                tint = Color(0xFFAAAAAA),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = seg.text,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

private fun formatTimecode(ms: Long): String {
    val totalSec = ms / 1000
    val mins = totalSec / 60
    val secs = totalSec % 60
    val tenths = (ms % 1000) / 100
    return String.format(Locale.US, "%02d:%02d.%d", mins, secs, tenths)
}
