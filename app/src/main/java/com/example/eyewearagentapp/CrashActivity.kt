package com.example.eyewearagentapp

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class CrashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val errorDetails = intent.getStringExtra("EXTRA_ERROR_DETAILS") ?: "未知のエラーが発生しました"

        val scrollView = ScrollView(this)
        val linearLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        val tvTitle = TextView(this).apply {
            text = "アプリ起動エラーが発生しました"
            textSize = 20f
            setTextColor(Color.RED)
            setTypeface(null, Typeface.BOLD)
        }

        val tvError = TextView(this).apply {
            text = errorDetails
            textSize = 12f
            setTextColor(Color.BLACK)
            setPadding(0, 16, 0, 16)
        }

        val btnCopy = MaterialButton(this).apply {
            text = "エラー詳細をコピー"
            setOnClickListener {
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("CrashLog", errorDetails)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "エラー内容をコピーしました", Toast.LENGTH_SHORT).show()
            }
        }

        linearLayout.addView(tvTitle)
        linearLayout.addView(tvError)
        linearLayout.addView(btnCopy)
        scrollView.addView(linearLayout)

        setContentView(scrollView)
    }
}
