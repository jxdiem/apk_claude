package com.jxdiem.diemgeo.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

@Entity(tableName = "polygons")
@TypeConverters(Converters::class)
data class PolygonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAtMillis: Long,
    val points: List<GeoPointSample>,
    val areaSquareMeters: Double,
    val averageAccuracyMeters: Float,
    val minTrustScore: Int,
    val geoJsonFilePath: String?
)
