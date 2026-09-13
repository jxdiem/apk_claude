package com.jxdiem.diemgeo.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [PolygonEntity::class, PhotoEntity::class, MapSourceEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun polygonDao(): PolygonDao
    abstract fun photoDao(): PhotoDao
    abstract fun mapSourceDao(): MapSourceDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "diem_geo.db"
                ).fallbackToDestructiveMigration().build().also { db ->
                    instance = db
                    seedDefaultSourcesIfNeeded(db)
                }
            }
        }

        private fun seedDefaultSourcesIfNeeded(db: AppDatabase) {
            CoroutineScope(Dispatchers.IO).launch {
                val dao = db.mapSourceDao()
                if (dao.countBuiltIn() > 0) return@launch
                val defaults = listOf(
                    MapSourceEntity(
                        name = "OpenStreetMap",
                        urlTemplate = "https://tile.openstreetmap.org/{z}/{x}/{y}.png",
                        type = MapSourceType.WMTS_XYZ,
                        isBuiltIn = true
                    ),
                    MapSourceEntity(
                        name = "OpenTopoMap",
                        urlTemplate = "https://tile.opentopomap.org/{z}/{x}/{y}.png",
                        type = MapSourceType.WMTS_XYZ,
                        isBuiltIn = true
                    ),
                    MapSourceEntity(
                        name = "Copernicus Sentinel-2 (EOX WMTS)",
                        urlTemplate = "https://tiles.maps.eox.at/wmts/1.0.0/s2cloudless-2023_3857/default/g/{z}/{y}/{x}.jpg",
                        type = MapSourceType.WMTS_XYZ,
                        isBuiltIn = true
                    ),
                    MapSourceEntity(
                        name = "Geoportale Nazionale - Ortofoto (WMS)",
                        urlTemplate = "https://wms.pcn.minambiente.it/ogc?map=/ms_ogc/WMS_v1.3/Ortofoto_WMS_2021.map&SERVICE=WMS&VERSION=1.3.0&REQUEST=GetMap&LAYERS=OI.ORTOIMMAGINI.2021&STYLES=&CRS=EPSG:3857&FORMAT=image/png&TRANSPARENT=TRUE&WIDTH={width}&HEIGHT={height}&BBOX={bbox}",
                        type = MapSourceType.WMS,
                        isBuiltIn = true
                    )
                )
                defaults.forEach { source -> dao.insert(source) }
            }
        }
    }
}
