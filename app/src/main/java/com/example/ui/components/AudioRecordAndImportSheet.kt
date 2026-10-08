package com.example.ui.components

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AudioRecordAndImportSheet(
    isRecording: Boolean,
    recordingAmplitude: Int,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onAudioFileSelected: (Uri, String) -> Unit,
    onSelectSamplePreset: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasMicPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasMicPermission = isGranted
        if (isGranted) {
            onStartRecording()
        }
    }

    val audioFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = "Uploaded_Audio_${System.currentTimeMillis()}.mp3"
            onAudioFileSelected(uri, fileName)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF141220))
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "অডিও যুক্ত করুন বা ভয়েস রেকর্ড করুন",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF00E5FF)
        )
        Spacer(modifier = Modifier.height(14.dp))

        // --- 1. Audio File Upload Card ---
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1D1932)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { audioFilePickerLauncher.launch("audio/*") }
                .testTag("upload_audio_card")
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF2B244A),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AudioFile,
                        contentDescription = "Upload audio",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ডিভাইস থেকে অডিও ফাইল আনুন",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "MP3, WAV, M4A, AAC যে কোনো সাইজের অডিও সমর্থন করে",
                        fontSize = 11.sp,
                        color = Color(0xFFAAA5C2)
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color(0xFF88849E)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 2. Live Voice Recorder Card ---
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1D1932)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("record_voice_card")
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isRecording) "রেকর্ডিং চলছে... আপনার বক্তব্য বলুন" else "সরাসরি মাইক্রোফোনে কথা রেকর্ড করুন",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isRecording) Color(0xFFFF3D71) else Color.White
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Live waveform amplitude animation when recording
                AnimatedVisibility(visible = isRecording) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF141024))
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val count = 28
                            val step = size.width / count
                            val midY = size.height * 0.5f
                            val normalizedAmp = (recordingAmplitude / 32767f).coerceIn(0.1f, 1f)

                            for (i in 0 until count) {
                                val x = i * step + step * 0.5f
                                val barH = (size.height * 0.8f * normalizedAmp * (0.4f + (i % 3) * 0.3f)).coerceAtLeast(6f)
                                drawLine(
                                    color = Color(0xFFFF3D71),
                                    start = Offset(x, midY - barH * 0.5f),
                                    end = Offset(x, midY + barH * 0.5f),
                                    strokeWidth = 4f
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Record / Stop Button
                FilledIconButton(
                    onClick = {
                        if (isRecording) {
                            onStopRecording()
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .testTag("record_button"),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isRecording) Color(0xFFFF3D71) else Color(0xFF00E5FF),
                        contentColor = Color(0xFF0C0A14)
                    )
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = if (isRecording) "Stop Recording" else "Start Recording",
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isRecording) "থামাতে ট্যাপ করুন (স্বয়ংক্রিয় ক্যাপশন হবে)" else "রেকর্ড শুরু করতে ট্যাপ করুন",
                    fontSize = 11.sp,
                    color = Color(0xFFAAA5C2)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- 3. Preloaded Sample Audio Demos ---
        Text(
            text = "তাত্ক্ষণিক পরীক্ষার জন্য প্রস্তুত অডিও ডেমো",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(10.dp))

        val samples = listOf(
            Triple(
                "স্বপ্নের জয়যাত্রা (Motivational Speech)",
                "১৮ সেকেন্ড • বাংলা মোটিভেশনাল বক্তব্য • উচ্চ শক্তি",
                Color(0xFFFFD600)
            ),
            Triple(
                "AI Revolution & Future Tech",
                "১৫ সেকেন্ড • ইংরেজি টেক স্পিচ • Grok & Gemini ফিচার",
                Color(0xFF00E5FF)
            ),
            Triple(
                "নদীর কূলের কবিতা ও অনুভূতি",
                "১২ সেকেন্ড • বাংলা কবিতা ও শান্ত সুর • স্নিগ্ধ ভাবাবেগ",
                Color(0xFFFF3D71)
            )
        )

        samples.forEachIndexed { index, (title, desc, color) ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1B172E),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C2648)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clickable { onSelectSamplePreset(index) }
                    .testTag("sample_preset_$index")
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF262040),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = desc, fontSize = 10.sp, color = Color(0xFFAAA5C2))
                    }
                    Button(
                        onClick = { onSelectSamplePreset(index) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2A2346),
                            contentColor = color
                        )
                    ) {
                        Text("লোড", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
