package com.example.service

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

class AudioService(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var mediaRecorder: MediaRecorder? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var currentRecordedFile: File? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingAmplitude = MutableStateFlow(0)
    val recordingAmplitude: StateFlow<Int> = _recordingAmplitude.asStateFlow()

    // Live speech recognition text as user speaks into mic
    private val _liveRecognizedText = MutableStateFlow("")
    val liveRecognizedText: StateFlow<String> = _liveRecognizedText.asStateFlow()

    private var progressJob: Job? = null
    private var recordingAmplitudeJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var recordingStartTimeMs: Long = 0L
    var lastRecordedDurationMs: Long = 0L
        private set

    fun loadAudio(uriString: String?, durationHintMs: Long = 0L) {
        stopPlayback()
        if (uriString.isNullOrBlank()) {
            _durationMs.value = durationHintMs.coerceAtLeast(1000L)
            return
        }

        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                if (uriString.startsWith("/")) {
                    setDataSource(uriString)
                } else {
                    setDataSource(context, Uri.parse(uriString))
                }
                prepare()
            }
            mediaPlayer = player

            // Accurate duration from MediaPlayer or MediaMetadataRetriever
            val playerDur = player.duration.toLong()
            val metaDur = getAccurateDuration(uriString)
            val accurateDuration = when {
                playerDur > 0 -> playerDur
                metaDur > 0 -> metaDur
                durationHintMs > 0 -> durationHintMs
                else -> 1000L
            }
            _durationMs.value = accurateDuration.coerceAtLeast(500L)
            _currentPositionMs.value = 0L

            player.setOnCompletionListener {
                _isPlaying.value = false
                _currentPositionMs.value = _durationMs.value
                stopProgressTracking()
            }
        } catch (e: Exception) {
            Log.e("AudioService", "Error loading audio: ${e.message}", e)
            val metaDur = getAccurateDuration(uriString)
            val dur = when {
                metaDur > 0 -> metaDur
                durationHintMs > 0 -> durationHintMs
                else -> 1000L
            }
            _durationMs.value = dur.coerceAtLeast(500L)
        }
    }

    fun getAccurateDuration(uriOrPath: String?): Long {
        if (uriOrPath.isNullOrBlank()) return 0L
        return try {
            val retriever = MediaMetadataRetriever()
            if (uriOrPath.startsWith("/")) {
                retriever.setDataSource(uriOrPath)
            } else {
                retriever.setDataSource(context, Uri.parse(uriOrPath))
            }
            val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            durStr?.toLongOrNull() ?: 0L
        } catch (_: Exception) {
            0L
        }
    }

    /**
     * Copies a content:// URI from SAF to a local cache file so that MediaExtractor
     * and Gemini API can access direct file descriptors.
     */
    fun copyUriToLocalFile(uri: Uri): File {
        val extension = try {
            val mime = context.contentResolver.getType(uri)
            when {
                mime?.contains("wav", true) == true -> "wav"
                mime?.contains("mp4", true) == true || mime?.contains("m4a", true) == true -> "m4a"
                mime?.contains("aac", true) == true -> "aac"
                mime?.contains("ogg", true) == true -> "ogg"
                else -> "mp3"
            }
        } catch (_: Exception) {
            "mp3"
        }

        val cacheFile = File(context.cacheResolverDir(), "imported_audio_${System.currentTimeMillis()}.$extension")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(cacheFile).use { output ->
                input.copyTo(output)
            }
        }
        return cacheFile
    }

    fun play() {
        val player = mediaPlayer
        if (player != null) {
            try {
                if (_currentPositionMs.value >= _durationMs.value && _durationMs.value > 0) {
                    player.seekTo(0)
                    _currentPositionMs.value = 0L
                }
                player.start()
                _isPlaying.value = true
                startProgressTracking()
            } catch (e: Exception) {
                Log.e("AudioService", "Play failed: ${e.message}")
            }
        } else {
            // Virtual playback if audio file is synthetic
            _isPlaying.value = true
            startSyntheticPlayback()
        }
    }

    fun pause() {
        try {
            mediaPlayer?.pause()
        } catch (e: Exception) {
            Log.e("AudioService", "Pause failed: ${e.message}")
        }
        _isPlaying.value = false
        stopProgressTracking()
    }

    fun setSpeed(speed: Float) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mediaPlayer?.let { player ->
                    val params = player.playbackParams
                    params.speed = speed.coerceIn(0.5f, 2.5f)
                    player.playbackParams = params
                }
            }
        } catch (e: Exception) {
            Log.w("AudioService", "Error setting playback speed: ${e.message}")
        }
    }

    fun seekTo(positionMs: Long) {
        val clamped = positionMs.coerceIn(0L, _durationMs.value.coerceAtLeast(1000L))
        _currentPositionMs.value = clamped
        try {
            mediaPlayer?.seekTo(clamped.toInt())
        } catch (e: Exception) {
            Log.e("AudioService", "Seek failed: ${e.message}")
        }
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            pause()
        } else {
            play()
        }
    }

    fun stopPlayback() {
        pause()
        try {
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        _isPlaying.value = false
    }

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (_isPlaying.value) {
                val pos = mediaPlayer?.currentPosition?.toLong() ?: _currentPositionMs.value
                _currentPositionMs.value = pos
                delay(33) // ~30fps smooth timeline tracking
            }
        }
    }

    private fun startSyntheticPlayback() {
        progressJob?.cancel()
        progressJob = scope.launch {
            val total = if (_durationMs.value > 0) _durationMs.value else 18000L
            while (_isPlaying.value) {
                delay(33)
                val next = _currentPositionMs.value + 33
                if (next >= total) {
                    _currentPositionMs.value = total
                    _isPlaying.value = false
                    break
                } else {
                    _currentPositionMs.value = next
                }
            }
        }
    }

    private fun stopProgressTracking() {
        progressJob?.cancel()
        progressJob = null
    }

    // --- Voice Recording with Live Speech Recognition ---

    fun startRecording(): File? {
        stopPlayback()
        _liveRecognizedText.value = ""
        recordingStartTimeMs = System.currentTimeMillis()
        try {
            val outFile = File(context.cacheResolverDir(), "recorded_voice_${System.currentTimeMillis()}.m4a")
            currentRecordedFile = outFile

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(outFile.absolutePath)
                prepare()
                start()
            }
            mediaRecorder = recorder
            _isRecording.value = true

            // Amplitude tracker for live waveform visualizer
            recordingAmplitudeJob?.cancel()
            recordingAmplitudeJob = scope.launch {
                while (_isRecording.value) {
                    try {
                        val maxAmp = mediaRecorder?.maxAmplitude ?: 0
                        _recordingAmplitude.value = maxAmp
                    } catch (_: Exception) {}
                    delay(100)
                }
            }

            // Start On-device Speech Recognizer for real-time live captions
            startOnDeviceSpeechRecognition()

            return outFile
        } catch (e: Exception) {
            Log.e("AudioService", "Failed to start recording: ${e.message}", e)
            _isRecording.value = false
            return null
        }
    }

    private fun startOnDeviceSpeechRecognition() {
        try {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {}
                        override fun onBeginningOfSpeech() {}
                        override fun onRmsChanged(rmsdB: Float) {}
                        override fun onBufferReceived(buffer: ByteArray?) {}
                        override fun onEndOfSpeech() {}
                        override fun onError(error: Int) {
                            Log.w("AudioService", "SpeechRecognizer error: $error")
                        }
                        override fun onResults(results: Bundle?) {
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            if (!matches.isNullOrEmpty()) {
                                _liveRecognizedText.value = matches[0]
                            }
                        }
                        override fun onPartialResults(partialResults: Bundle?) {
                            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            if (!matches.isNullOrEmpty()) {
                                _liveRecognizedText.value = matches[0]
                            }
                        }
                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD")
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "bn-BD")
                    putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                }
                speechRecognizer?.startListening(intent)
            }
        } catch (e: Exception) {
            Log.w("AudioService", "SpeechRecognizer setup exception: ${e.message}")
        }
    }

    fun stopRecording(): File? {
        val elapsed = if (recordingStartTimeMs > 0) System.currentTimeMillis() - recordingStartTimeMs else 0L
        lastRecordedDurationMs = elapsed
        recordingAmplitudeJob?.cancel()
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null

        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            Log.e("AudioService", "Error stopping recorder: ${e.message}")
        }
        mediaRecorder = null
        _isRecording.value = false
        _recordingAmplitude.value = 0
        return currentRecordedFile
    }

    /**
     * Synthesizes a valid, playable WAV audio file with pleasant musical speech tones
     */
    fun createSyntheticAudioFile(durationSeconds: Int = 18): File {
        val file = File(context.cacheResolverDir(), "demo_voice_${durationSeconds}s.wav")
        if (file.exists() && file.length() > 44) return file

        val sampleRate = 22050
        val numSamples = durationSeconds * sampleRate
        val pcmData = ByteArray(numSamples * 2)

        var idx = 0
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val cadence = kotlin.math.sin(2.0 * Math.PI * 3.5 * t).coerceAtLeast(0.0)
            val pitch = 180.0 + 25.0 * kotlin.math.sin(2.0 * Math.PI * 0.8 * t)
            val wave = kotlin.math.sin(2.0 * Math.PI * pitch * t) * 0.7 +
                    kotlin.math.sin(2.0 * Math.PI * (pitch * 2) * t) * 0.3
            val sample = (wave * cadence * 14000.0).toInt().coerceIn(-32767, 32767).toShort()

            pcmData[idx++] = (sample.toInt() and 0xFF).toByte()
            pcmData[idx++] = ((sample.toInt() shr 8) and 0xFF).toByte()
        }

        FileOutputStream(file).use { out ->
            val totalDataLen = pcmData.size + 36
            val byteRate = sampleRate * 2

            val header = ByteArray(44)
            header[0] = 'R'.code.toByte(); header[1] = 'I'.code.toByte(); header[2] = 'F'.code.toByte(); header[3] = 'F'.code.toByte()
            header[4] = (totalDataLen and 0xff).toByte()
            header[5] = ((totalDataLen shr 8) and 0xff).toByte()
            header[6] = ((totalDataLen shr 16) and 0xff).toByte()
            header[7] = ((totalDataLen shr 24) and 0xff).toByte()
            header[8] = 'W'.code.toByte(); header[9] = 'A'.code.toByte(); header[10] = 'V'.code.toByte(); header[11] = 'E'.code.toByte()
            header[12] = 'f'.code.toByte(); header[13] = 'm'.code.toByte(); header[14] = 't'.code.toByte(); header[15] = ' '.code.toByte()
            header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0
            header[20] = 1; header[21] = 0
            header[22] = 1; header[23] = 0
            header[24] = (sampleRate and 0xff).toByte()
            header[25] = ((sampleRate shr 8) and 0xff).toByte()
            header[26] = ((sampleRate shr 16) and 0xff).toByte()
            header[27] = ((sampleRate shr 24) and 0xff).toByte()
            header[28] = (byteRate and 0xff).toByte()
            header[29] = ((byteRate shr 8) and 0xff).toByte()
            header[30] = ((byteRate shr 16) and 0xff).toByte()
            header[31] = ((byteRate shr 24) and 0xff).toByte()
            header[32] = 2; header[33] = 0
            header[34] = 16; header[35] = 0
            header[36] = 'd'.code.toByte(); header[37] = 'a'.code.toByte(); header[38] = 't'.code.toByte(); header[39] = 'a'.code.toByte()
            header[40] = (pcmData.size and 0xff).toByte()
            header[41] = ((pcmData.size shr 8) and 0xff).toByte()
            header[42] = ((pcmData.size shr 16) and 0xff).toByte()
            header[43] = ((pcmData.size shr 24) and 0xff).toByte()

            out.write(header)
            out.write(pcmData)
        }

        return file
    }

    private fun Context.cacheResolverDir(): File {
        val dir = File(cacheDir, "capgrok_audio")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun release() {
        stopPlayback()
        scope.cancel()
    }
}
