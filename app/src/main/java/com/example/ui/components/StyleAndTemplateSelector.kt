package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*

@Composable
fun StyleAndTemplateSelector(
    style: VideoStyle,
    onTemplateSelect: (CaptionStyleTemplate) -> Unit,
    onAspectRatioSelect: (VideoAspectRatio) -> Unit,
    onBackgroundSelect: (BackgroundPreset) -> Unit,
    onFontSelect: (FontFamilyPreset) -> Unit,
    onFontSizeChange: (Int) -> Unit,
    onVerticalOffsetChange: (Float) -> Unit,
    onHighlightColorChange: (Long) -> Unit,
    onToggleAllCaps: (Boolean) -> Unit,
    onToggleWordHighlight: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF141220))
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // --- 1. Subtitle Templates ---
        Text(
            text = "ক্যাপশন ও সাবটাইটেল টেমপ্লেট",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF00E5FF)
        )
        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(CaptionStyleTemplate.entries) { tmpl ->
                val isSelected = style.template == tmpl
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) Color(0xFF2A2346) else Color(0xFF1C192E),
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) Color(0xFFFFD600) else Color(0xFF332E50)
                    ),
                    modifier = Modifier
                        .width(150.dp)
                        .testTag("template_${tmpl.name.lowercase()}")
                        .clickable { onTemplateSelect(tmpl) }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = tmpl.displayName,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color(0xFFFFD600) else Color.White
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFFFFD600),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = tmpl.description,
                            fontSize = 11.sp,
                            color = Color(0xFFAAA5C2),
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- 2. Video Aspect Ratio ---
        Text(
            text = "ভিডিও রেশিও (Aspect Ratio)",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            VideoAspectRatio.entries.forEach { ratio ->
                val isSelected = style.aspectRatio == ratio
                FilledTonalButton(
                    onClick = { onAspectRatioSelect(ratio) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (isSelected) Color(0xFF00E5FF) else Color(0xFF221E38),
                        contentColor = if (isSelected) Color(0xFF0C0A14) else Color.White
                    )
                ) {
                    Text(text = ratio.label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- 3. Background Aesthetic Presets ---
        Text(
            text = "ভিডিও ব্যাকগ্রাউন্ড প্রিসেট",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(BackgroundPreset.entries) { bg ->
                val isSelected = style.backgroundPreset == bg
                FilterChip(
                    selected = isSelected,
                    onClick = { onBackgroundSelect(bg) },
                    label = { Text(bg.title, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF7C4DFF),
                        selectedLabelColor = Color.White,
                        containerColor = Color(0xFF1E1A32),
                        labelColor = Color(0xFFCCC8E0)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- 4. Font Typography ---
        Text(
            text = "ফন্ট স্টাইল (Typography)",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(FontFamilyPreset.entries) { font ->
                val isSelected = style.fontFamily == font
                FilterChip(
                    selected = isSelected,
                    onClick = { onFontSelect(font) },
                    label = { Text(font.title, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFFFD600),
                        selectedLabelColor = Color.Black,
                        containerColor = Color(0xFF1E1A32),
                        labelColor = Color(0xFFCCC8E0)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- 5. Font Size & Position Sliders ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "ফন্ট সাইজ (${style.fontSizeSp}sp)", fontSize = 13.sp, color = Color(0xFFCCC8E0))
        }
        Slider(
            value = style.fontSizeSp.toFloat(),
            onValueChange = { onFontSizeChange(it.toInt()) },
            valueRange = 16f..36f,
            steps = 20,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF00E5FF),
                activeTrackColor = Color(0xFF00E5FF)
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "সাবটাইটেল পজিশন (উল্লম্ব অবস্থান)", fontSize = 13.sp, color = Color(0xFFCCC8E0))
        }
        Slider(
            value = style.verticalOffset,
            onValueChange = onVerticalOffsetChange,
            valueRange = -0.7f..0.7f,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFFFFD600),
                activeTrackColor = Color(0xFFFFD600)
            )
        )

        Spacer(modifier = Modifier.height(14.dp))

        // --- 6. Active Highlight Colors ---
        Text(
            text = "অ্যাক্টিভ ওয়ার্ড হাইলাইট কালার",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))

        val highlightColors = listOf(
            0xFFFFEB3B, // Neon Yellow
            0xFF00E5FF, // Electric Cyan
            0xFFFF3D71, // Vibrant Coral
            0xFF00E676, // Bright Lime
            0xFFFF9100, // Punch Orange
            0xFFFFFFFF  // Crisp White
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            highlightColors.forEach { colLong ->
                val isSelected = style.highlightColor == colLong
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(colLong))
                        .clickable { onHighlightColorChange(colLong) }
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) Color.White else Color(0x66FFFFFF),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 7. Toggles ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "সব অক্ষর বড় হাতের (ALL CAPS)", fontSize = 13.sp, color = Color.White)
            Switch(
                checked = style.allCaps,
                onCheckedChange = onToggleAllCaps,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF00E5FF),
                    checkedTrackColor = Color(0xFF1E3A4B)
                )
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "শব্দভিত্তিক হাইলাইট এনিমেশন", fontSize = 13.sp, color = Color.White)
            Switch(
                checked = style.showWordHighlight,
                onCheckedChange = onToggleWordHighlight,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFFFFD600),
                    checkedTrackColor = Color(0xFF423B18)
                )
            )
        }
    }
}
