package com.example.eyewearagentapp

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.eyewearagentapp.ai.SpeechToTextManager
import com.example.eyewearagentapp.ai.TranslationManager
import com.example.eyewearagentapp.audio.AudioRecorderManager
import com.example.eyewearagentapp.audio.BluetoothScoManager
import com.example.eyewearagentapp.db.AppDatabase
import com.example.eyewearagentapp.db.RecordingItem
import com.example.eyewearagentapp.ui.RecordingAdapter
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var bluetoothScoManager: BluetoothScoManager
    private lateinit var audioRecorderManager: AudioRecorderManager
    private lateinit var translationManager: TranslationManager
    private var speechToTextManager: SpeechToTextManager? = null
    private lateinit var db: AppDatabase

    private lateinit var tvEyewearStatus: TextView
    private lateinit var btnCheckConnection: MaterialButton
    private lateinit var rbMeeting: RadioButton
    private lateinit var etTitle: TextInputEditText
    private lateinit var tvTimer: TextView
    private lateinit var btnRecord: MaterialButton
    private lateinit var llLivePreview: View
    private lateinit var tvLiveTranscription: TextView
    private lateinit var tvLiveTranslation: TextView
    private lateinit var rvRecordings: RecyclerView
    private lateinit var adapter: RecordingAdapter

    private var currentOutputFile: File? = null
    private var liveTranscriptionText = ""
    private var timerHandler = Handler(Looper.getMainLooper())
    private var timerSeconds = 0
    private val timerRunnable = object : Runnable {
        override fun run() {
            timerSeconds++
            val hours = timerSeconds / 3600
            val mins = (timerSeconds % 3600) / 60
            val secs = timerSeconds % 60
            tvTimer.text = String.format(Locale.JAPAN, "%02d:%02d:%02d", hours, mins, secs)
            timerHandler.postDelayed(this, 1000)
        }
    }

    private var mediaPlayer: MediaPlayer? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val recordGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        if (!recordGranted) {
            Toast.makeText(this, "マイク権限が必要です", Toast.LENGTH_SHORT).show()
        }
        updateBluetoothStatus()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_main)

            bluetoothScoManager = BluetoothScoManager(this)
            audioRecorderManager = AudioRecorderManager(this)
            translationManager = TranslationManager()
            db = AppDatabase.getDatabase(this)

            initViews()
            setupRecyclerView()
            observeRecordings()
            checkAndRequestPermissions()
        } catch (e: Throwable) {
            e.printStackTrace()
            try {
                val sw = StringWriter()
                val pw = PrintWriter(sw)
                e.printStackTrace(pw)
                val intent = Intent(this, CrashActivity::class.java).apply {
                    putExtra("EXTRA_ERROR_DETAILS", sw.toString())
                }
                startActivity(intent)
                finish()
            } catch (ex: Throwable) {
                Toast.makeText(this, "初期化エラー: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateBluetoothStatus()
    }

    private fun initViews() {
        tvEyewearStatus = findViewById(R.id.tvEyewearStatus)
        btnCheckConnection = findViewById(R.id.btnCheckConnection)
        rbMeeting = findViewById(R.id.rbMeeting)
        etTitle = findViewById(R.id.etTitle)
        tvTimer = findViewById(R.id.tvTimer)
        btnRecord = findViewById(R.id.btnRecord)
        llLivePreview = findViewById(R.id.llLivePreview)
        tvLiveTranscription = findViewById(R.id.tvLiveTranscription)
        tvLiveTranslation = findViewById(R.id.tvLiveTranslation)
        rvRecordings = findViewById(R.id.rvRecordings)

        btnCheckConnection.setOnClickListener { updateBluetoothStatus() }
        btnRecord.setOnClickListener { toggleRecording() }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
        } else {
            updateBluetoothStatus()
        }
    }

    private fun updateBluetoothStatus() {
        val isConnected = bluetoothScoManager.isScoDeviceConnected()
        if (isConnected) {
            tvEyewearStatus.text = "Huawei Eyewear: 接続準備完了 (Bluetooth)"
            tvEyewearStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
        } else {
            tvEyewearStatus.text = "Huawei Eyewear: 未検出 (本体マイク使用可能)"
            tvEyewearStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark))
        }
    }

    private fun setupRecyclerView() {
        adapter = RecordingAdapter(
            onItemClick = { item -> showDetailDialog(item) },
            onDeleteClick = { item -> deleteRecording(item) }
        )
        rvRecordings.layoutManager = LinearLayoutManager(this)
        rvRecordings.adapter = adapter
    }

    private fun observeRecordings() {
        lifecycleScope.launch {
            db.recordingDao().getAllRecordings().collectLatest { list ->
                adapter.submitList(list)
            }
        }
    }

    private fun toggleRecording() {
        if (audioRecorderManager.isRecording) {
            stopRecordingProcess()
        } else {
            if (!bluetoothScoManager.isScoDeviceConnected()) {
                AlertDialog.Builder(this)
                    .setTitle("Huawei Eyewear未検出")
                    .setMessage("スマートグラスが検出されませんでした。スマホ本体のマイクを使用して録音を開始しますか？")
                    .setPositiveButton("このまま開始") { _, _ -> startRecordingProcess() }
                    .setNegativeButton("キャンセル", null)
                    .show()
            } else {
                startRecordingProcess()
            }
        }
    }

    private fun startRecordingProcess() {
        bluetoothScoManager.startSco()

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.JAPAN).format(Date())
        val fileName = "REC_$timeStamp.m4a"
        val storageDir = getExternalFilesDir(null) ?: filesDir
        currentOutputFile = File(storageDir, fileName)

        val success = audioRecorderManager.startRecording(currentOutputFile!!)
        if (!success) {
            Toast.makeText(this, "録音の開始に失敗しました", Toast.LENGTH_SHORT).show()
            return
        }

        // タイマー開始
        timerSeconds = 0
        tvTimer.text = "00:00:00"
        timerHandler.postDelayed(timerRunnable, 1000)

        // UI状態変更
        btnRecord.text = "録音停止"
        btnRecord.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
        llLivePreview.visibility = View.VISIBLE
        liveTranscriptionText = ""
        tvLiveTranscription.text = "聞き取り中..."
        tvLiveTranslation.text = "Listening..."

        // リアルタイム文字起こし開始
        speechToTextManager = SpeechToTextManager(
            context = this,
            onPartialText = { text -> updateLiveText(text) },
            onFinalText = { text -> updateLiveText(text) },
            onErrorOccurred = { err -> }
        )
        speechToTextManager?.startListening()
    }

    private fun updateLiveText(text: String) {
        liveTranscriptionText = text
        tvLiveTranscription.text = text
        translationManager.translateJapaneseToEnglish(
            text = text,
            onSuccess = { translated -> tvLiveTranslation.text = translated },
            onError = { }
        )
    }

    private fun stopRecordingProcess() {
        timerHandler.removeCallbacks(timerRunnable)
        speechToTextManager?.stopListening()
        speechToTextManager?.destroy()
        speechToTextManager = null

        val durationSec = audioRecorderManager.stopRecording()
        bluetoothScoManager.stopSco()

        btnRecord.text = "録音開始"
        btnRecord.setBackgroundColor(ContextCompat.getColor(this, com.google.android.material.R.color.design_default_color_primary))
        llLivePreview.visibility = View.GONE

        // DBへ保存
        currentOutputFile?.let { file ->
            val mode = if (rbMeeting.isChecked) "MEETING" else "MEMO"
            val enteredTitle = etTitle.text?.toString()?.trim() ?: ""
            val defaultTitle = if (mode == "MEETING") "会議録音 $file" else "アイデアメモ $file"
            val finalTitle = if (enteredTitle.isNotBlank()) enteredTitle else defaultTitle

            val item = RecordingItem(
                title = finalTitle,
                filePath = file.absolutePath,
                createdAt = System.currentTimeMillis(),
                durationSeconds = durationSec,
                mode = mode,
                transcription = liveTranscriptionText.ifBlank { null },
                translation = tvLiveTranslation.text.toString().ifBlank { null }
            )

            lifecycleScope.launch(Dispatchers.IO) {
                db.recordingDao().insertRecording(item)
                withContext(Dispatchers.Main) {
                    etTitle.text?.clear()
                    Toast.makeText(this@MainActivity, "録音を保存しました", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun deleteRecording(item: RecordingItem) {
        AlertDialog.Builder(this)
            .setTitle("録音の削除")
            .setMessage("「${item.title}」を削除しますか？")
            .setPositiveButton("削除") { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    val file = File(item.filePath)
                    if (file.exists()) file.delete()
                    db.recordingDao().deleteRecording(item)
                }
            }
            .setNegativeButton("キャンセル", null)
            .show()
    }

    private fun showDetailDialog(item: RecordingItem) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_detail, null)
        val tvDetailTitle = dialogView.findViewById<TextView>(R.id.tvDetailTitle)
        val tvDetailInfo = dialogView.findViewById<TextView>(R.id.tvDetailInfo)
        val btnPlayAudio = dialogView.findViewById<MaterialButton>(R.id.btnPlayAudio)
        val tvAudioStatus = dialogView.findViewById<TextView>(R.id.tvAudioStatus)
        val btnTranscribe = dialogView.findViewById<MaterialButton>(R.id.btnTranscribe)
        val etTranscription = dialogView.findViewById<EditText>(R.id.etTranscription)
        val btnTranslate = dialogView.findViewById<MaterialButton>(R.id.btnTranslate)
        val etTranslation = dialogView.findViewById<EditText>(R.id.etTranslation)
        val btnCopyText = dialogView.findViewById<MaterialButton>(R.id.btnCopyText)
        val btnCloseDetail = dialogView.findViewById<MaterialButton>(R.id.btnCloseDetail)

        tvDetailTitle.text = item.title
        val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.JAPAN)
        tvDetailInfo.text = "日時: ${sdf.format(Date(item.createdAt))}  |  長さ: ${item.durationSeconds}秒"
        etTranscription.setText(item.transcription ?: "")
        etTranslation.setText(item.translation ?: "")

        val alertDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        // 音声再生処理
        btnPlayAudio.setOnClickListener {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
                mediaPlayer?.release()
                mediaPlayer = null
                btnPlayAudio.text = "再生"
                tvAudioStatus.text = "停止中"
            } else {
                try {
                    mediaPlayer = MediaPlayer().apply {
                        setDataSource(item.filePath)
                        prepare()
                        start()
                        setOnCompletionListener {
                            btnPlayAudio.text = "再生"
                            tvAudioStatus.text = "再生完了"
                        }
                    }
                    btnPlayAudio.text = "停止"
                    tvAudioStatus.text = "再生中..."
                } catch (e: Exception) {
                    Toast.makeText(this, "再生エラー", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 後からの文字起こし実行
        btnTranscribe.setOnClickListener {
            Toast.makeText(this, "文字起こしを開始します...", Toast.LENGTH_SHORT).show()
            speechToTextManager = SpeechToTextManager(
                context = this,
                onPartialText = { text -> etTranscription.setText(text) },
                onFinalText = { text ->
                    etTranscription.setText(text)
                    val updated = item.copy(transcription = text)
                    lifecycleScope.launch(Dispatchers.IO) {
                        db.recordingDao().updateRecording(updated)
                    }
                },
                onErrorOccurred = { err ->
                    Toast.makeText(this, "文字起こしエラー: $err", Toast.LENGTH_SHORT).show()
                }
            )
            speechToTextManager?.startListening()
        }

        // 後からの翻訳実行
        btnTranslate.setOnClickListener {
            val sourceText = etTranscription.text.toString()
            if (sourceText.isBlank()) {
                Toast.makeText(this, "翻訳するテキストがありません", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Toast.makeText(this, "翻訳中...", Toast.LENGTH_SHORT).show()
            translationManager.translateJapaneseToEnglish(
                text = sourceText,
                onSuccess = { translatedText ->
                    etTranslation.setText(translatedText)
                    val updated = item.copy(translation = translatedText)
                    lifecycleScope.launch(Dispatchers.IO) {
                        db.recordingDao().updateRecording(updated)
                    }
                },
                onError = { ex ->
                    Toast.makeText(this, "翻訳エラー: ${ex.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // テキストコピー
        btnCopyText.setOnClickListener {
            val clipText = "【文字起こし】\n${etTranscription.text}\n\n【翻訳】\n${etTranslation.text}"
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("EyewearRecording", clipText)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "テキストをコピーしました", Toast.LENGTH_SHORT).show()
        }

        btnCloseDetail.setOnClickListener {
            mediaPlayer?.release()
            mediaPlayer = null
            alertDialog.dismiss()
        }

        alertDialog.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        speechToTextManager?.destroy()
        mediaPlayer?.release()
    }
}
