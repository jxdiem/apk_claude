package com.jxdiem.diemgeo.map

import com.jxdiem.diemgeo.db.MapSourceEntity
import com.jxdiem.diemgeo.db.MapSourceType
import org.osmdroid.tileprovider.tilesource.ITileSource

object TileSourceFactory {
    fun from(source: MapSourceEntity): ITileSource = when (source.type) {
        MapSourceType.WMTS_XYZ -> XyzTemplateTileSource(source.name, source.urlTemplate)
        MapSourceType.WMS -> WmsTemplateTileSource(source.name, source.urlTemplate)
    }
}
