package com.example.eyewearagentapp.ai

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

class SpeechToTextManager(
    private val context: Context,
    private val onPartialText: (String) -> Unit,
    private val onFinalText: (String) -> Unit,
    private val onErrorOccurred: (String) -> Unit
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    init {
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
                            val message = getErrorMessage(error)
                            onErrorOccurred(message)
                            isListening = false
                        }

                        override fun onResults(results: Bundle?) {
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            if (!matches.isNullOrEmpty()) {
                                onFinalText(matches[0])
                            }
                            isListening = false
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            if (!matches.isNullOrEmpty()) {
                                onPartialText(matches[0])
                            }
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            speechRecognizer = null
        }
    }

    fun startListening(languageLocale: Locale = Locale.JAPANESE) {
        try {
            if (speechRecognizer == null) {
                onErrorOccurred("音声認識がサポートされていないか利用できません。")
                return
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageLocale.language)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }

            speechRecognizer?.startListening(intent)
            isListening = true
        } catch (e: Exception) {
            e.printStackTrace()
            onErrorOccurred("音声認識の開始に失敗しました: ${e.localizedMessage}")
            isListening = false
        }
    }

    fun stopListening() {
        if (isListening) {
            speechRecognizer?.stopListening()
            isListening = false
        }
    }

    fun destroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    private fun getErrorMessage(errorCode: Int): String {
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "音声入力エラー"
            SpeechRecognizer.ERROR_CLIENT -> "クライアントエラー"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "権限が不足しています"
            SpeechRecognizer.ERROR_NETWORK -> "ネットワークエラー"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "ネットワークタイムアウト"
            SpeechRecognizer.ERROR_NO_MATCH -> "音声が認識されませんでした"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "音声認識サービスがビジーです"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "音声入力がタイムアウトしました"
            else -> "未知のエラー ($errorCode)"
        }
    }
}
