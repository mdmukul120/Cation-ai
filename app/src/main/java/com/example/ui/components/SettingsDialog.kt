package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
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

@Composable
fun SettingsDialog(
    currentGeminiKey: String,
    currentGrokKey: String,
    onSaveKeys: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var geminiKey by remember { mutableStateOf(currentGeminiKey) }
    var grokKey by remember { mutableStateOf(currentGrokKey) }

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
                        text = "এআই সেটিংস ও API চাবি",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00E5FF)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "অ্যাপটি ডিফল্টভাবে স্থানীয় স্মার্ট বিশ্লেষণ ইঞ্জিন দিয়ে সচল থাকে। রিয়েল-টাইম ক্লাউড এআই-এর জন্য আপনার কী যুক্ত করতে পারেন:",
                    fontSize = 12.sp,
                    color = Color(0xFFCCC8E0),
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Gemini API Key
                Text(
                    text = "Google Gemini API Key (সারসংক্ষেপ ও ক্যাপশন)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = geminiKey,
                    onValueChange = { geminiKey = it },
                    placeholder = { Text("AI Studio Gemini Key", color = Color(0xFF77738C)) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = Color(0xFF00E5FF))
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00E5FF),
                        unfocusedBorderColor = Color(0xFF4A4462)
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("gemini_key_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Grok API Key
                Text(
                    text = "xAI Grok API Key (ভয়েস ক্যাডেন্স ও অডিও বিশ্লেষণ)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = grokKey,
                    onValueChange = { grokKey = it },
                    placeholder = { Text("xAI Grok API Key", color = Color(0xFF77738C)) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = Color(0xFFFF5722))
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFF5722),
                        unfocusedBorderColor = Color(0xFF4A4462)
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("grok_key_input")
                )

                Spacer(modifier = Modifier.height(20.dp))

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
                            onSaveKeys(geminiKey.trim(), grokKey.trim())
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00E5FF),
                            contentColor = Color(0xFF0C0A14)
                        ),
                        modifier = Modifier.testTag("save_settings_button")
                    ) {
                        Text("সংরক্ষণ করুন", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
