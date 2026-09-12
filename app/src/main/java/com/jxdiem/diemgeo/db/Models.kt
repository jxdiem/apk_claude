package com.jxdiem.diemgeo.db

/**
 * A single measured point, always captured together with the sensor context
 * that was available at the time (point 12 of the spec: metadata on every
 * coordinate, not just on the final geometry).
 */
data class GeoPointSample(
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double,
    val accuracyMeters: Float,
    val timestampMillis: Long,
    val satellitesInView: Int,
    val satellitesUsedInFix: Int,
    val trustScore: Int,
    val isMockSuspected: Boolean
)

data class AiLabel(
    val text: String,
    val confidence: Float
)

/** Full sensor snapshot embedded as metadata alongside a polygon or a photo. */
data class SensorSnapshot(
    val accelerometer: FloatArray?,
    val magnetometer: FloatArray?,
    val gyroscope: FloatArray?,
    val pressureHpa: Float?,
    val gnssHorizontalAccuracyM: Float?,
    val gnssSatellitesInView: Int,
    val gnssSatellitesUsed: Int,
    val locationIsMock: Boolean,
    val trustScore: Int
)
