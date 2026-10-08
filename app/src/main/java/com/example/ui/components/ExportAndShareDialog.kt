package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.example.viewmodel.ExportState
import java.io.File

@Composable
fun ExportAndShareDialog(
    exportState: ExportState,
    ffmpegScript: String,
    srtSubtitles: String,
    onStartExport: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var showFFmpegScript by remember { mutableStateOf(false) }
    var showSrtScript by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF1E1A32),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ভিডিও এক্সপোর্ট ও শেয়ার",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00E5FF)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Progress state
                if (exportState.isExporting) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            progress = { exportState.progress },
                            modifier = Modifier.size(56.dp),
                            color = Color(0xFF00E5FF),
                            strokeWidth = 5.dp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        val percent = (exportState.progress * 100).toInt()
                        Text(
                            text = "$percent%",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD600)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = exportState.statusMessage,
                            fontSize = 12.sp,
                            color = Color(0xFFAAA5C2)
                        )
                    }
                } else if (exportState.exportedFile != null) {
                    // Success View
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF193226)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF00E676))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(text = "ভিডিও তৈরি সম্পন্ন!", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                Text(
                                    text = exportState.exportedFile.name,
                                    fontSize = 11.sp,
                                    color = Color(0xFF80CBC4)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Play Video & Share Video Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { playVideo(context, exportState.exportedFile) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00E5FF),
                                contentColor = Color(0xFF0C0A14)
                            )
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("প্লে করুন", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { shareVideo(context, exportState.exportedFile) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFFD600),
                                contentColor = Color(0xFF0C0A14)
                            )
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("শেয়ার", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Start Export Button
                    Text(
                        text = "উচ্চমানের MP4 ভিডিও (720p HD) ফরম্যাটে দ্রুত রেন্ডার করুন। ব্যাকগ্রাউন্ড মোশন এবং ক্যাপশন ওভারলে যুক্ত হবে।",
                        fontSize = 12.sp,
                        color = Color(0xFFCCC8E0),
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onStartExport,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("start_export_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00E5FF),
                            contentColor = Color(0xFF0C0A14)
                        )
                    ) {
                        Icon(imageVector = Icons.Default.VideoCall, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "ভিডিও রেন্ডারিং শুরু করুন (MP4)", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = Color(0xFF2E294A))
                Spacer(modifier = Modifier.height(14.dp))

                // --- FFmpeg Section ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "FFmpeg রেন্ডারিং কমান্ড ও স্ক্রিপ্ট",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    TextButton(onClick = { showFFmpegScript = !showFFmpegScript }) {
                        Text(if (showFFmpegScript) "লুকান" else "দেখান", color = Color(0xFF00E5FF), fontSize = 11.sp)
                    }
                }

                AnimatedVisibility(visible = showFFmpegScript) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF121020),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = ffmpegScript,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF80D8FF),
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = {
                                copyToClipboard(context, "CapGrok FFmpeg Command", ffmpegScript)
                                Toast.makeText(context, "FFmpeg স্ক্রিপ্ট কপি করা হয়েছে!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.align(Alignment.End),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("কমান্ড কপি করুন", fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // --- SRT Subtitles Section ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SRT সাবটাইটেল ফাইল এক্সপোর্ট",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    TextButton(onClick = { showSrtScript = !showSrtScript }) {
                        Text(if (showSrtScript) "লুকান" else "দেখান", color = Color(0xFFFFD600), fontSize = 11.sp)
                    }
                }

                AnimatedVisibility(visible = showSrtScript) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF121020),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = srtSubtitles,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFFFFE082),
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = {
                                copyToClipboard(context, "CapGrok SRT Subtitles", srtSubtitles)
                                Toast.makeText(context, "SRT সাবটাইটেল কপি করা হয়েছে!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.align(Alignment.End),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("SRT কপি করুন", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

private fun playVideo(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "video/mp4")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "ভিডিও প্লেয়ার খোলা যায়নি: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun shareVideo(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "CapGrok AI Video")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "ভিডিও শেয়ার করুন"))
    } catch (e: Exception) {
        Toast.makeText(context, "শেয়ার করা সম্ভব হয়নি: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
}
