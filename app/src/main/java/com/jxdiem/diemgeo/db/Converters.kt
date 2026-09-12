package com.jxdiem.diemgeo.db

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromPointList(points: List<GeoPointSample>): String = gson.toJson(points)

    @TypeConverter
    fun toPointList(json: String): List<GeoPointSample> {
        val type = object : TypeToken<List<GeoPointSample>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    @TypeConverter
    fun fromLabelList(labels: List<AiLabel>): String = gson.toJson(labels)

    @TypeConverter
    fun toLabelList(json: String): List<AiLabel> {
        val type = object : TypeToken<List<AiLabel>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    @TypeConverter
    fun fromMapSourceType(type: MapSourceType): String = type.name

    @TypeConverter
    fun toMapSourceType(value: String): MapSourceType = MapSourceType.valueOf(value)
}
