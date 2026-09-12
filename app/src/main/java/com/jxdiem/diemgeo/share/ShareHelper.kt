package com.jxdiem.diemgeo.share

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

object ShareHelper {

    fun sharePhoto(context: Context, photoFile: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, null).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    fun shareGeoJson(context: Context, geoJsonFile: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", geoJsonFile)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/geo+json"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, null).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}
