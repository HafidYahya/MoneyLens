package com.app.moneylens.tts

import android.content.Context
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

    init {
        tts = TextToSpeech(context, this)
        // Set up utterance progress listener to track speech
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                isSpeaking = true
            }

            override fun onDone(utteranceId: String?) {
                isSpeaking = false
            }

            override fun onError(utteranceId: String?) {
                isSpeaking = false
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
            // Process queued messages
            processQueue()
        } else {
            Log.e(TAG, "TTS initialization failed")
            isReady = false
        }
    }

    fun speak(text: String) {
        if (isReady && tts != null) {
            // Stop current speech
            if (tts!!.isSpeaking) {
                tts!!.stop()
            }
            
            val params = HashMap<String, String>()
            params[TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID] = "utterance_${System.currentTimeMillis()}"
            
            tts!!.speak(text, TextToSpeech.QUEUE_FLUSH, params)
        } else {
            // Queue the message if TTS is not ready
            queue.add(text)
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
    }

    companion object {
        private const val TAG = "TextToSpeechManager"
    }
}

