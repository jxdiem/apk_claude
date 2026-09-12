package com.jxdiem.diemgeo

import android.app.Application
import android.preference.PreferenceManager
import org.osmdroid.config.Configuration

class DiemGeoApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        Configuration.getInstance().userAgentValue = packageName
        Configuration.getInstance().osmdroidTileCache =
            getExternalFilesDir("osmdroid_cache") ?: filesDir
    }
}
