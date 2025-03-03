package com.darina.PRM_2_S25580.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.darina.PRM_2_S25580.model.Entity

@Dao
interface DaoEntity {
    @Insert
    suspend fun insert(entity: Entity)

    @Update
    suspend fun update(entity: Entity)

    @Delete
    suspend fun delete(entity: Entity)

    @Query("SELECT * FROM entities")
    fun getAllEntities(): LiveData<List<Entity>>

    @Query("SELECT * FROM entities WHERE id = :entityId")
    fun fetchEntityById(entityId: Int): LiveData<Entity?>

    @Query("DELETE FROM entities WHERE id = :entityId")
    suspend fun deleteById(entityId: Int)

    @Query("SELECT * FROM entities ORDER BY id DESC LIMIT 1")
    fun fetchMostRecentEntity(): LiveData<Entity>


    @Query("DELETE FROM entities")
    suspend fun clearAllEntities()
}
