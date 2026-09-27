package com.example.eyewearagentapp

import android.app.Application
import android.content.Intent
import android.util.Log
import java.io.PrintWriter
import java.io.StringWriter

class EyewearApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // 未捕獲例外が発生した場合にCrashActivityを自動起動してエラーを表示
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("EyewearApp", "Uncaught Exception in thread ${thread.name}", throwable)

            try {
                val sw = StringWriter()
                val pw = PrintWriter(sw)
                throwable.printStackTrace(pw)
                val stackTraceString = sw.toString()

                val intent = Intent(this, CrashActivity::class.java).apply {
                    putExtra("EXTRA_ERROR_DETAILS", stackTraceString)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }
}
