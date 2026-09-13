package com.jxdiem.diemgeo.camera

import androidx.exifinterface.media.ExifInterface
import com.google.gson.Gson
import com.jxdiem.diemgeo.db.SensorSnapshot
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Writes standard EXIF GPS/time tags plus the full sensor snapshot (spec
 * point 12) as a JSON blob in the UserComment tag, so any generic photo
 * viewer shows correct GPS info while the app itself can recover the raw
 * sensor readings later.
 */
object PhotoMetadataWriter {

    fun write(
        file: File,
        latitude: Double,
        longitude: Double,
        altitudeMeters: Double,
        takenAtMillis: Long,
        sensorSnapshot: SensorSnapshot
    ) {
        val exif = ExifInterface(file.absolutePath)

        exif.setLatLong(latitude, longitude)
        exif.setAltitude(altitudeMeters)

        val exifDateFormat = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US)
        val dateString = exifDateFormat.format(Date(takenAtMillis))
        exif.setAttribute(ExifInterface.TAG_DATETIME, dateString)
        exif.setAttribute(ExifInterface.TAG_GPS_DATESTAMP, SimpleDateFormat("yyyy:MM:dd", Locale.US).also {
            it.timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date(takenAtMillis)))

        // Standard EXIF fields for "which way was the camera pointing" —
        // magnetic bearing, since it comes from the on-board magnetometer
        // rather than a true-north-corrected source.
        sensorSnapshot.cameraOrientation?.let { orientation ->
            exif.setAttribute(ExifInterface.TAG_GPS_IMG_DIRECTION, orientation.azimuthDeg.toString())
            exif.setAttribute(ExifInterface.TAG_GPS_IMG_DIRECTION_REF, "M")
        }

        exif.setAttribute(ExifInterface.TAG_USER_COMMENT, "diem_geo:" + Gson().toJson(sensorSnapshot))

        exif.saveAttributes()
    }
}
