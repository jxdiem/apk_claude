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
 * A polygon vertex is only added when the surveyor explicitly taps "Fissa
 * punto" while standing on it — this is a point-by-point survey workflow,
 * not a continuous GPS trace, so accidental drift or a wobbly signal while
 * walking between vertices never distorts the shape. Only active while the
 * map screen is in the foreground (no background service).
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

    /** Fissa punto: records the current location as the next vertex. */
    fun addVertex(trustState: LocationTrustState): Boolean {
        if (!_state.value.isTracking) return false
        val sample = trustState.toGeoPointSample() ?: return false

        val current = _state.value.points
        val last = current.lastOrNull()
        if (last != null && distanceMeters(last, sample) < MIN_VERTEX_DISTANCE_M) {
            return false // guards against an accidental double-tap on the same spot
        }

        val updated = current + sample
        _state.value = _state.value.copy(
            points = updated,
            liveAreaSquareMeters = polygonAreaSquareMeters(updated)
        )
        return true
    }

    private fun distanceMeters(a: GeoPointSample, b: GeoPointSample): Float {
        val result = FloatArray(1)
        Location.distanceBetween(a.latitude, a.longitude, b.latitude, b.longitude, result)
        return result[0]
    }

    companion object {
        private const val MIN_VERTEX_DISTANCE_M = 0.5f
    }
}
