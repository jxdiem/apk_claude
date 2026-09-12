package com.jxdiem.diemgeo.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

enum class MapSourceType { WMS, WMTS_XYZ }

@Entity(tableName = "map_sources")
@TypeConverters(Converters::class)
data class MapSourceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val urlTemplate: String,
    val type: MapSourceType,
    val isBuiltIn: Boolean = false
)
