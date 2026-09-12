package com.jxdiem.diemgeo.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jxdiem.diemgeo.db.AppDatabase
import com.jxdiem.diemgeo.db.PhotoEntity
import com.jxdiem.diemgeo.db.PolygonEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class SavedViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)

    val polygons: StateFlow<List<PolygonEntity>> = db.polygonDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val photos: StateFlow<List<PhotoEntity>> = db.photoDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deletePolygon(polygon: PolygonEntity) {
        viewModelScope.launch {
            db.polygonDao().delete(polygon)
            polygon.geoJsonFilePath?.let { File(it).delete() }
        }
    }

    fun deletePhoto(photo: PhotoEntity) {
        viewModelScope.launch {
            db.photoDao().delete(photo)
            File(photo.filePath).delete()
        }
    }
}
