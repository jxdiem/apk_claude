package com.jxdiem.diemgeo.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jxdiem.diemgeo.databinding.ItemPolygonBinding
import com.jxdiem.diemgeo.db.PolygonEntity
import java.text.DateFormat
import java.util.Date

class PolygonAdapter(
    private val onShare: (PolygonEntity) -> Unit,
    private val onDelete: (PolygonEntity) -> Unit
) : ListAdapter<PolygonEntity, PolygonAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPolygonBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onShare, onDelete)
    }

    class ViewHolder(private val binding: ItemPolygonBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(polygon: PolygonEntity, onShare: (PolygonEntity) -> Unit, onDelete: (PolygonEntity) -> Unit) {
            binding.textName.text = polygon.name
            val date = DateFormat.getDateTimeInstance().format(Date(polygon.createdAtMillis))
            binding.textDetails.text = "$date  •  ${"%.1f".format(polygon.areaSquareMeters)} m²  •  " +
                "${polygon.points.size} punti  •  fiducia min. ${polygon.minTrustScore}%"
            binding.buttonShare.setOnClickListener { onShare(polygon) }
            binding.buttonDelete.setOnClickListener { onDelete(polygon) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<PolygonEntity>() {
            override fun areItemsTheSame(oldItem: PolygonEntity, newItem: PolygonEntity) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: PolygonEntity, newItem: PolygonEntity) = oldItem == newItem
        }
    }
}
