package com.cash.guide.domain.speech

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class SpeechRecognitionState {
    IDLE,
    LISTENING,
    PROCESSING,
    ERROR
}

class SpeechRecognizerHelper(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var wasSystemMuted = false

    private val _state = MutableStateFlow(SpeechRecognitionState.IDLE)
    val state: StateFlow<SpeechRecognitionState> = _state.asStateFlow()

    private val _accumulatedText = MutableStateFlow("")
    val accumulatedText: StateFlow<String> = _accumulatedText.asStateFlow()

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var latestPartial: String = ""
    private var isUserRecording = false
    private var consecutiveSilenceCount = 0

    var onSpeechResult: ((String) -> Unit)? = null

    init {
        mainHandler.post {
            initRecognizer()
        }
    }

    var preferredScript: com.cash.guide.domain.ai.AiOutputScript = run {
        val sysLang = context.resources.configuration.locales.get(0)?.language?.lowercase() ?: "fr"
        if (sysLang.startsWith("ar")) com.cash.guide.domain.ai.AiOutputScript.ARABIC
        else com.cash.guide.domain.ai.AiOutputScript.FRENCH
    }

    fun updateScript(script: com.cash.guide.domain.ai.AiOutputScript) {
        if (this.preferredScript != script) {
            Log.d(TAG, "Switching speech recognition preferred script from ${this.preferredScript} to $script")
            this.preferredScript = script
            if (isUserRecording) {
                mainHandler.post {
                    try {
                        speechRecognizer?.cancel()
                        startListeningSafe()
                    } catch (e: Exception) {
                        Log.w(TAG, "Error switching recognition script on the fly", e)
                    }
                }
            }
        }
    }

    fun reset() {
        isUserRecording = false
        stopListening()
        _accumulatedText.value = ""
        _partialText.value = ""
        latestPartial = ""
        consecutiveSilenceCount = 0
        _errorMessage.value = null
        _state.value = SpeechRecognitionState.IDLE
    }

    fun startListening(script: com.cash.guide.domain.ai.AiOutputScript = preferredScript) {
        this.preferredScript = script
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _state.value = SpeechRecognitionState.ERROR
            _errorMessage.value = "التعرف على الصوت غير متاح في هذا الهاتف"
            return
        }

        isUserRecording = false
        stopListening()
        _accumulatedText.value = ""
        _partialText.value = ""
        latestPartial = ""
        consecutiveSilenceCount = 0
        _errorMessage.value = null

        isUserRecording = true
        _state.value = SpeechRecognitionState.LISTENING
        initRecognizer()
        startListeningSafe()
    }

    private fun initRecognizer() {
        if (speechRecognizer == null) {
            try {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(createListener())
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error initializing speech recognizer", e)
            }
        }
    }

    private fun muteBeeps() {
        if (wasSystemMuted) return
        try {
            audioManager?.adjustStreamVolume(AudioManager.STREAM_SYSTEM, AudioManager.ADJUST_MUTE, 0)
            audioManager?.adjustStreamVolume(AudioManager.STREAM_NOTIFICATION, AudioManager.ADJUST_MUTE, 0)
            wasSystemMuted = true
        } catch (e: Exception) {
            Log.w(TAG, "Cannot mute system stream beeps", e)
        }
    }

    private fun unmuteBeeps() {
        if (!wasSystemMuted) return
        try {
            audioManager?.adjustStreamVolume(AudioManager.STREAM_SYSTEM, AudioManager.ADJUST_UNMUTE, 0)
            audioManager?.adjustStreamVolume(AudioManager.STREAM_NOTIFICATION, AudioManager.ADJUST_UNMUTE, 0)
            wasSystemMuted = false
        } catch (e: Exception) {
            Log.w(TAG, "Cannot unmute system stream beeps", e)
        }
    }

    private fun startListeningSafe() {
        if (!isUserRecording) return
        muteBeeps()
        try {
            if (speechRecognizer == null) {
                initRecognizer()
            }
            speechRecognizer?.startListening(buildIntent())
        } catch (e: Exception) {
            Log.w(TAG, "Exception starting listening, re-initializing", e)
            destroy()
            initRecognizer()
            try {
                speechRecognizer?.startListening(buildIntent())
            } catch (e2: Exception) {
                Log.e(TAG, "Failed again to start listening", e2)
            }
        }
    }

    private fun buildIntent(): Intent {
        val (primaryLang, additionalLangs) = when (preferredScript) {
            com.cash.guide.domain.ai.AiOutputScript.FRENCH -> {
                "fr-FR" to arrayListOf("fr-FR", "fr-MA")
            }
            com.cash.guide.domain.ai.AiOutputScript.FRANCO -> {
                "ar-MA" to arrayListOf("ar-MA", "fr-FR", "fr-MA")
            }
            com.cash.guide.domain.ai.AiOutputScript.ARABIC -> {
                "ar-MA" to arrayListOf("ar-MA", "ar-DZ", "ar-SA")
            }
        }

        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, primaryLang)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, primaryLang)
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, true)
            putStringArrayListExtra("android.speech.extra.ADDITIONAL_LANGUAGES", additionalLangs)
            putExtra("android.speech.extra.ADDITIONAL_LANGUAGES", additionalLangs.toTypedArray())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }
    }

    private fun createListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            if (isUserRecording) {
                _state.value = SpeechRecognitionState.LISTENING
                _errorMessage.value = null
            }
        }

        override fun onBeginningOfSpeech() {
            if (isUserRecording) {
                _state.value = SpeechRecognitionState.LISTENING
                consecutiveSilenceCount = 0
            }
        }

        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}

        override fun onError(error: Int) {
            Log.w(TAG, "SpeechRecognizer error: $error, isUserRecording=$isUserRecording")
            if (!isUserRecording) return

            // Errors 6 (TIMEOUT), 7 (NO_MATCH), 8 (BUSY), 11 (SERVER_DISCONNECTED)
            // are temporary during continuous recording and should NOT cancel the user's session!
            if (error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT || 
                error == SpeechRecognizer.ERROR_NO_MATCH ||
                error == 11 || 
                error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
                
                if (error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT || error == SpeechRecognizer.ERROR_NO_MATCH) {
                    consecutiveSilenceCount++
                }

                mainHandler.postDelayed({
                    if (isUserRecording) {
                        startListeningSafe()
                    }
                }, 300)
                return
            }

            if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                _state.value = SpeechRecognitionState.ERROR
                _errorMessage.value = "يجب السماح بصلاحية الميكروفون"
                isUserRecording = false
                return
            }

            // For any other transient errors, keep recording alive
            mainHandler.postDelayed({
                if (isUserRecording) {
                    startListeningSafe()
                }
            }, 400)
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val spokenText = matches?.firstOrNull()?.trim() ?: ""
            Log.d(TAG, "Speech recognition onResults chunk: '$spokenText', isUserRecording=$isUserRecording")

            if (spokenText.isNotBlank()) {
                consecutiveSilenceCount = 0
                val current = _accumulatedText.value.trim()
                val updated = if (current.isBlank()) {
                    spokenText
                } else if (!current.contains(spokenText)) {
                    "$current $spokenText"
                } else {
                    current
                }
                _accumulatedText.value = updated
                _partialText.value = updated
                latestPartial = ""
            }

            if (isUserRecording) {
                mainHandler.postDelayed({
                    if (isUserRecording) {
                        startListeningSafe()
                    }
                }, 100)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            if (!isUserRecording) return
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull()?.trim() ?: ""
            if (text.isNotBlank()) {
                latestPartial = text
                val current = _accumulatedText.value.trim()
                val combined = if (current.isBlank()) {
                    text
                } else {
                    "$current $text"
                }
                _partialText.value = combined
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    fun stopAndDeliver() {
        isUserRecording = false
        mainHandler.removeCallbacksAndMessages(null)
        val transcript = getBestTranscript()
        stopListening()
        _state.value = SpeechRecognitionState.IDLE
        if (transcript.isNotBlank()) {
            onSpeechResult?.invoke(transcript)
        }
    }

    fun stopListening() {
        isUserRecording = false
        mainHandler.removeCallbacksAndMessages(null)
        unmuteBeeps()
        try {
            speechRecognizer?.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "Error canceling speech recognizer", e)
        } finally {
            if (_state.value != SpeechRecognitionState.PROCESSING) {
                _state.value = SpeechRecognitionState.IDLE
            }
        }
    }

    fun destroy() {
        isUserRecording = false
        mainHandler.removeCallbacksAndMessages(null)
        unmuteBeeps()
        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.e(TAG, "Error destroying speech recognizer", e)
        } finally {
            speechRecognizer = null
            _state.value = SpeechRecognitionState.IDLE
        }
    }

    fun getBestTranscript(): String {
        val acc = _accumulatedText.value.trim()
        val partial = latestPartial.trim()
        return when {
            acc.isNotBlank() && partial.isNotBlank() && !acc.endsWith(partial) -> "$acc $partial".trim()
            acc.isNotBlank() -> acc
            else -> partial
        }
    }

    companion object {
        private const val TAG = "SpeechRecognizerHelper"
    }
}
