package com.example.service

import android.content.Context
import android.graphics.*
import android.media.*
import android.net.Uri
import android.os.Environment
import android.util.Log
import com.example.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.util.Locale

class VideoExporter(private val context: Context) {

    /**
     * Renders a real MP4 video file with animated background, synchronized dynamic captions,
     * and multiplexed synchronized audio from the project's source audio!
     */
    suspend fun exportVideo(
        project: ProjectState,
        fps: Int = 30,
        onProgress: (Float, String) -> Unit
    ): Result<File> = withContext(Dispatchers.Default) {
        val exportDir = File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir, "exports")
        if (!exportDir.exists()) exportDir.mkdirs()

        val cleanTitle = project.title.replace("[^a-zA-Z0-9_-]".toRegex(), "_").take(24)
        val outputFile = File(exportDir, "CapGrok_${cleanTitle}_${System.currentTimeMillis()}.mp4")

        try {
            onProgress(0.05f, "অডিও ও ভিডিও এনকোডার প্রস্তুত হচ্ছে...")

            // Adjust dimensions according to aspect ratio
            val (width, height) = when (project.style.aspectRatio) {
                VideoAspectRatio.NINE_SIXTEEN -> Pair(720, 1280)
                VideoAspectRatio.ONE_ONE -> Pair(720, 720)
                VideoAspectRatio.SIXTEEN_NINE -> Pair(1280, 720)
            }

            // Exact duration matching the voice
            val durationMs = project.audioDurationMs.coerceAtLeast(1000L).coerceAtMost(300000L)
            val totalFrames = ((durationMs / 1000f) * fps).toInt().coerceAtLeast(15)
            val bitRate = 4_000_000 // 4 Mbps high quality
            val videoMimeType = MediaFormat.MIMETYPE_VIDEO_AVC

            // 1. Prepare Audio Source (Ensure AAC track is ready for MP4 container)
            val audioSourceFile = resolveAudioSourceFile(project.audioUri)
            val preparedAacFile = audioSourceFile?.let { prepareAacAudioFile(it) }

            val audioExtractor = MediaExtractor()
            var audioTrackIndexInExtractor = -1
            var audioFormat: MediaFormat? = null

            if (preparedAacFile != null && preparedAacFile.exists()) {
                try {
                    audioExtractor.setDataSource(preparedAacFile.absolutePath)
                    for (i in 0 until audioExtractor.trackCount) {
                        val format = audioExtractor.getTrackFormat(i)
                        val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                        if (mime.startsWith("audio/")) {
                            audioTrackIndexInExtractor = i
                            audioFormat = format
                            break
                        }
                    }
                } catch (e: Exception) {
                    Log.w("VideoExporter", "AudioExtractor failed: ${e.message}")
                }
            }

            // 2. Prepare Video Encoder
            val videoFormat = MediaFormat.createVideoFormat(videoMimeType, width, height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
                setInteger(MediaFormat.KEY_FRAME_RATE, fps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1) // 1 second keyframes
            }

            val encoder = MediaCodec.createEncoderByType(videoMimeType)
            encoder.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val inputSurface = encoder.createInputSurface()
            encoder.start()

            val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var videoTrackIndex = -1
            var audioTrackIndexInMuxer = -1
            var muxerStarted = false

            val bufferInfo = MediaCodec.BufferInfo()
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
            }

            val frameDurationUs = (1_000_000L / fps)

            // 3. Render and Encode Video Frames
            for (frameIdx in 0 until totalFrames) {
                val currentTimestampMs = ((frameIdx.toFloat() / fps) * 1000f).toLong()
                val currentTimestampUs = frameIdx * frameDurationUs

                // Lock hardware canvas to draw frame
                val canvas = inputSurface.lockHardwareCanvas()
                if (canvas != null) {
                    try {
                        // Draw selected background (Gradient / Solid / Custom / Cyber)
                        drawBackground(canvas, width, height, project.style, frameIdx, totalFrames)

                        // Draw synchronized typography & active word highlights
                        drawCaptions(
                            canvas = canvas,
                            width = width,
                            height = height,
                            project = project,
                            currentTimeMs = currentTimestampMs,
                            textPaint = paint,
                            strokePaint = strokePaint
                        )
                    } finally {
                        inputSurface.unlockCanvasAndPost(canvas)
                    }
                }

                // Drain video encoder output
                while (true) {
                    val outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, 0)
                    if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        break
                    } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        if (muxerStarted) {
                            throw RuntimeException("Format changed after muxer started")
                        }
                        val newVideoFormat = encoder.outputFormat
                        videoTrackIndex = muxer.addTrack(newVideoFormat)

                        // Add audio track to muxer BEFORE starting muxer!
                        if (audioFormat != null) {
                            try {
                                audioTrackIndexInMuxer = muxer.addTrack(audioFormat)
                            } catch (e: Exception) {
                                Log.w("VideoExporter", "Failed to add audio track: ${e.message}")
                            }
                        }

                        muxer.start()
                        muxerStarted = true
                    } else if (outputBufferIndex >= 0) {
                        val encodedData = encoder.getOutputBuffer(outputBufferIndex)
                        if (encodedData != null) {
                            if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                                bufferInfo.size = 0
                            }
                            if (bufferInfo.size != 0 && muxerStarted) {
                                bufferInfo.presentationTimeUs = currentTimestampUs
                                muxer.writeSampleData(videoTrackIndex, encodedData, bufferInfo)
                            }
                        }
                        encoder.releaseOutputBuffer(outputBufferIndex, false)
                    }
                }

                // Progress update
                val progress = 0.05f + 0.75f * (frameIdx.toFloat() / totalFrames)
                if (frameIdx % 15 == 0 || frameIdx == totalFrames - 1) {
                    val percent = (progress * 100).toInt()
                    onProgress(progress, "ভিডিও ফ্রেম রেন্ডার হচ্ছে ($frameIdx/$totalFrames) - $percent%")
                }
            }

            // Signal End of Stream for Video
            encoder.signalEndOfInputStream()

            // Drain remaining video frames
            var eos = false
            var drainTries = 0
            while (!eos && drainTries < 60) {
                drainTries++
                val outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, 10_000)
                if (outputBufferIndex >= 0) {
                    val encodedData = encoder.getOutputBuffer(outputBufferIndex)
                    if (encodedData != null && bufferInfo.size != 0 && muxerStarted) {
                        muxer.writeSampleData(videoTrackIndex, encodedData, bufferInfo)
                    }
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        eos = true
                    }
                    encoder.releaseOutputBuffer(outputBufferIndex, false)
                } else if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    if (drainTries > 15) break
                }
            }

            try {
                encoder.stop()
                encoder.release()
            } catch (_: Exception) {}

            // 4. Multiplex Audio Track into MP4
            if (muxerStarted && audioTrackIndexInMuxer >= 0 && audioTrackIndexInExtractor >= 0) {
                onProgress(0.85f, "অরিজিনাল ভয়েস ও অডিও ট্র্যাক যুক্ত হচ্ছে...")
                try {
                    audioExtractor.selectTrack(audioTrackIndexInExtractor)
                    val maxAudioBufferSize = (audioFormat?.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE) ?: (128 * 1024)).coerceAtLeast(64 * 1024)
                    val audioBuffer = ByteBuffer.allocate(maxAudioBufferSize)
                    val audioBufferInfo = MediaCodec.BufferInfo()

                    val maxTimeUs = durationMs * 1000L

                    while (true) {
                        audioBufferInfo.offset = 0
                        audioBufferInfo.size = audioExtractor.readSampleData(audioBuffer, 0)
                        if (audioBufferInfo.size < 0) {
                            break // End of audio stream
                        }

                        val sampleTimeUs = audioExtractor.sampleTime
                        if (sampleTimeUs > maxTimeUs) {
                            break // Reached video end
                        }

                        audioBufferInfo.presentationTimeUs = sampleTimeUs
                        audioBufferInfo.flags = audioExtractor.sampleFlags

                        muxer.writeSampleData(audioTrackIndexInMuxer, audioBuffer, audioBufferInfo)
                        audioExtractor.advance()
                    }
                } catch (e: Exception) {
                    Log.e("VideoExporter", "Error writing audio samples: ${e.message}", e)
                }
            }

            try {
                audioExtractor.release()
            } catch (_: Exception) {}

            try {
                if (muxerStarted) {
                    muxer.stop()
                }
                muxer.release()
            } catch (e: Exception) {
                Log.e("VideoExporter", "Muxer release error: ${e.message}")
            }

            onProgress(1.0f, "সম্পূর্ণ ভিডিও তৈরি হয়েছে!")
            Result.success(outputFile)
        } catch (e: Exception) {
            Log.e("VideoExporter", "Failed to export video: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Resolves audio file from path or uri
     */
    private fun resolveAudioSourceFile(audioUriOrPath: String?): File? {
        if (audioUriOrPath.isNullOrBlank()) return null
        return if (audioUriOrPath.startsWith("/")) {
            val f = File(audioUriOrPath)
            if (f.exists()) f else null
        } else {
            try {
                val uri = Uri.parse(audioUriOrPath)
                val destFile = File(context.cacheDir, "source_export_audio.m4a")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    destFile.outputStream().use { output -> input.copyTo(output) }
                }
                if (destFile.exists()) destFile else null
            } catch (_: Exception) {
                null
            }
        }
    }

    /**
     * Converts WAV/PCM to AAC in M4A container if needed, so that MediaMuxer can write it.
     */
    private fun prepareAacAudioFile(sourceFile: File): File? {
        // If already M4A or AAC, return directly
        if (sourceFile.name.endsWith(".m4a", true) || sourceFile.name.endsWith(".aac", true)) {
            return sourceFile
        }

        // If WAV, encode to AAC using MediaCodec
        val aacFile = File(context.cacheDir, "transcoded_${System.currentTimeMillis()}.m4a")
        return try {
            encodeWavToAac(sourceFile, aacFile)
            if (aacFile.exists() && aacFile.length() > 0) aacFile else sourceFile
        } catch (e: Exception) {
            Log.w("VideoExporter", "WAV to AAC transcode fallback: ${e.message}")
            sourceFile
        }
    }

    private fun encodeWavToAac(wavFile: File, outputFile: File) {
        val sampleRate = 22050
        val channels = 1
        val bitRate = 64000

        val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channels).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
        }

        val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        encoder.start()

        val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var audioTrackIndex = -1
        var muxerStarted = false

        val bufferInfo = MediaCodec.BufferInfo()
        val inputStream = FileInputStream(wavFile)
        // Skip 44-byte WAV header
        inputStream.skip(44)

        val rawBuffer = ByteArray(4096)
        var eos = false
        var presentationTimeUs = 0L

        while (!eos) {
            val inputBufIndex = encoder.dequeueInputBuffer(10_000)
            if (inputBufIndex >= 0) {
                val inputBuffer = encoder.getInputBuffer(inputBufIndex) ?: continue
                inputBuffer.clear()
                val bytesRead = inputStream.read(rawBuffer)
                if (bytesRead <= 0) {
                    encoder.queueInputBuffer(inputBufIndex, 0, 0, presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                    eos = true
                } else {
                    inputBuffer.put(rawBuffer, 0, bytesRead)
                    encoder.queueInputBuffer(inputBufIndex, 0, bytesRead, presentationTimeUs, 0)
                    presentationTimeUs += (bytesRead / 2L * 1_000_000L / sampleRate)
                }
            }

            while (true) {
                val outputBufIndex = encoder.dequeueOutputBuffer(bufferInfo, 0)
                if (outputBufIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    break
                } else if (outputBufIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    val newFormat = encoder.outputFormat
                    audioTrackIndex = muxer.addTrack(newFormat)
                    muxer.start()
                    muxerStarted = true
                } else if (outputBufIndex >= 0) {
                    val encodedBuffer = encoder.getOutputBuffer(outputBufIndex)
                    if (encodedBuffer != null && bufferInfo.size > 0 && muxerStarted) {
                        muxer.writeSampleData(audioTrackIndex, encodedBuffer, bufferInfo)
                    }
                    encoder.releaseOutputBuffer(outputBufIndex, false)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        break
                    }
                }
            }
        }

        inputStream.close()
        try {
            encoder.stop()
            encoder.release()
        } catch (_: Exception) {}
        try {
            if (muxerStarted) muxer.stop()
            muxer.release()
        } catch (_: Exception) {}
    }

    private fun drawBackground(
        canvas: Canvas,
        width: Int,
        height: Int,
        style: VideoStyle,
        frameIdx: Int,
        totalFrames: Int
    ) {
        val t = (frameIdx.toFloat() / totalFrames.coerceAtLeast(1)) * 2f * Math.PI.toFloat()
        val shift = kotlin.math.sin(t) * 0.2f

        // Check if custom solid color is selected
        if (style.backgroundPreset == BackgroundPreset.SOLID_CUSTOM) {
            canvas.drawColor(style.customSolidBgColor.toInt())
            return
        }

        val (startColor, endColor) = when (style.backgroundPreset) {
            BackgroundPreset.GRADIENT_NEON -> Pair(Color.rgb(18, 14, 45), Color.rgb(10, 48, 75))
            BackgroundPreset.GRADIENT_SUNSET -> Pair(Color.rgb(45, 12, 32), Color.rgb(65, 24, 15))
            BackgroundPreset.DARK_STUDIO -> Pair(Color.rgb(22, 20, 36), Color.rgb(12, 10, 20))
            BackgroundPreset.CYBER_PULSE -> Pair(Color.rgb(38, 12, 60), Color.rgb(14, 8, 30))
            BackgroundPreset.SOLID_EMERALD -> Pair(Color.rgb(8, 40, 32), Color.rgb(4, 20, 16))
            BackgroundPreset.MINIMAL_BLACK, BackgroundPreset.SOLID_CUSTOM, BackgroundPreset.CUSTOM_MEDIA -> Pair(Color.rgb(8, 8, 12), Color.rgb(15, 15, 22))
        }

        val gradient = LinearGradient(
            0f, 0f,
            width.toFloat() * (1f + shift), height.toFloat(),
            startColor, endColor,
            Shader.TileMode.CLAMP
        )

        val bgPaint = Paint().apply { shader = gradient }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Subtle ambient sound wave pulses
        val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(25, 0, 229, 255)
            this.style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        val midY = height * 0.5f
        for (i in 0 until 5) {
            val radius = 100f + i * 80f + kotlin.math.sin(t * 3f + i) * 30f
            canvas.drawCircle(width * 0.5f, midY, radius, wavePaint)
        }
    }

    private fun drawCaptions(
        canvas: Canvas,
        width: Int,
        height: Int,
        project: ProjectState,
        currentTimeMs: Long,
        textPaint: Paint,
        strokePaint: Paint
    ) {
        val segment = project.captions.find { currentTimeMs in it.startMs..it.endMs }
            ?: return

        val style = project.style
        val centerY = height * (0.5f + style.verticalOffset * 0.4f)

        // Configure font & paint
        val typefaceStyle = when (style.fontFamily) {
            FontFamilyPreset.SANS_BOLD -> Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            FontFamilyPreset.BANGLA_CALLIGRAPHIC, FontFamilyPreset.BANGLA_MODERN -> Typeface.create(Typeface.SERIF, Typeface.BOLD)
            FontFamilyPreset.MODERN_SANS -> Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            FontFamilyPreset.ELEGANT_SERIF -> Typeface.create(Typeface.SERIF, Typeface.BOLD)
            FontFamilyPreset.MONOSPACE -> Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            FontFamilyPreset.HEAVY_IMPACT -> Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        val baseTextSize = (style.fontSizeSp * (width / 360f)).coerceIn(28f, 72f)
        textPaint.typeface = typefaceStyle
        textPaint.textSize = baseTextSize
        textPaint.textAlign = Paint.Align.CENTER

        strokePaint.typeface = typefaceStyle
        strokePaint.textSize = baseTextSize
        strokePaint.textAlign = Paint.Align.CENTER
        strokePaint.strokeWidth = style.strokeWidth * (width / 360f)
        strokePaint.color = style.strokeColor.toInt()

        val textToRender = if (style.allCaps) segment.text.uppercase(Locale.getDefault()) else segment.text
        val activeWord = segment.words.find { currentTimeMs in it.startMs..it.endMs }

        when (style.template) {
            CaptionStyleTemplate.HORMOZI_PUNCH -> {
                val words = segment.words.ifEmpty {
                    textToRender.split("\\s+".toRegex()).map { WordTiming(it, segment.startMs, segment.endMs) }
                }
                drawWordsInLine(canvas, words, activeWord, width * 0.5f, centerY, textPaint, strokePaint, style, bounceActive = true)
            }
            CaptionStyleTemplate.CAPCUT_BOUNCE -> {
                val words = segment.words.ifEmpty {
                    textToRender.split("\\s+".toRegex()).map { WordTiming(it, segment.startMs, segment.endMs) }
                }
                drawWordsInLine(canvas, words, activeWord, width * 0.5f, centerY, textPaint, strokePaint, style, bounceActive = true)
            }
            CaptionStyleTemplate.BOXED_HIGHLIGHT -> {
                val words = segment.words.ifEmpty {
                    textToRender.split("\\s+".toRegex()).map { WordTiming(it, segment.startMs, segment.endMs) }
                }
                drawWordsInLine(canvas, words, activeWord, width * 0.5f, centerY, textPaint, strokePaint, style, bounceActive = false, drawBoxForActive = true)
            }
            CaptionStyleTemplate.MINIMAL_PILL -> {
                val bounds = Rect()
                textPaint.getTextBounds(textToRender, 0, textToRender.length, bounds)
                val padX = 36f
                val padY = 20f
                val rect = RectF(
                    (width * 0.5f) - (bounds.width() * 0.5f) - padX,
                    centerY - (bounds.height() * 0.5f) - padY,
                    (width * 0.5f) + (bounds.width() * 0.5f) + padX,
                    centerY + (bounds.height() * 0.5f) + padY
                )
                val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(190, 15, 15, 25)
                    this.style = Paint.Style.FILL
                }
                canvas.drawRoundRect(rect, 24f, 24f, pillPaint)

                textPaint.color = style.textColor.toInt()
                canvas.drawText(textToRender, width * 0.5f, centerY + bounds.height() * 0.35f, textPaint)
            }
            CaptionStyleTemplate.KARAOKE_FLOW -> {
                val words = segment.words.ifEmpty {
                    textToRender.split("\\s+".toRegex()).map { WordTiming(it, segment.startMs, segment.endMs) }
                }
                drawWordsInLine(canvas, words, activeWord, width * 0.5f, centerY, textPaint, strokePaint, style, bounceActive = false)
            }
            CaptionStyleTemplate.NEON_CYBER -> {
                strokePaint.color = Color.rgb(0, 229, 255)
                strokePaint.strokeWidth = 10f
                canvas.drawText(textToRender, width * 0.5f, centerY, strokePaint)

                textPaint.color = Color.WHITE
                canvas.drawText(textToRender, width * 0.5f, centerY, textPaint)
            }
            CaptionStyleTemplate.CINEMATIC -> {
                strokePaint.strokeWidth = 4f
                strokePaint.color = Color.BLACK
                canvas.drawText(textToRender, width * 0.5f, centerY, strokePaint)

                textPaint.color = Color.rgb(245, 245, 235)
                canvas.drawText(textToRender, width * 0.5f, centerY, textPaint)
            }
        }
    }

    private fun drawWordsInLine(
        canvas: Canvas,
        words: List<WordTiming>,
        activeWord: WordTiming?,
        centerX: Float,
        centerY: Float,
        textPaint: Paint,
        strokePaint: Paint,
        style: VideoStyle,
        bounceActive: Boolean,
        drawBoxForActive: Boolean = false
    ) {
        val wordSpacing = 16f
        val widths = words.map { textPaint.measureText(it.word) }
        val totalWidth = widths.sum() + (words.size - 1) * wordSpacing

        var curX = centerX - (totalWidth * 0.5f)

        words.forEachIndexed { index, item ->
            val w = widths[index]
            val wordX = curX + (w * 0.5f)
            val isActive = activeWord != null && activeWord.word == item.word

            if (isActive && drawBoxForActive) {
                val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = style.highlightColor.toInt()
                    this.style = Paint.Style.FILL
                }
                val r = RectF(curX - 8f, centerY - 38f, curX + w + 8f, centerY + 14f)
                canvas.drawRoundRect(r, 10f, 10f, boxPaint)
            }

            val wordColor = if (isActive && style.showWordHighlight) {
                if (drawBoxForActive) Color.BLACK else style.highlightColor.toInt()
            } else {
                style.textColor.toInt()
            }

            if (isActive && bounceActive) {
                canvas.save()
                canvas.scale(1.15f, 1.15f, wordX, centerY)
            }

            if (!drawBoxForActive || !isActive) {
                strokePaint.textAlign = Paint.Align.CENTER
                canvas.drawText(item.word, wordX, centerY, strokePaint)
            }

            textPaint.textAlign = Paint.Align.CENTER
            textPaint.color = wordColor
            canvas.drawText(item.word, wordX, centerY, textPaint)

            if (isActive && bounceActive) {
                canvas.restore()
            }

            curX += w + wordSpacing
        }
    }

    fun generateFFmpegScript(project: ProjectState, audioFileName: String = "audio.mp3"): String {
        val assContent = generateAssSubtitles(project)
        val cmd = """
            # --- CapGrok FFmpeg Video Rendering Command ---
            # 1. Save subtitles as 'captions.ass'
            # 2. Run this command:
            ffmpeg -f lavfi -i color=c=0x161426:s=720x1280:r=30 -i "$audioFileName" \
              -vf "ass=captions.ass" \
              -c:v libx264 -preset fast -crf 22 -c:a aac -b:a 192k \
              -shortest output_capgrok.mp4
        """.trimIndent()
        return "$cmd\n\n# --- captions.ass Subtitle File ---\n$assContent"
    }

    fun generateAssSubtitles(project: ProjectState): String {
        val sb = StringBuilder()
        sb.appendLine("[Script Info]")
        sb.appendLine("Title: CapGrok Export")
        sb.appendLine("ScriptType: v4.00+")
        sb.appendLine("PlayResX: 720")
        sb.appendLine("PlayResY: 1280")
        sb.appendLine("")
        sb.appendLine("[V4+ Styles]")
        sb.appendLine("Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding")
        sb.appendLine("Style: Default,Arial,48,&H00FFFFFF,&H0000FFFF,&H00000000,&H80000000,-1,0,0,0,100,100,0,0,1,3,1,2,30,30,180,1")
        sb.appendLine("")
        sb.appendLine("[Events]")
        sb.appendLine("Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text")

        project.captions.forEach { seg ->
            val startStr = formatAssTime(seg.startMs)
            val endStr = formatAssTime(seg.endMs)
            val textWithKaraoke = if (seg.words.isNotEmpty()) {
                seg.words.joinToString(" ") { w ->
                    val durCs = ((w.endMs - w.startMs) / 10).coerceAtLeast(1)
                    "{\\k$durCs}${w.word}"
                }
            } else {
                seg.text
            }
            sb.appendLine("Dialogue: 0,$startStr,$endStr,Default,,0,0,0,,${textWithKaraoke}")
        }
        return sb.toString()
    }

    fun generateSrtSubtitles(project: ProjectState): String {
        val sb = StringBuilder()
        project.captions.forEachIndexed { index, seg ->
            sb.appendLine("${index + 1}")
            sb.appendLine("${formatSrtTime(seg.startMs)} --> ${formatSrtTime(seg.endMs)}")
            sb.appendLine(seg.text)
            sb.appendLine()
        }
        return sb.toString()
    }

    private fun formatAssTime(ms: Long): String {
        val hours = ms / 3_600_000
        val mins = (ms % 3_600_000) / 60_000
        val secs = (ms % 60_000) / 1000
        val cs = (ms % 1000) / 10
        return String.format(Locale.US, "%d:%02d:%02d.%02d", hours, mins, secs, cs)
    }

    private fun formatSrtTime(ms: Long): String {
        val hours = ms / 3_600_000
        val mins = (ms % 3_600_000) / 60_000
        val secs = (ms % 60_000) / 1000
        val millis = ms % 1000
        return String.format(Locale.US, "%02d:%02d:%02d,%03d", hours, mins, secs, millis)
    }
}
