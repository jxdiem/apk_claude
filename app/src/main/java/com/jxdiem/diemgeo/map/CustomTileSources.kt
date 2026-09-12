package com.jxdiem.diemgeo.map

import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.MapTileIndex
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.ln
import kotlin.math.sinh

private const val EARTH_HALF_CIRCUMFERENCE_M = 20037508.342789244

/** WMTS/XYZ source addressed with a `{z}/{x}/{y}` (or `{y}` flipped to TMS) URL template. */
class XyzTemplateTileSource(
    name: String,
    private val urlTemplate: String,
    minZoom: Int = 0,
    maxZoom: Int = 19
) : OnlineTileSourceBase(name, minZoom, maxZoom, 256, if (urlTemplate.endsWith(".jpg")) ".jpg" else ".png", arrayOf(urlTemplate)) {

    override fun getTileURLString(pMapTileIndex: Long): String {
        val z = MapTileIndex.getZoom(pMapTileIndex)
        val x = MapTileIndex.getX(pMapTileIndex)
        val y = MapTileIndex.getY(pMapTileIndex)
        return urlTemplate
            .replace("{z}", z.toString())
            .replace("{x}", x.toString())
            .replace("{y}", y.toString())
    }
}

/**
 * Generic WMS 1.3.0 GetMap source. The URL template must contain
 * `{bbox}`, `{width}` and `{height}` placeholders; the bounding box is
 * computed per-tile in EPSG:3857 using the standard slippy-map tile grid.
 */
class WmsTemplateTileSource(
    name: String,
    private val urlTemplate: String,
    minZoom: Int = 0,
    maxZoom: Int = 19,
    private val tileSizePx: Int = 256
) : OnlineTileSourceBase(name, minZoom, maxZoom, tileSizePx, ".png", arrayOf(urlTemplate)) {

    override fun getTileURLString(pMapTileIndex: Long): String {
        val z = MapTileIndex.getZoom(pMapTileIndex)
        val x = MapTileIndex.getX(pMapTileIndex)
        val y = MapTileIndex.getY(pMapTileIndex)
        val (minX, minY, maxX, maxY) = tileBoundsMeters(z, x, y)
        val bbox = "$minX,$minY,$maxX,$maxY"
        return urlTemplate
            .replace("{bbox}", bbox)
            .replace("{width}", tileSizePx.toString())
            .replace("{height}", tileSizePx.toString())
    }

    private data class Bounds(val minX: Double, val minY: Double, val maxX: Double, val maxY: Double)

    private fun tileBoundsMeters(z: Int, x: Int, y: Int): Bounds {
        val n = 1 shl z
        val lonMinDeg = x.toDouble() / n * 360.0 - 180.0
        val lonMaxDeg = (x + 1).toDouble() / n * 360.0 - 180.0
        val latMaxRad = atan(sinh(PI * (1 - 2.0 * y / n)))
        val latMinRad = atan(sinh(PI * (1 - 2.0 * (y + 1) / n)))

        fun lonToMerc(lonDeg: Double) = lonDeg * EARTH_HALF_CIRCUMFERENCE_M / 180.0
        fun latToMerc(latRad: Double): Double {
            val latDeg = Math.toDegrees(latRad)
            val y0 = ln(Math.tan(PI / 4 + Math.toRadians(latDeg) / 2))
            return y0 * EARTH_HALF_CIRCUMFERENCE_M / PI
        }

        return Bounds(
            minX = lonToMerc(lonMinDeg),
            minY = latToMerc(latMinRad),
            maxX = lonToMerc(lonMaxDeg),
            maxY = latToMerc(latMaxRad)
        )
    }
}
