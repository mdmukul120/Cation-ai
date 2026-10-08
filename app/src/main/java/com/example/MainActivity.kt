package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.EditorTab
import com.example.viewmodel.EditorViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: EditorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                CapGrokApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CapGrokApp(viewModel: EditorViewModel) {
    val project by viewModel.project.collectAsStateWithLifecycle()
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val isPlaying by viewModel.audioService.isPlaying.collectAsStateWithLifecycle()
    val currentPositionMs by viewModel.audioService.currentPositionMs.collectAsStateWithLifecycle()
    val isRecording by viewModel.audioService.isRecording.collectAsStateWithLifecycle()
    val recordingAmplitude by viewModel.audioService.recordingAmplitude.collectAsStateWithLifecycle()
    val isAiProcessing by viewModel.isAiProcessing.collectAsStateWithLifecycle()
    val aiStatusMessage by viewModel.aiStatusMessage.collectAsStateWithLifecycle()
    val exportState by viewModel.exportState.collectAsStateWithLifecycle()
    val selectedSegmentId by viewModel.selectedSegmentId.collectAsStateWithLifecycle()
    val geminiKey by viewModel.geminiApiKey.collectAsStateWithLifecycle()
    val grokKey by viewModel.grokApiKey.collectAsStateWithLifecycle()

    var showExportDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showAddCaptionDialog by remember { mutableStateOf(false) }

    val editingSegment = project.captions.find { it.id == selectedSegmentId }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFF0E0C1A),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF00E5FF),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Subtitles,
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
                                text = "CapGrok",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                text = "Grok Voice + Gemini AI",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFFFD600)
                            )
                        }
                    }
                },
                actions = {
                    // Aspect ratio quick switch
                    IconButton(
                        onClick = {
                            val nextRatio = when (project.style.aspectRatio) {
                                VideoAspectRatio.NINE_SIXTEEN -> VideoAspectRatio.ONE_ONE
                                VideoAspectRatio.ONE_ONE -> VideoAspectRatio.SIXTEEN_NINE
                                VideoAspectRatio.SIXTEEN_NINE -> VideoAspectRatio.NINE_SIXTEEN
                            }
                            viewModel.setAspectRatio(nextRatio)
                        },
                        modifier = Modifier.testTag("ratio_switch_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AspectRatio,
                            contentDescription = "Switch aspect ratio",
                            tint = Color(0xFFCCCCCC)
                        )
                    }

                    // Settings Dialog button
                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "AI Settings",
                            tint = Color(0xFFCCCCCC)
                        )
                    }

                    // Export Button
                    Button(
                        onClick = { showExportDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00E5FF),
                            contentColor = Color(0xFF0C0A14)
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("export_top_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "এক্সপোর্ট",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF141124)
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. Video Player & Preview Canvas Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.95f)
                    .background(Color(0xFF0E0C1A)),
                contentAlignment = Alignment.Center
            ) {
                VideoPreviewCanvas(
                    project = project,
                    currentPositionMs = currentPositionMs,
                    isPlaying = isPlaying,
                    onTogglePlayPause = { viewModel.audioService.togglePlayPause() },
                    onVerticalOffsetChange = { viewModel.setVerticalOffset(it) },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // 2. Editor Navigation Tabs (CapCut style)
            PrimaryTabRow(
                selectedTabIndex = currentTab.ordinal,
                containerColor = Color(0xFF161326),
                contentColor = Color(0xFF00E5FF),
                divider = { HorizontalDivider(color = Color(0xFF262040)) }
            ) {
                EditorTab.entries.forEach { tab ->
                    val isSelected = currentTab == tab
                    Tab(
                        selected = isSelected,
                        onClick = { viewModel.setTab(tab) },
                        text = {
                            Text(
                                text = when (tab) {
                                    EditorTab.TIMELINE -> "টাইমলাইন"
                                    EditorTab.STYLE -> "টেমপ্লেট ও স্টাইল"
                                    EditorTab.AI_INSIGHTS -> "এআই বিশ্লেষণ"
                                    EditorTab.AUDIO_IMPORT -> "অডিও ও রেকর্ড"
                                },
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color(0xFF00E5FF) else Color(0xFFAAA5C2)
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = when (tab) {
                                    EditorTab.TIMELINE -> Icons.Default.Timeline
                                    EditorTab.STYLE -> Icons.Default.Palette
                                    EditorTab.AI_INSIGHTS -> Icons.Default.AutoAwesome
                                    EditorTab.AUDIO_IMPORT -> Icons.Default.Mic
                                },
                                contentDescription = tab.title,
                                modifier = Modifier.size(18.dp),
                                tint = if (isSelected) Color(0xFF00E5FF) else Color(0xFF75708E)
                            )
                        },
                        modifier = Modifier.testTag("tab_${tab.name.lowercase()}")
                    )
                }
            }

            // 3. Tab Contents
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.05f)
                    .background(Color(0xFF141220))
            ) {
                when (currentTab) {
                    EditorTab.TIMELINE -> {
                        TimelineTrack(
                            durationMs = project.audioDurationMs,
                            currentPositionMs = currentPositionMs,
                            isPlaying = isPlaying,
                            captions = project.captions,
                            selectedSegmentId = selectedSegmentId,
                            onSeek = { viewModel.audioService.seekTo(it) },
                            onTogglePlayPause = { viewModel.audioService.togglePlayPause() },
                            onSelectSegment = { viewModel.setSelectedSegment(it) },
                            onAddSegmentClick = { showAddCaptionDialog = true },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    EditorTab.STYLE -> {
                        StyleAndTemplateSelector(
                            style = project.style,
                            onTemplateSelect = { viewModel.setTemplate(it) },
                            onAspectRatioSelect = { viewModel.setAspectRatio(it) },
                            onBackgroundSelect = { viewModel.setBackground(it) },
                            onFontSelect = { viewModel.setFont(it) },
                            onFontSizeChange = { viewModel.setFontSize(it) },
                            onVerticalOffsetChange = { viewModel.setVerticalOffset(it) },
                            onHighlightColorChange = { viewModel.updateStyle { s -> s.copy(highlightColor = it) } },
                            onToggleAllCaps = { viewModel.updateStyle { s -> s.copy(allCaps = it) } },
                            onToggleWordHighlight = { viewModel.updateStyle { s -> s.copy(showWordHighlight = it) } },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    EditorTab.AI_INSIGHTS -> {
                        AiSummaryAndVoiceInsights(
                            summary = project.summary,
                            voiceAnalysis = project.voiceAnalysis,
                            isProcessing = isAiProcessing,
                            processingMessage = aiStatusMessage,
                            onReAnalyzeClick = {
                                val transcript = project.captions.joinToString(" ") { it.text }
                                viewModel.runAiProcessing(
                                    transcriptPrompt = transcript,
                                    durationMs = project.audioDurationMs
                                )
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    EditorTab.AUDIO_IMPORT -> {
                        AudioRecordAndImportSheet(
                            isRecording = isRecording,
                            recordingAmplitude = recordingAmplitude,
                            onStartRecording = { viewModel.startRecording() },
                            onStopRecording = { viewModel.stopRecordingAndImport() },
                            onAudioFileSelected = { uri, name ->
                                viewModel.onAudioSelected(uri, name)
                            },
                            onSelectSamplePreset = { viewModel.loadSample(it) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }

    // --- Modals & Dialogs ---

    // 1. Caption Segment Editor Dialog
    if (editingSegment != null) {
        CaptionSegmentEditorDialog(
            segment = editingSegment,
            onDismiss = { viewModel.setSelectedSegment(null) },
            onSave = { newText, startMs, endMs ->
                viewModel.updateCaptionSegment(editingSegment.id, newText, startMs, endMs)
            },
            onSplit = { splitAtMs ->
                viewModel.splitCaptionSegment(editingSegment.id, splitAtMs)
            },
            onDelete = {
                viewModel.deleteCaptionSegment(editingSegment.id)
            }
        )
    }

    // 2. Add New Caption Dialog
    if (showAddCaptionDialog) {
        var newText by remember { mutableStateOf("") }
        val defaultStart = currentPositionMs
        val defaultEnd = (currentPositionMs + 2500L).coerceAtMost(project.audioDurationMs)

        AlertDialog(
            onDismissRequest = { showAddCaptionDialog = false },
            title = { Text("নতুন ক্যাপশন বাক্য যোগ করুন", color = Color(0xFF00E5FF)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newText,
                        onValueChange = { newText = it },
                        label = { Text("ক্যাপশন টেক্সট") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "টাইমস্ট্যাম্প: ${defaultStart}ms - ${defaultEnd}ms",
                        fontSize = 12.sp,
                        color = Color(0xFFAAA5C2)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newText.isNotBlank()) {
                            viewModel.addCaptionSegment(defaultStart, defaultEnd, newText.trim())
                            showAddCaptionDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color.Black)
                ) {
                    Text("যোগ করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCaptionDialog = false }) {
                    Text("বাতিল", color = Color.White)
                }
            },
            containerColor = Color(0xFF1E1A30)
        )
    }

    // 3. Export & Share Dialog
    if (showExportDialog || exportState.isExporting || exportState.exportedFile != null) {
        ExportAndShareDialog(
            exportState = exportState,
            ffmpegScript = viewModel.getFFmpegScript(),
            srtSubtitles = viewModel.getSrtSubtitles(),
            onStartExport = { viewModel.startExport() },
            onDismiss = {
                showExportDialog = false
                viewModel.dismissExport()
            }
        )
    }

    // 4. Settings Dialog
    if (showSettingsDialog) {
        SettingsDialog(
            currentGeminiKey = geminiKey,
            currentGrokKey = grokKey,
            onSaveKeys = { gemKey, grKey ->
                viewModel.setGeminiApiKey(gemKey)
                viewModel.setGrokApiKey(grKey)
            },
            onDismiss = { showSettingsDialog = false }
        )
    }
}
