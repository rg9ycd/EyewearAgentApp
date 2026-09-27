package com.example.eyewearagentapp.ai

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions

class TranslationManager {

    /**
     * 日本語から英語へ（または指定言語間での）オンデバイス翻訳
     */
    fun translateJapaneseToEnglish(
        text: String,
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        if (text.isBlank()) {
            onSuccess("")
            return
        }

        try {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.JAPANESE)
                .setTargetLanguage(TranslateLanguage.ENGLISH)
                .build()

            val translator = Translation.getClient(options)

            val conditions = DownloadConditions.Builder()
                .requireWifi()
                .build()

            // モデルがダウンロードされていない場合は自動ダウンロード
            translator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener {
                    translator.translate(text)
                        .addOnSuccessListener { translatedText ->
                            onSuccess(translatedText)
                            translator.close()
                        }
                        .addOnFailureListener { exception ->
                            onError(exception)
                            translator.close()
                        }
                }
                .addOnFailureListener { exception ->
                    // Wi-Fi以外のネットワーク等でダウンロード失敗時は条件を緩めて再試行
                    translator.downloadModelIfNeeded()
                        .addOnSuccessListener {
                            translator.translate(text)
                                .addOnSuccessListener { translatedText ->
                                    onSuccess(translatedText)
                                    translator.close()
                                }
                                .addOnFailureListener { ex ->
                                    onError(ex)
                                    translator.close()
                                }
                        }
                        .addOnFailureListener { ex ->
                            onError(ex)
                            translator.close()
                        }
                }
        } catch (e: Exception) {
            e.printStackTrace()
            onError(e)
        }
    }
}
