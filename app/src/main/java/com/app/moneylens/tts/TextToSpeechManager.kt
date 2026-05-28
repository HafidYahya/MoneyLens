package com.app.moneylens.tts

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.OnInitListener
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.*

class TextToSpeechManager(context: Context) : OnInitListener {
    private var tts: TextToSpeech? = null
    private var isReady = false
    private val queue: MutableList<String> = mutableListOf()
    var isSpeaking = false
        private set
    private var readyCallback: (() -> Unit)? = null
    private var onSpeechCompleteCallback: (() -> Unit)? = null  // Callback when TTS finishes
    
    // Audio Focus Management
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null

    init {
        tts = TextToSpeech(context, this)
        // Set up utterance progress listener to track speech
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                isSpeaking = true
                Log.d(TAG, "TTS onStart: $utteranceId")
            }

            override fun onDone(utteranceId: String?) {
                isSpeaking = false
                releaseAudioFocus()
                Log.d(TAG, "TTS onDone: $utteranceId")
                // Invoke callback when TTS completely done
                onSpeechCompleteCallback?.invoke()
            }

            override fun onError(utteranceId: String?) {
                isSpeaking = false
                releaseAudioFocus()
                Log.w(TAG, "TTS Error: $utteranceId")
            }
        })
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isReady = true
            // Set language to Indonesian using builder pattern
            val locale = Locale("id", "ID")
            val result = tts?.setLanguage(locale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "Language not supported, fallback to English")
                tts?.setLanguage(Locale.ENGLISH)
            }
            // Set speaking rate
            tts?.setSpeechRate(0.9f)
            Log.d(TAG, "TTS initialized and ready")
            // Call ready callback if set
            readyCallback?.invoke()
            // Process queued messages
            processQueue()
        } else {
            Log.e(TAG, "TTS initialization failed")
            isReady = false
        }
    }

    /**
     * Register callback ketika TTS siap
     */
    fun onTTSReady(callback: () -> Unit) {
        readyCallback = callback
        // Jika sudah ready, invoke langsung
        if (isReady) {
            callback.invoke()
        }
    }

    /**
     * Register callback ketika TTS selesai berbicara
     */
    fun onSpeechComplete(callback: () -> Unit) {
        onSpeechCompleteCallback = callback
    }

    fun speak(text: String) {
        if (isReady && tts != null) {
            try {
                Log.d(TAG, "TTS.speak() called with text: '$text'")
                
                // Request audio focus to compete with TalkBack/other audio
                requestAudioFocus()
                
                // Stop current speech
                if (tts!!.isSpeaking) {
                    Log.d(TAG, "TTS currently speaking, stopping first")
                    tts!!.stop()
                }
                
                val params = HashMap<String, String>()
                params[TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID] = "utterance_${System.currentTimeMillis()}"
                
                val result = tts!!.speak(text, TextToSpeech.QUEUE_FLUSH, params)
                Log.d(TAG, "TTS.speak() result: $result (0=success, 1=error)")
                
                if (result != 0) {
                    Log.w(TAG, "TTS.speak() returned error code $result")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in speak(): ${e.message}", e)
            }
        } else {
            // Queue the message if TTS is not ready
            Log.d(TAG, "TTS not ready (isReady=$isReady, tts=${tts != null}), queuing: '$text'")
            queue.add(text)
        }
    }
    
    private fun requestAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                
                audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(audioAttributes)
                    .setAcceptsDelayedFocusGain(false)  // Do NOT wait, need it now
                    .setWillPauseWhenDucked(false)   // Don't pause, force through
                    .build()
                
                val result = audioManager.requestAudioFocus(audioFocusRequest!!)
                val status = when (result) {
                    AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> "GRANTED"
                    AudioManager.AUDIOFOCUS_REQUEST_DELAYED -> "DELAYED"
                    AudioManager.AUDIOFOCUS_REQUEST_FAILED -> "FAILED"
                    else -> "UNKNOWN($result)"
                }
                Log.d(TAG, "Audio focus request result: $status")
            } else {
                // Pre-Android O
                val result = audioManager.requestAudioFocus(
                    null,
                    AudioManager.STREAM_ACCESSIBILITY,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
                )
                val status = if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) "GRANTED" else "FAILED"
                Log.d(TAG, "Audio focus request (legacy) result: $status")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting audio focus: ${e.message}", e)
        }
    }
    
    private fun releaseAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && audioFocusRequest != null) {
                audioManager.abandonAudioFocusRequest(audioFocusRequest!!)
            } else {
                audioManager.abandonAudioFocus(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing audio focus: ${e.message}", e)
        }
    }

    private fun processQueue() {
        if (queue.isNotEmpty()) {
            val text = queue.removeAt(0)
            speak(text)
        }
    }

    fun stop() {
        if (tts != null && tts!!.isSpeaking) {
            tts!!.stop()
        }
    }

    fun shutdown() {
        if (tts != null) {
            tts!!.stop()
            tts!!.shutdown()
        }
        releaseAudioFocus()
    }

    companion object {
        private const val TAG = "TextToSpeechManager"
    }
}

