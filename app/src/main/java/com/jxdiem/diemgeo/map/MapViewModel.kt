package com.jxdiem.diemgeo.map

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jxdiem.diemgeo.db.AppDatabase
import com.jxdiem.diemgeo.db.MapSourceEntity
import com.jxdiem.diemgeo.db.MapSourceType
import com.jxdiem.diemgeo.db.PolygonEntity
import com.jxdiem.diemgeo.location.LocationTrustEngine
import com.jxdiem.diemgeo.polygon.GeoJsonExporter
import com.jxdiem.diemgeo.polygon.PolygonTracker
import com.jxdiem.diemgeo.polygon.PolygonTrackingState
import com.jxdiem.diemgeo.polygon.polygonAreaSquareMeters
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class MapViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    val trustEngine = LocationTrustEngine(application)
    val tracker = PolygonTracker()

    val mapSources: StateFlow<List<MapSourceEntity>> = db.mapSourceDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trackingState: StateFlow<PolygonTrackingState> = tracker.state

    init {
        viewModelScope.launch {
            trustEngine.observe().collect { tracker.onNewTrustState(it) }
        }
    }

    fun startTracking() = tracker.start()

    fun cancelTracking() = tracker.cancel()

    fun stopAndSavePolygon(name: String, onSaved: (PolygonEntity) -> Unit) {
        val points = tracker.stop()
        if (points.size < 3) return
        viewModelScope.launch {
            val area = polygonAreaSquareMeters(points)
            val avgAccuracy = points.map { it.accuracyMeters }.average().toFloat()
            val minTrust = points.minOf { it.trustScore }

            var entity = PolygonEntity(
                name = name,
                createdAtMillis = System.currentTimeMillis(),
                points = points,
                areaSquareMeters = area,
                averageAccuracyMeters = avgAccuracy,
                minTrustScore = minTrust,
                geoJsonFilePath = null
            )
            val id = db.polygonDao().insert(entity)
            entity = entity.copy(id = id)

            val dir = File(getApplication<Application>().getExternalFilesDir(null), "polygons")
            val geoJsonFile = GeoJsonExporter.writeToFile(entity, dir)
            entity = entity.copy(geoJsonFilePath = geoJsonFile.absolutePath)
            db.polygonDao().update(entity)

            onSaved(entity)
        }
    }

    fun addMapSource(name: String, urlTemplate: String, type: MapSourceType) {
        viewModelScope.launch {
            db.mapSourceDao().insert(
                MapSourceEntity(name = name, urlTemplate = urlTemplate, type = type, isBuiltIn = false)
            )
        }
    }
}
