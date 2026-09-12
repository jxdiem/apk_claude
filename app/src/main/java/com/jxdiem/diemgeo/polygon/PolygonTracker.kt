package com.jxdiem.diemgeo.polygon

import android.location.Location
import com.jxdiem.diemgeo.db.GeoPointSample
import com.jxdiem.diemgeo.location.LocationTrustState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class PolygonTrackingState(
    val isTracking: Boolean = false,
    val points: List<GeoPointSample> = emptyList(),
    val liveAreaSquareMeters: Double = 0.0
)

/**
 * Accumulates GeoPointSample vertices while the user walks the perimeter of
 * a plot. Only active while the map screen is in the foreground (no
 * background service): tracking pauses automatically if the screen leaves
 * the foreground, matching the simpler foreground-only model chosen for
 * this app.
 */
class PolygonTracker {

    private val _state = MutableStateFlow(PolygonTrackingState())
    val state: StateFlow<PolygonTrackingState> = _state

    fun start() {
        _state.value = PolygonTrackingState(isTracking = true, points = emptyList())
    }

    fun stop(): List<GeoPointSample> {
        val points = _state.value.points
        _state.value = PolygonTrackingState(isTracking = false, points = emptyList())
        return points
    }

    fun cancel() {
        _state.value = PolygonTrackingState(isTracking = false, points = emptyList())
    }

    fun onNewTrustState(trustState: LocationTrustState) {
        if (!_state.value.isTracking) return
        val sample = trustState.toGeoPointSample() ?: return

        val current = _state.value.points
        val last = current.lastOrNull()
        if (last != null) {
            val distance = distanceMeters(last, sample)
            if (distance < MIN_VERTEX_DISTANCE_M) return
        }

        val updated = current + sample
        _state.value = _state.value.copy(
            points = updated,
            liveAreaSquareMeters = polygonAreaSquareMeters(updated)
        )
    }

    private fun distanceMeters(a: GeoPointSample, b: GeoPointSample): Float {
        val result = FloatArray(1)
        Location.distanceBetween(a.latitude, a.longitude, b.latitude, b.longitude, result)
        return result[0]
    }

    companion object {
        private const val MIN_VERTEX_DISTANCE_M = 2.0f
    }
}
