package com.example.eyewearagentapp.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import java.io.File

class AudioRecorderManager(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var startTimeMs: Long = 0
    var isRecording = false
        private set

    fun startRecording(outputFile: File): Boolean {
        return try {
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }
            startTimeMs = SystemClock.elapsedRealtime()
            isRecording = true
            true
        } catch (e: Exception) {
            e.printStackTrace()
            releaseRecorder()
            false
        }
    }

    fun stopRecording(): Int {
        var durationSeconds = 0
        if (isRecording) {
            val endTimeMs = SystemClock.elapsedRealtime()
            durationSeconds = ((endTimeMs - startTimeMs) / 1000).toInt()
            try {
                mediaRecorder?.stop()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                releaseRecorder()
            }
        }
        return durationSeconds
    }

    private fun releaseRecorder() {
        try {
            mediaRecorder?.reset()
            mediaRecorder?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mediaRecorder = null
        isRecording = false
    }
}
