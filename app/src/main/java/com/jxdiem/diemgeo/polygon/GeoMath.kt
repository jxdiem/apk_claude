package com.jxdiem.diemgeo.polygon

import com.jxdiem.diemgeo.db.GeoPointSample
import kotlin.math.PI
import kotlin.math.cos

private const val EARTH_RADIUS_M = 6371000.0

/**
 * Planar-projected polygon area (equirectangular approx, local meters),
 * accurate enough for the field-sized polygons this app traces on foot.
 * Uses the Shoelace formula after projecting lat/lon around the polygon
 * centroid latitude.
 */
fun polygonAreaSquareMeters(points: List<GeoPointSample>): Double {
    if (points.size < 3) return 0.0

    val refLat = points.map { it.latitude }.average()
    val metersPerDegLat = EARTH_RADIUS_M * PI / 180.0
    val metersPerDegLon = metersPerDegLat * cos(Math.toRadians(refLat))

    val projected = points.map { p ->
        Pair(p.longitude * metersPerDegLon, p.latitude * metersPerDegLat)
    }

    var sum = 0.0
    for (i in projected.indices) {
        val (x1, y1) = projected[i]
        val (x2, y2) = projected[(i + 1) % projected.size]
        sum += x1 * y2 - x2 * y1
    }
    return kotlin.math.abs(sum) / 2.0
}
