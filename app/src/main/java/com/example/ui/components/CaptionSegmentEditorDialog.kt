package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.CaptionSegment

@Composable
fun CaptionSegmentEditorDialog(
    segment: CaptionSegment,
    onDismiss: () -> Unit,
    onSave: (String, Long, Long) -> Unit,
    onSplit: (Long) -> Unit,
    onDelete: () -> Unit
) {
    var text by remember(segment.id) { mutableStateOf(segment.text) }
    var startMs by remember(segment.id) { mutableLongStateOf(segment.startMs) }
    var endMs by remember(segment.id) { mutableLongStateOf(segment.endMs) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1E1A30),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "ক্যাপশন এডিট ও সময় সমন্বয়",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00E5FF)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Text Field
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("ক্যাপশন টেক্সট") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00E5FF),
                        unfocusedBorderColor = Color(0xFF4A4462)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("caption_text_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Start Time adjuster
                Text(text = "শুরুর সময়: ${startMs}ms (${startMs / 1000f}s)", fontSize = 12.sp, color = Color(0xFFAAA5C2))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = { startMs = (startMs - 200).coerceAtLeast(0L) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("-200ms", fontSize = 11.sp)
                    }
                    FilledTonalButton(
                        onClick = { startMs = (startMs + 200).coerceAtMost(endMs - 100L) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("+200ms", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // End Time adjuster
                Text(text = "শেষের সময়: ${endMs}ms (${endMs / 1000f}s)", fontSize = 12.sp, color = Color(0xFFAAA5C2))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = { endMs = (endMs - 200).coerceAtLeast(startMs + 100L) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("-200ms", fontSize = 11.sp)
                    }
                    FilledTonalButton(
                        onClick = { endMs += 200 },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("+200ms", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Split & Delete actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val mid = (startMs + endMs) / 2
                            onSplit(mid)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFD600))
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.CallSplit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("দ্বিখণ্ডিত", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            onDelete()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF3D71))
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("মুছুন", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Save and Cancel buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("বাতিল", color = Color(0xFFCCCCCC))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSave(text, startMs, endMs)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00E5FF),
                            contentColor = Color(0xFF0C0A14)
                        ),
                        modifier = Modifier.testTag("save_caption_button")
                    ) {
                        Text("সংরক্ষণ", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
