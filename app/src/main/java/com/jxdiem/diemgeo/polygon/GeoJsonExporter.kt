package com.jxdiem.diemgeo.polygon

import com.google.gson.GsonBuilder
import com.jxdiem.diemgeo.db.PolygonEntity
import java.io.File

/**
 * Exports a polygon as GeoJSON, embedding the full per-point sensor metadata
 * (spec point 12) inside each Feature's `properties`, not just the geometry.
 */
object GeoJsonExporter {

    fun toGeoJson(polygon: PolygonEntity): String {
        val gson = GsonBuilder().setPrettyPrinting().create()

        val coordinates = polygon.points.map { listOf(it.longitude, it.latitude, it.altitudeMeters) }
        val closedRing = if (coordinates.isNotEmpty() && coordinates.first() != coordinates.last()) {
            coordinates + listOf(coordinates.first())
        } else {
            coordinates
        }

        val feature = mapOf(
            "type" to "Feature",
            "geometry" to mapOf(
                "type" to "Polygon",
                "coordinates" to listOf(closedRing)
            ),
            "properties" to mapOf(
                "name" to polygon.name,
                "createdAtMillis" to polygon.createdAtMillis,
                "areaSquareMeters" to polygon.areaSquareMeters,
                "averageAccuracyMeters" to polygon.averageAccuracyMeters,
                "minTrustScore" to polygon.minTrustScore,
                "pointMetadata" to polygon.points.map {
                    mapOf(
                        "timestampMillis" to it.timestampMillis,
                        "accuracyMeters" to it.accuracyMeters,
                        "satellitesInView" to it.satellitesInView,
                        "satellitesUsedInFix" to it.satellitesUsedInFix,
                        "trustScore" to it.trustScore,
                        "isMockSuspected" to it.isMockSuspected
                    )
                }
            )
        )

        val featureCollection = mapOf(
            "type" to "FeatureCollection",
            "features" to listOf(feature)
        )

        return gson.toJson(featureCollection)
    }

    fun writeToFile(polygon: PolygonEntity, targetDir: File): File {
        if (!targetDir.exists()) targetDir.mkdirs()
        val safeName = polygon.name.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val file = File(targetDir, "${safeName}_${polygon.createdAtMillis}.geojson")
        file.writeText(toGeoJson(polygon))
        return file
    }
}
