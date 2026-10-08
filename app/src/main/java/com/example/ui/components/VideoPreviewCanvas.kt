package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import java.util.Locale

@Composable
fun VideoPreviewCanvas(
    project: ProjectState,
    currentPositionMs: Long,
    isPlaying: Boolean,
    onTogglePlayPause: () -> Unit,
    onVerticalOffsetChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val style = project.style
    val aspectRatio = style.aspectRatio.ratio

    // Animated background pulse
    val infiniteTransition = rememberInfiniteTransition(label = "canvas_bg_anim")
    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse"
    )

    // Current caption segment
    val currentSegment = project.captions.find { currentPositionMs in it.startMs..it.endMs }
    val currentActiveWord = currentSegment?.words?.find { currentPositionMs in it.startMs..it.endMs }

    val textMeasurer = rememberTextMeasurer()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        // Aspect ratio container card
        Card(
            modifier = Modifier
                .aspectRatio(aspectRatio)
                .fillMaxHeight(0.95f)
                .shadow(16.dp, shape = RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .testTag("video_preview_canvas")
                .clickable { onTogglePlayPause() }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val deltaNormalized = dragAmount.y / size.height
                        val newOffset = (style.verticalOffset + deltaNormalized * 2f).coerceIn(-0.7f, 0.7f)
                        onVerticalOffsetChange(newOffset)
                    }
                },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // 1. Draw Background
                    if (style.backgroundPreset == BackgroundPreset.SOLID_CUSTOM) {
                        drawRect(color = Color(style.customSolidBgColor))
                    } else {
                        val (bgGradStart, bgGradEnd) = when (style.backgroundPreset) {
                            BackgroundPreset.GRADIENT_NEON -> Pair(Color(0xFF120E2D), Color(0xFF0A304B))
                            BackgroundPreset.GRADIENT_SUNSET -> Pair(Color(0xFF2D0C20), Color(0xFF41180F))
                            BackgroundPreset.DARK_STUDIO -> Pair(Color(0xFF161424), Color(0xFF0C0A14))
                            BackgroundPreset.CYBER_PULSE -> Pair(Color(0xFF260C3C), Color(0xFF0E081E))
                            BackgroundPreset.SOLID_EMERALD -> Pair(Color(0xFF082820), Color(0xFF041410))
                            BackgroundPreset.MINIMAL_BLACK, BackgroundPreset.SOLID_CUSTOM, BackgroundPreset.CUSTOM_MEDIA -> Pair(Color(0xFF08080C), Color(0xFF0F0F16))
                        }

                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(bgGradStart, bgGradEnd),
                                startY = 0f,
                                endY = h
                            )
                        )
                    }

                    // Ambient sound wave aura
                    if (isPlaying) {
                        for (i in 0 until 4) {
                            val waveRadius = (w * 0.25f) + (i * 45f) + (pulseProgress * 60f)
                            drawCircle(
                                color = Color(0x1800E5FF),
                                radius = waveRadius,
                                center = Offset(w * 0.5f, h * 0.5f),
                                style = Stroke(width = 2.5f)
                            )
                        }
                    }

                    // 2. Draw Captions
                    if (currentSegment != null) {
                        val centerY = h * (0.5f + style.verticalOffset * 0.4f)
                        val textToRender = if (style.allCaps) currentSegment.text.uppercase(Locale.getDefault()) else currentSegment.text

                        val (composeFontFamily, fontWeight, fontStyle) = when (style.fontFamily) {
                            FontFamilyPreset.SANS_BOLD -> Triple(FontFamily.SansSerif, FontWeight.ExtraBold, FontStyle.Normal)
                            FontFamilyPreset.BANGLA_CALLIGRAPHIC -> Triple(FontFamily.Serif, FontWeight.Bold, FontStyle.Italic)
                            FontFamilyPreset.BANGLA_MODERN -> Triple(FontFamily.SansSerif, FontWeight.Bold, FontStyle.Normal)
                            FontFamilyPreset.MODERN_SANS -> Triple(FontFamily.SansSerif, FontWeight.Medium, FontStyle.Normal)
                            FontFamilyPreset.ELEGANT_SERIF -> Triple(FontFamily.Serif, FontWeight.Bold, FontStyle.Normal)
                            FontFamilyPreset.MONOSPACE -> Triple(FontFamily.Monospace, FontWeight.Bold, FontStyle.Normal)
                            FontFamilyPreset.HEAVY_IMPACT -> Triple(FontFamily.SansSerif, FontWeight.Black, FontStyle.Normal)
                        }

                        val fontSizeSp = (style.fontSizeSp * (w / 340f)).sp.value.coerceIn(16f, 38f).sp

                        when (style.template) {
                            CaptionStyleTemplate.HORMOZI_PUNCH -> {
                                val words = currentSegment.words.ifEmpty {
                                    textToRender.split("\\s+".toRegex()).map { WordTiming(it, currentSegment.startMs, currentSegment.endMs) }
                                }

                                val fullAnnotated = buildAnnotatedString {
                                    words.forEachIndexed { idx, wItem ->
                                        val isActive = currentActiveWord != null && currentActiveWord.word == wItem.word
                                        val wordColor = if (isActive && style.showWordHighlight) Color(style.highlightColor) else Color(style.textColor)
                                        withStyle(
                                            SpanStyle(
                                                color = wordColor,
                                                fontWeight = if (isActive) FontWeight.Black else FontWeight.ExtraBold,
                                                fontSize = if (isActive) (fontSizeSp.value * 1.15f).sp else fontSizeSp
                                            )
                                        ) {
                                            append(wItem.word)
                                        }
                                        if (idx < words.size - 1) append(" ")
                                    }
                                }

                                val textLayout = textMeasurer.measure(
                                    text = fullAnnotated,
                                    style = TextStyle(
                                        fontFamily = composeFontFamily,
                                        fontSize = fontSizeSp,
                                        textAlign = TextAlign.Center
                                    ),
                                    constraints = androidx.compose.ui.unit.Constraints(maxWidth = (w * 0.9f).toInt())
                                )

                                val textBoundsW = textLayout.size.width.toFloat()
                                val textBoundsH = textLayout.size.height.toFloat()
                                val startX = (w - textBoundsW) * 0.5f
                                val startY = centerY - (textBoundsH * 0.5f)

                                drawRoundRect(
                                    color = Color(0x66000000),
                                    topLeft = Offset(startX - 18f, startY - 10f),
                                    size = Size(textBoundsW + 36f, textBoundsH + 20f),
                                    cornerRadius = CornerRadius(16f, 16f)
                                )

                                drawText(
                                    textLayoutResult = textLayout,
                                    topLeft = Offset(startX, startY)
                                )
                            }
                            CaptionStyleTemplate.CAPCUT_BOUNCE -> {
                                val fullAnnotated = buildAnnotatedString {
                                    val words = currentSegment.words.ifEmpty {
                                        textToRender.split("\\s+".toRegex()).map { WordTiming(it, currentSegment.startMs, currentSegment.endMs) }
                                    }
                                    words.forEachIndexed { idx, wItem ->
                                        val isActive = currentActiveWord != null && currentActiveWord.word == wItem.word
                                        val col = if (isActive && style.showWordHighlight) Color(style.highlightColor) else Color(style.textColor)
                                        withStyle(
                                            SpanStyle(
                                                color = col,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = if (isActive) (fontSizeSp.value * 1.18f).sp else fontSizeSp
                                            )
                                        ) {
                                            append(wItem.word)
                                        }
                                        if (idx < words.size - 1) append(" ")
                                    }
                                }

                                val layout = textMeasurer.measure(
                                    text = fullAnnotated,
                                    style = TextStyle(
                                        fontFamily = composeFontFamily,
                                        textAlign = TextAlign.Center
                                    ),
                                    constraints = androidx.compose.ui.unit.Constraints(maxWidth = (w * 0.88f).toInt())
                                )
                                val sx = (w - layout.size.width) * 0.5f
                                val sy = centerY - (layout.size.height * 0.5f)

                                drawText(textLayoutResult = layout, topLeft = Offset(sx, sy))
                            }
                            CaptionStyleTemplate.BOXED_HIGHLIGHT -> {
                                val words = currentSegment.words.ifEmpty {
                                    textToRender.split("\\s+".toRegex()).map { WordTiming(it, currentSegment.startMs, currentSegment.endMs) }
                                }
                                val fullAnnotated = buildAnnotatedString {
                                    words.forEachIndexed { idx, wItem ->
                                        val isActive = currentActiveWord != null && currentActiveWord.word == wItem.word
                                        val col = if (isActive) Color.Black else Color(style.textColor)
                                        withStyle(
                                            SpanStyle(
                                                color = col,
                                                background = if (isActive && style.showWordHighlight) Color(style.highlightColor) else Color.Transparent,
                                                fontWeight = FontWeight.Black,
                                                fontSize = fontSizeSp
                                            )
                                        ) {
                                            append(" ${wItem.word} ")
                                        }
                                        if (idx < words.size - 1) append(" ")
                                    }
                                }
                                val layout = textMeasurer.measure(
                                    text = fullAnnotated,
                                    style = TextStyle(fontFamily = composeFontFamily, textAlign = TextAlign.Center),
                                    constraints = androidx.compose.ui.unit.Constraints(maxWidth = (w * 0.9f).toInt())
                                )
                                drawText(textLayoutResult = layout, topLeft = Offset((w - layout.size.width) * 0.5f, centerY - layout.size.height * 0.5f))
                            }
                            CaptionStyleTemplate.MINIMAL_PILL -> {
                                val layout = textMeasurer.measure(
                                    text = AnnotatedString(textToRender),
                                    style = TextStyle(
                                        color = Color(style.textColor),
                                        fontFamily = composeFontFamily,
                                        fontWeight = fontWeight,
                                        fontSize = fontSizeSp,
                                        textAlign = TextAlign.Center
                                    ),
                                    constraints = androidx.compose.ui.unit.Constraints(maxWidth = (w * 0.85f).toInt())
                                )

                                val tw = layout.size.width.toFloat()
                                val th = layout.size.height.toFloat()
                                val sx = (w - tw) * 0.5f
                                val sy = centerY - (th * 0.5f)

                                drawRoundRect(
                                    color = Color(0xB012111D),
                                    topLeft = Offset(sx - 24f, sy - 14f),
                                    size = Size(tw + 48f, th + 28f),
                                    cornerRadius = CornerRadius(24f, 24f)
                                )

                                drawText(textLayoutResult = layout, topLeft = Offset(sx, sy))
                            }
                            CaptionStyleTemplate.KARAOKE_FLOW -> {
                                val fullAnnotated = buildAnnotatedString {
                                    val words = currentSegment.words.ifEmpty {
                                        textToRender.split("\\s+".toRegex()).map { WordTiming(it, currentSegment.startMs, currentSegment.endMs) }
                                    }
                                    words.forEachIndexed { idx, wItem ->
                                        val isPassedOrActive = currentPositionMs >= wItem.startMs
                                        val col = if (isPassedOrActive) Color(style.highlightColor) else Color(0x99FFFFFF)
                                        withStyle(
                                            SpanStyle(
                                                color = col,
                                                fontWeight = if (isPassedOrActive) FontWeight.ExtraBold else FontWeight.Normal,
                                                fontSize = fontSizeSp
                                            )
                                        ) {
                                            append(wItem.word)
                                        }
                                        if (idx < words.size - 1) append(" ")
                                    }
                                }

                                val layout = textMeasurer.measure(
                                    text = fullAnnotated,
                                    style = TextStyle(
                                        fontFamily = composeFontFamily,
                                        textAlign = TextAlign.Center
                                    ),
                                    constraints = androidx.compose.ui.unit.Constraints(maxWidth = (w * 0.9f).toInt())
                                )
                                drawText(
                                    textLayoutResult = layout,
                                    topLeft = Offset((w - layout.size.width) * 0.5f, centerY - layout.size.height * 0.5f)
                                )
                            }
                            CaptionStyleTemplate.NEON_CYBER -> {
                                val layout = textMeasurer.measure(
                                    text = AnnotatedString(textToRender),
                                    style = TextStyle(
                                        color = Color.White,
                                        fontFamily = composeFontFamily,
                                        fontWeight = FontWeight.Black,
                                        fontSize = fontSizeSp,
                                        textAlign = TextAlign.Center
                                    ),
                                    constraints = androidx.compose.ui.unit.Constraints(maxWidth = (w * 0.88f).toInt())
                                )
                                val tw = layout.size.width.toFloat()
                                val th = layout.size.height.toFloat()
                                val sx = (w - tw) * 0.5f
                                val sy = centerY - (th * 0.5f)

                                drawRoundRect(
                                    color = Color(0x3300E5FF),
                                    topLeft = Offset(sx - 20f, sy - 12f),
                                    size = Size(tw + 40f, th + 24f),
                                    cornerRadius = CornerRadius(12f, 12f)
                                )
                                drawRoundRect(
                                    color = Color(0xFF00E5FF),
                                    topLeft = Offset(sx - 20f, sy - 12f),
                                    size = Size(tw + 40f, th + 24f),
                                    cornerRadius = CornerRadius(12f, 12f),
                                    style = Stroke(width = 3f)
                                )

                                drawText(textLayoutResult = layout, topLeft = Offset(sx, sy))
                            }
                            CaptionStyleTemplate.CINEMATIC -> {
                                val layout = textMeasurer.measure(
                                    text = AnnotatedString(textToRender),
                                    style = TextStyle(
                                        color = Color(0xFFFFF7E6),
                                        fontFamily = FontFamily.Serif,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = (fontSizeSp.value * 0.92f).sp,
                                        textAlign = TextAlign.Center
                                    ),
                                    constraints = androidx.compose.ui.unit.Constraints(maxWidth = (w * 0.85f).toInt())
                                )
                                val sx = (w - layout.size.width) * 0.5f
                                val sy = centerY - (layout.size.height * 0.5f)

                                drawText(textLayoutResult = layout, topLeft = Offset(sx, sy))
                            }
                        }
                    }
                }

                // Play overlay if paused
                if (!isPlaying) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x33000000)),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(32.dp),
                            color = Color(0xCC1A1828),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play video preview",
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(14.dp)
                            )
                        }
                    }
                }

                // Drag to position badge hint
                Surface(
                    color = Color(0x99000000),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "ড্র্যাগ করে টেক্সট সরান",
                            fontSize = 11.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
