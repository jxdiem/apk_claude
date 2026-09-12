package com.jxdiem.diemgeo.camera

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.jxdiem.diemgeo.ai.PhotoLabeler
import com.jxdiem.diemgeo.db.AppDatabase
import com.jxdiem.diemgeo.db.PhotoEntity
import com.jxdiem.diemgeo.location.LocationTrustEngine
import com.jxdiem.diemgeo.stego.Steganography
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

private data class StegoPayload(
    val lat: Double,
    val lon: Double,
    val alt: Double,
    val accuracyM: Float,
    val timestampMillis: Long,
    val trustScore: Int,
    val satellitesUsed: Int
)

class CameraViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    val trustEngine = LocationTrustEngine(application)

    private val _lastCapture = MutableSharedFlow<PhotoEntity>(extraBufferCapacity = 1)
    val lastCapture: SharedFlow<PhotoEntity> = _lastCapture

    init {
        viewModelScope.launch { trustEngine.observe().collect() }
    }

    fun capture(bitmap: Bitmap) {
        viewModelScope.launch {
            val trustState = trustEngine.state.value
            val location = trustState.location

            val labels = PhotoLabeler.label(bitmap)

            val payload = StegoPayload(
                lat = location?.latitude ?: 0.0,
                lon = location?.longitude ?: 0.0,
                alt = location?.takeIf { it.hasAltitude() }?.altitude ?: 0.0,
                accuracyM = location?.takeIf { it.hasAccuracy() }?.accuracy ?: -1f,
                timestampMillis = System.currentTimeMillis(),
                trustScore = trustState.trustScore,
                satellitesUsed = trustState.satellitesUsedInFix
            )
            val payloadJson = Gson().toJson(payload)

            val watermarked = withContext(Dispatchers.Default) {
                Steganography.embed(bitmap, payloadJson)
            }

            val photosDir = File(getApplication<Application>().getExternalFilesDir(null), "photos").apply { mkdirs() }
            val file = File(photosDir, "diem_geo_${payload.timestampMillis}.png")
            withContext(Dispatchers.IO) {
                FileOutputStream(file).use { out -> watermarked.compress(Bitmap.CompressFormat.PNG, 100, out) }
            }

            PhotoMetadataWriter.write(
                file = file,
                latitude = payload.lat,
                longitude = payload.lon,
                altitudeMeters = payload.alt,
                takenAtMillis = payload.timestampMillis,
                sensorSnapshot = trustEngine.lastSensorSnapshot()
            )

            val entity = PhotoEntity(
                filePath = file.absolutePath,
                takenAtMillis = payload.timestampMillis,
                latitude = payload.lat,
                longitude = payload.lon,
                altitudeMeters = payload.alt,
                accuracyMeters = payload.accuracyM,
                satellitesInView = trustState.satellitesInView,
                satellitesUsedInFix = trustState.satellitesUsedInFix,
                trustScore = trustState.trustScore,
                aiLabels = labels,
                stegoSignature = Steganography.sha256(payloadJson)
            )
            val id = db.photoDao().insert(entity)
            _lastCapture.emit(entity.copy(id = id))
        }
    }
}
