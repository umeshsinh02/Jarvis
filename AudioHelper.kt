package com.jarvis.assistant.util

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Base64
import com.jarvis.assistant.domain.model.JarvisVoiceEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

@Singleton
class AudioHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient
) : TextToSpeech.OnInitListener {

    private data class QueuedSpeech(
        val sentence: String,
        val apiKey: String,
        val engine: JarvisVoiceEngine,
        val generationId: Long
    )

    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val speechQueue = Channel<QueuedSpeech>(Channel.UNLIMITED)
    private val speechGeneration = AtomicLong(0L)

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var activeAudioTrack: AudioTrack? = null
    private var isTtsReady = false

    private var voiceSpeed = 1.0f
    private var voicePitch = 0.86f
    private var activeUtterances = 0

    private val _amplitudeFlow = MutableStateFlow(0f)
    val amplitudeFlow: StateFlow<Float> = _amplitudeFlow.asStateFlow()

    private val _isSpeakingFlow = MutableStateFlow(false)
    val isSpeakingFlow: StateFlow<Boolean> = _isSpeakingFlow.asStateFlow()

    // Battery efficiency: auto-cancel idle mic session after 20 seconds of silence
    private val idleStopRunnable = Runnable {
        stopListening()
    }

    private var onFinalResultCallback: ((String) -> Unit)? = null
    private var onPartialResultCallback: ((String) -> Unit)? = null
    private var onErrorCallback: ((String) -> Unit)? = null
    private var onAllSpeechDoneCallback: (() -> Unit)? = null

    init {
        mainHandler.post {
            textToSpeech = TextToSpeech(context, this)
        }
        startNeuralSpeechConsumer()
    }

    private fun startNeuralSpeechConsumer() {
        scope.launch {
            for (item in speechQueue) {
                if (item.generationId != speechGeneration.get()) continue

                var playedNeurally = false
                if (item.engine == JarvisVoiceEngine.CINEMATIC_NEURAL && item.apiKey.isNotBlank()) {
                    playedNeurally = synthesizeAndPlayNeuralVoice(
                        text = item.sentence,
                        apiKey = item.apiKey,
                        generationId = item.generationId
                    )
                    if (!playedNeurally && item.generationId == speechGeneration.get()) {
                        speakWithDeviceBritishTts(item.sentence, flushQueue = false)
                    }
                } else {
                    speakWithDeviceBritishTts(item.sentence, flushQueue = false)
                }

                if (playedNeurally && speechQueue.isEmpty && item.generationId == speechGeneration.get()) {
                    _amplitudeFlow.value = 0f
                    _isSpeakingFlow.value = false
                    mainHandler.post { onAllSpeechDoneCallback?.invoke() }
                }
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val tts = textToSpeech ?: return
            val ukResult = tts.setLanguage(Locale.UK)
            if (ukResult != TextToSpeech.LANG_MISSING_DATA &&
                ukResult != TextToSpeech.LANG_NOT_SUPPORTED
            ) {
                // Prefer deep British male voice (Google en-gb-x-rjs / en-gb-x-gbd) for JARVIS baritone
                val britishMale = tts.voices?.firstOrNull { voice ->
                    voice.locale == Locale.UK &&
                        (voice.name.contains("en-gb-x-rjs", ignoreCase = true) ||
                            voice.name.contains("en-gb-x-gbd", ignoreCase = true) ||
                            voice.name.contains("male", ignoreCase = true))
                } ?: tts.voices?.firstOrNull { it.locale == Locale.UK }

                if (britishMale != null) {
                    tts.voice = britishMale
                }
            }
            tts.setSpeechRate(voiceSpeed)
            tts.setPitch(voicePitch)
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeakingFlow.value = true
                }

                override fun onDone(utteranceId: String?) {
                    activeUtterances = (activeUtterances - 1).coerceAtLeast(0)
                    if (activeUtterances == 0) {
                        _isSpeakingFlow.value = false
                        mainHandler.post { onAllSpeechDoneCallback?.invoke() }
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    activeUtterances = (activeUtterances - 1).coerceAtLeast(0)
                    if (activeUtterances == 0) {
                        _isSpeakingFlow.value = false
                    }
                }
            })
            isTtsReady = true
        }
    }

    fun updateVoiceParams(speed: Float, pitch: Float) {
        voiceSpeed = speed
        voicePitch = pitch
        textToSpeech?.setSpeechRate(speed)
        textToSpeech?.setPitch(pitch)
    }

    fun setOnAllSpeechDoneListener(listener: () -> Unit) {
        onAllSpeechDoneCallback = listener
    }

    /**
     * Synthesizes a crystalline 2-tone Stark HUD acoustic chime directly in memory.
     */
    fun playHudChime(isListeningCue: Boolean) {
        scope.launch {
            runCatching {
                val sampleRate = 24_000
                val durationMs = 170
                val totalSamples = (sampleRate * durationMs) / 1000
                val pcm = ShortArray(totalSamples)
                val f1 = if (isListeningCue) 660.0 else 523.25
                val f2 = if (isListeningCue) 990.0 else 783.99

                for (i in 0 until totalSamples) {
                    val t = i.toDouble() / sampleRate
                    val freq = if (i < totalSamples / 2) f1 else f2
                    val env = 1.0 - (i.toDouble() / totalSamples)
                    pcm[i] = (sin(2.0 * PI * freq * t) * 4200.0 * env).toInt().toShort()
                }

                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(pcm.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(pcm, 0, pcm.size)
                track.play()
                Thread.sleep(200L)
                track.release()
            }
        }
    }

    /**
     * Queues a sentence for cinematic neural JARVIS playback (or offline British baritone fallback).
     */
    fun speakSentence(
        sentence: String,
        flushQueue: Boolean = false,
        apiKey: String = "",
        voiceEngine: JarvisVoiceEngine = JarvisVoiceEngine.BRITISH_BARITONE
    ) {
        val clean = sentence.trim()
        if (clean.isEmpty()) return

        val gen = if (flushQueue) {
            stopSpeaking()
            speechGeneration.incrementAndGet()
        } else {
            speechGeneration.get()
        }

        speechQueue.trySend(
            QueuedSpeech(
                sentence = clean,
                apiKey = apiKey,
                engine = voiceEngine,
                generationId = gen
            )
        )
    }

    private fun synthesizeAndPlayNeuralVoice(
        text: String,
        apiKey: String,
        generationId: Long
    ): Boolean {
        return runCatching {
            val payload = JSONObject().apply {
                put(
                    "contents",
                    JSONArray().put(
                        JSONObject().put(
                            "parts",
                            JSONArray().put(
                                JSONObject().put(
                                    "text",
                                    "Speak in a calm, refined British AI butler baritone with dry wit and crisp articulation: $text"
                                )
                            )
                        )
                    )
                )
                put(
                    "generationConfig",
                    JSONObject().apply {
                        put("responseModalities", JSONArray().put("AUDIO"))
                        put(
                            "speechConfig",
                            JSONObject().put(
                                "voiceConfig",
                                JSONObject().put(
                                    "prebuiltVoiceConfig",
                                    JSONObject().put("voiceName", "Charon")
                                )
                            )
                        )
                    }
                )
            }

            val url =
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-preview-tts:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val responseBody = okHttpClient.newCall(request).execute().use { res ->
                if (!res.isSuccessful) return false
                res.body?.string().orEmpty()
            }
            if (generationId != speechGeneration.get()) return false

            val json = JSONObject(responseBody)
            val base64Audio = json
                .getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getJSONObject("inlineData")
                .getString("data")

            val rawBytes = Base64.decode(base64Audio, Base64.DEFAULT)
            // Strip 44-byte RIFF WAV header if present; Gemini TTS outputs 24kHz mono 16-bit PCM
            val pcmOffset = if (rawBytes.size > 44 &&
                rawBytes[0] == 'R'.code.toByte() &&
                rawBytes[1] == 'I'.code.toByte()
            ) 44 else 0

            val sampleRate = (24_000 * voiceSpeed).toInt().coerceIn(16_000, 36_000)
            val pcmLen = rawBytes.size - pcmOffset
            if (pcmLen <= 0) return false

            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(maxOf(minBuf, 4096))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            activeAudioTrack = track
            _isSpeakingFlow.value = true
            track.play()

            val chunkSize = 2048
            var offset = pcmOffset
            while (offset < rawBytes.size && generationId == speechGeneration.get()) {
                val len = minOf(chunkSize, rawBytes.size - offset)
                track.write(rawBytes, offset, len)

                // Compute live RMS amplitude for ArcReactorOrb & VoiceWaveform
                var peak = 0
                var idx = offset
                while (idx + 1 < offset + len) {
                    val sample = ((rawBytes[idx + 1].toInt() shl 8) or (rawBytes[idx].toInt() and 0xFF)).toShort()
                    peak = maxOf(peak, abs(sample.toInt()))
                    idx += 2
                }
                _amplitudeFlow.value = (peak / 18_000f).coerceIn(0f, 1f)
                offset += len
            }

            runCatching {
                track.stop()
                track.release()
            }
            activeAudioTrack = null
            true
        }.getOrDefault(false)
    }

    private fun speakWithDeviceBritishTts(clean: String, flushQueue: Boolean) {
        if (!isTtsReady) return
        mainHandler.post {
            val mode = if (flushQueue) {
                activeUtterances = 0
                TextToSpeech.QUEUE_FLUSH
            } else {
                TextToSpeech.QUEUE_ADD
            }
            activeUtterances++
            textToSpeech?.speak(clean, mode, null, UUID.randomUUID().toString())
        }
    }

    fun stopSpeaking() {
        speechGeneration.incrementAndGet()
        runCatching {
            activeAudioTrack?.stop()
            activeAudioTrack?.release()
        }
        activeAudioTrack = null
        mainHandler.post {
            activeUtterances = 0
            textToSpeech?.stop()
            _isSpeakingFlow.value = false
            _amplitudeFlow.value = 0f
        }
    }

    fun startListening(
        onPartial: (String) -> Unit,
        onFinal: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!PermissionHelper.hasRecordAudio(context)) {
            onError("Microphone permission required, Sir.")
            return
        }

        onPartialResultCallback = onPartial
        onFinalResultCallback = onFinal
        onErrorCallback = onError

        mainHandler.post {
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                onError("Speech recognition service is unavailable on this device.")
                return@post
            }

            // Stop TTS so JARVIS doesn't listen to his own voice
            stopSpeaking()
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(createRecognitionListener())
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }

            scheduleIdleTimeout()
            speechRecognizer?.startListening(intent)
        }
    }

    fun stopListening() {
        mainHandler.post {
            mainHandler.removeCallbacks(idleStopRunnable)
            _amplitudeFlow.value = 0f
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
        }
    }

    private fun scheduleIdleTimeout() {
        mainHandler.removeCallbacks(idleStopRunnable)
        mainHandler.postDelayed(idleStopRunnable, 20_000L)
    }

    private fun createRecognitionListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {
            mainHandler.removeCallbacks(idleStopRunnable)
        }

        override fun onRmsChanged(rmsdB: Float) {
            // Normalize Android SpeechRecognizer RMS (-2..10 dB) into 0f..1f for HUD orb & waveform
            val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
            _amplitudeFlow.value = normalized
        }

        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {
            _amplitudeFlow.value = 0f
        }

        override fun onError(error: Int) {
            _amplitudeFlow.value = 0f
            val msg = when (error) {
                SpeechRecognizer.ERROR_NO_MATCH -> "Kuch samajh nahi aaya, Sir. Dobara boliye."
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Voice input timeout — standby mode active."
                SpeechRecognizer.ERROR_NETWORK,
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Speech recognition network unavailable."
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission denied."
                else -> "Audio capture interrupted (code $error)."
            }
            onErrorCallback?.invoke(msg)
        }

        override fun onResults(results: Bundle?) {
            _amplitudeFlow.value = 0f
            val text = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                ?.trim()
                .orEmpty()

            if (text.isNotEmpty()) {
                onFinalResultCallback?.invoke(text)
            } else {
                onErrorCallback?.invoke("Empty speech detected, Sir.")
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val partial = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                ?.trim()
                .orEmpty()
            if (partial.isNotEmpty()) {
                onPartialResultCallback?.invoke(partial)
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    fun release() {
        stopListening()
        stopSpeaking()
        speechQueue.close()
        scope.cancel()
        textToSpeech?.shutdown()
    }
}
