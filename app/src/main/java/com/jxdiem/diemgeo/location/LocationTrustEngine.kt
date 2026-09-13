package com.jxdiem.diemgeo.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.hardware.Sensor
import com.jxdiem.diemgeo.security.RootStatus
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.GnssStatus
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import com.jxdiem.diemgeo.db.CameraOrientation
import com.jxdiem.diemgeo.db.GeoPointSample
import com.jxdiem.diemgeo.db.SensorSnapshot
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Trust level derived from a 0-100 score. This is a heuristic indicator, not
 * a legal proof of location: Android exposes no API that guarantees a fix is
 * genuine, so we combine every signal we *can* read (mock-provider flag,
 * GNSS constellation health, on-board sensor plausibility) into one score.
 */
enum class TrustLevel { HIGH, MEDIUM, LOW }

fun trustLevelFor(score: Int): TrustLevel = when {
    score >= 70 -> TrustLevel.HIGH
    score >= 40 -> TrustLevel.MEDIUM
    else -> TrustLevel.LOW
}

data class LocationTrustState(
    val location: Location? = null,
    val satellitesInView: Int = 0,
    val satellitesUsedInFix: Int = 0,
    val averageCn0: Float = 0f,
    val isMockSuspected: Boolean = false,
    val sensorsLookStatic: Boolean = false,
    val trustScore: Int = 0
) {
    val trustLevel: TrustLevel get() = trustLevelFor(trustScore)

    fun toGeoPointSample(): GeoPointSample? {
        val loc = location ?: return null
        return GeoPointSample(
            latitude = loc.latitude,
            longitude = loc.longitude,
            altitudeMeters = if (loc.hasAltitude()) loc.altitude else 0.0,
            accuracyMeters = if (loc.hasAccuracy()) loc.accuracy else -1f,
            timestampMillis = loc.time,
            satellitesInView = satellitesInView,
            satellitesUsedInFix = satellitesUsedInFix,
            trustScore = trustScore,
            isMockSuspected = isMockSuspected
        )
    }

    fun toSensorSnapshot(
        lastAccel: FloatArray?,
        lastMag: FloatArray?,
        lastGyro: FloatArray?,
        lastPressure: Float?,
        cameraOrientation: CameraOrientation? = null
    ) = SensorSnapshot(
        accelerometer = lastAccel,
        magnetometer = lastMag,
        gyroscope = lastGyro,
        pressureHpa = lastPressure,
        gnssHorizontalAccuracyM = location?.takeIf { it.hasAccuracy() }?.accuracy,
        gnssSatellitesInView = satellitesInView,
        gnssSatellitesUsed = satellitesUsedInFix,
        locationIsMock = isMockSuspected,
        trustScore = trustScore,
        cameraOrientation = cameraOrientation
    )
}

/**
 * Combines LocationManager (GPS provider + GnssStatus) with the motion
 * sensors into a single live trust score. Started/stopped explicitly because
 * it holds onto system callbacks.
 */
class LocationTrustEngine(private val context: Context) {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private var lastAccel: FloatArray? = null
    private var lastMag: FloatArray? = null
    private var lastGyro: FloatArray? = null
    private var lastPressure: Float? = null
    private val accelSamples = ArrayDeque<Float>(MOTION_WINDOW)

    private val _state = MutableStateFlow(LocationTrustState())
    val state: StateFlow<LocationTrustState> = _state

    fun lastSensorSnapshot(): SensorSnapshot =
        _state.value.toSensorSnapshot(lastAccel, lastMag, lastGyro, lastPressure, currentCameraOrientation())

    /**
     * Direction the camera is pointing (azimuth) and its tilt (pitch/roll),
     * computed from the last accelerometer + magnetometer readings — the
     * same technique a compass app uses. Returns null until both sensors
     * have reported at least once.
     */
    fun currentCameraOrientation(): CameraOrientation? {
        val accel = lastAccel ?: return null
        val mag = lastMag ?: return null
        val rotationMatrix = FloatArray(9)
        if (!SensorManager.getRotationMatrix(rotationMatrix, null, accel, mag)) return null

        val orientationValues = FloatArray(3)
        SensorManager.getOrientation(rotationMatrix, orientationValues)

        var azimuthDeg = Math.toDegrees(orientationValues[0].toDouble()).toFloat()
        if (azimuthDeg < 0) azimuthDeg += 360f
        val pitchDeg = Math.toDegrees(orientationValues[1].toDouble()).toFloat()
        val rollDeg = Math.toDegrees(orientationValues[2].toDouble()).toFloat()

        return CameraOrientation(azimuthDeg = azimuthDeg, pitchDeg = pitchDeg, rollDeg = rollDeg)
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun observe() = callbackFlow<LocationTrustState> {
        if (!hasLocationPermission()) {
            _state.value = LocationTrustState()
            trySend(_state.value)
            awaitClose { }
            return@callbackFlow
        }

        var satellitesInView = 0
        var satellitesUsed = 0
        var avgCn0 = 0f

        fun emitState(satView: Int, satUsed: Int, cn0: Float, location: Location?) {
            val isMock = location?.let {
                @Suppress("DEPRECATION")
                if (android.os.Build.VERSION.SDK_INT >= 31) it.isMock else it.isFromMockProvider
            } ?: false

            val sensorsStatic = isAccelerometerSuspiciouslyStatic()

            val score = computeTrustScore(
                accuracyMeters = location?.takeIf { it.hasAccuracy() }?.accuracy,
                satellitesUsed = satUsed,
                averageCn0 = cn0,
                isMockSuspected = isMock,
                sensorsLookStatic = sensorsStatic
            )

            val newState = LocationTrustState(
                location = location,
                satellitesInView = satView,
                satellitesUsedInFix = satUsed,
                averageCn0 = cn0,
                isMockSuspected = isMock,
                sensorsLookStatic = sensorsStatic,
                trustScore = score
            )
            _state.value = newState
            trySend(newState)
        }

        val gnssCallback = object : GnssStatus.Callback() {
            override fun onSatelliteStatusChanged(status: GnssStatus) {
                satellitesInView = status.satelliteCount
                var used = 0
                var cn0Sum = 0f
                for (i in 0 until status.satelliteCount) {
                    if (status.usedInFix(i)) {
                        used++
                        cn0Sum += status.getCn0DbHz(i)
                    }
                }
                satellitesUsed = used
                avgCn0 = if (used > 0) cn0Sum / used else 0f
                emitState(satellitesInView, satellitesUsed, avgCn0, _state.value.location)
            }
        }

        val locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                emitState(satellitesInView, satellitesUsed, avgCn0, location)
            }

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
            override fun onProviderEnabled(provider: String) = Unit
            override fun onProviderDisabled(provider: String) = Unit
        }

        val sensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> {
                        lastAccel = event.values.clone()
                        val magnitude = kotlin.math.sqrt(
                            event.values[0] * event.values[0] +
                                event.values[1] * event.values[1] +
                                event.values[2] * event.values[2]
                        )
                        if (accelSamples.size >= MOTION_WINDOW) accelSamples.removeFirst()
                        accelSamples.addLast(magnitude)
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> lastMag = event.values.clone()
                    Sensor.TYPE_GYROSCOPE -> lastGyro = event.values.clone()
                    Sensor.TYPE_PRESSURE -> lastPressure = event.values.getOrNull(0)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        locationManager.registerGnssStatusCallback(gnssCallback, null)
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            LOCATION_UPDATE_INTERVAL_MS,
            0f,
            locationListener
        )
        registerSensor(sensorListener, Sensor.TYPE_ACCELEROMETER)
        registerSensor(sensorListener, Sensor.TYPE_MAGNETIC_FIELD)
        registerSensor(sensorListener, Sensor.TYPE_GYROSCOPE)
        registerSensor(sensorListener, Sensor.TYPE_PRESSURE)

        awaitClose {
            locationManager.unregisterGnssStatusCallback(gnssCallback)
            locationManager.removeUpdates(locationListener)
            sensorManager.unregisterListener(sensorListener)
        }
    }

    private fun registerSensor(listener: SensorEventListener, type: Int) {
        sensorManager.getDefaultSensor(type)?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    /**
     * A device held by a walking person always shows some accelerometer
     * variance from gravity + micro-movement. A perfectly flat, unchanging
     * signal over a long window is consistent with a location being fed by
     * software on an emulator/rooted device rather than held in hand — it is
     * a weak signal on its own, so it only contributes a small weight to the
     * overall score.
     */
    private fun isAccelerometerSuspiciouslyStatic(): Boolean {
        if (accelSamples.size < MOTION_WINDOW) return false
        val avg = accelSamples.average()
        val variance = accelSamples.sumOf { (it - avg) * (it - avg) } / accelSamples.size
        return variance < STATIC_VARIANCE_THRESHOLD
    }

    private fun computeTrustScore(
        accuracyMeters: Float?,
        satellitesUsed: Int,
        averageCn0: Float,
        isMockSuspected: Boolean,
        sensorsLookStatic: Boolean
    ): Int {
        if (isMockSuspected) return 0

        var score = 100

        score -= when {
            accuracyMeters == null -> 40
            accuracyMeters <= 5f -> 0
            accuracyMeters <= 15f -> 10
            accuracyMeters <= 30f -> 25
            else -> 45
        }

        score -= when {
            satellitesUsed >= 7 -> 0
            satellitesUsed >= 4 -> 10
            satellitesUsed >= 1 -> 25
            else -> 40
        }

        if (satellitesUsed > 0 && averageCn0 < MIN_HEALTHY_CN0) score -= 10

        if (sensorsLookStatic) score -= 15

        if (RootStatus.isRooted) score -= 30

        return score.coerceIn(0, 100)
    }

    companion object {
        private const val LOCATION_UPDATE_INTERVAL_MS = 1000L
        private const val MOTION_WINDOW = 20
        private const val STATIC_VARIANCE_THRESHOLD = 0.0005f
        private const val MIN_HEALTHY_CN0 = 18f
    }
}
