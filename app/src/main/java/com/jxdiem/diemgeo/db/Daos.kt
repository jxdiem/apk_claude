package com.jxdiem.diemgeo.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PolygonDao {
    @Insert
    suspend fun insert(polygon: PolygonEntity): Long

    @Update
    suspend fun update(polygon: PolygonEntity)

    @Delete
    suspend fun delete(polygon: PolygonEntity)

    @Query("SELECT * FROM polygons ORDER BY createdAtMillis DESC")
    fun observeAll(): Flow<List<PolygonEntity>>

    @Query("SELECT * FROM polygons WHERE id = :id")
    suspend fun getById(id: Long): PolygonEntity?
}

@Dao
interface PhotoDao {
    @Insert
    suspend fun insert(photo: PhotoEntity): Long

    @Delete
    suspend fun delete(photo: PhotoEntity)

    @Query("SELECT * FROM photos ORDER BY takenAtMillis DESC")
    fun observeAll(): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE id = :id")
    suspend fun getById(id: Long): PhotoEntity?
}

@Dao
interface MapSourceDao {
    @Insert
    suspend fun insert(source: MapSourceEntity): Long

    @Delete
    suspend fun delete(source: MapSourceEntity)

    @Query("SELECT * FROM map_sources ORDER BY isBuiltIn DESC, name ASC")
    fun observeAll(): Flow<List<MapSourceEntity>>

    @Query("SELECT COUNT(*) FROM map_sources WHERE isBuiltIn = 1")
    suspend fun countBuiltIn(): Int
}
