package com.jxdiem.diemgeo.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

@Entity(tableName = "photos")
@TypeConverters(Converters::class)
data class PhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val filePath: String,
    val takenAtMillis: Long,
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double,
    val accuracyMeters: Float,
    val satellitesInView: Int,
    val satellitesUsedInFix: Int,
    val trustScore: Int,
    val aiLabels: List<AiLabel>,
    val stegoSignature: String
)
