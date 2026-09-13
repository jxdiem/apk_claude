package com.jxdiem.diemgeo.ui

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jxdiem.diemgeo.databinding.ItemPhotoBinding
import com.jxdiem.diemgeo.db.PhotoEntity
import java.text.DateFormat
import java.util.Date

class PhotoAdapter(
    private val onShare: (PhotoEntity) -> Unit,
    private val onDelete: (PhotoEntity) -> Unit,
    private val onVerify: (PhotoEntity, (String) -> Unit) -> Unit
) : ListAdapter<PhotoEntity, PhotoAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPhotoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onShare, onDelete, onVerify)
    }

    class ViewHolder(private val binding: ItemPhotoBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            photo: PhotoEntity,
            onShare: (PhotoEntity) -> Unit,
            onDelete: (PhotoEntity) -> Unit,
            onVerify: (PhotoEntity, (String) -> Unit) -> Unit
        ) {
            val date = DateFormat.getDateTimeInstance().format(Date(photo.takenAtMillis))
            val orientationText = photo.azimuthDeg?.let { az ->
                "  •  direzione %.0f°%s".format(az, photo.pitchDeg?.let { p -> ", inclinazione %.0f°".format(p) } ?: "")
            } ?: ""
            binding.textDetails.text = "$date  •  fiducia ${photo.trustScore}%  •  " +
                "%.1f, %.1f".format(photo.latitude, photo.longitude) + orientationText
            binding.textLabels.text = if (photo.aiLabels.isNotEmpty()) {
                photo.aiLabels.joinToString { it.text }
            } else {
                "—"
            }
            binding.textStego.text = ""

            binding.imageThumb.setImageBitmap(
                runCatching {
                    BitmapFactory.Options().apply { inSampleSize = 8 }.let { opts ->
                        BitmapFactory.decodeFile(photo.filePath, opts)
                    }
                }.getOrNull()
            )

            binding.buttonShare.setOnClickListener { onShare(photo) }
            binding.buttonDelete.setOnClickListener { onDelete(photo) }
            binding.buttonVerify.setOnClickListener {
                onVerify(photo) { result -> binding.textStego.text = result }
            }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<PhotoEntity>() {
            override fun areItemsTheSame(oldItem: PhotoEntity, newItem: PhotoEntity) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: PhotoEntity, newItem: PhotoEntity) = oldItem == newItem
        }
    }
}
