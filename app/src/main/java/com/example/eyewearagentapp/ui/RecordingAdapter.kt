package com.example.eyewearagentapp.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.eyewearagentapp.R
import com.example.eyewearagentapp.db.RecordingItem
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecordingAdapter(
    private val onItemClick: (RecordingItem) -> Unit,
    private val onDeleteClick: (RecordingItem) -> Unit
) : ListAdapter<RecordingItem, RecordingAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recording, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvModeBadge: TextView = itemView.findViewById(R.id.tvModeBadge)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        private val tvDuration: TextView = itemView.findViewById(R.id.tvDuration)
        private val tvCreatedAt: TextView = itemView.findViewById(R.id.tvCreatedAt)
        private val tvTranscriptionPreview: TextView = itemView.findViewById(R.id.tvTranscriptionPreview)
        private val btnDelete: MaterialButton = itemView.findViewById(R.id.btnDelete)
        private val btnDetail: MaterialButton = itemView.findViewById(R.id.btnDetail)

        fun bind(item: RecordingItem) {
            tvTitle.text = item.title.ifBlank { "無題の録音" }
            tvModeBadge.text = if (item.mode == "MEETING") "会議" else "メモ"
            tvDuration.text = formatDuration(item.durationSeconds)

            val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.JAPAN)
            tvCreatedAt.text = sdf.format(Date(item.createdAt))

            tvTranscriptionPreview.text = when {
                !item.transcription.isNullOrBlank() -> item.transcription
                else -> "（文字起こし未実行）"
            }

            btnDetail.setOnClickListener { onItemClick(item) }
            itemView.setOnClickListener { onItemClick(item) }
            btnDelete.setOnClickListener { onDeleteClick(item) }
        }

        private fun formatDuration(seconds: Int): String {
            val mins = seconds / 60
            val secs = seconds % 60
            return String.format(Locale.JAPAN, "%02d:%02d", mins, secs)
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<RecordingItem>() {
        override fun areItemsTheSame(oldItem: RecordingItem, newItem: RecordingItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: RecordingItem, newItem: RecordingItem): Boolean {
            return oldItem == newItem
        }
    }
}
