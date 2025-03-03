package com.darina.PRM_2_S25580.rep

import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import com.darina.PRM_2_S25580.dao.DaoEntity
import com.darina.PRM_2_S25580.model.Entity

class EntityRep(private val daoEntity: DaoEntity) {

    @WorkerThread
    suspend fun insert(entity: Entity) {
        daoEntity.insert(entity)
    }

    @WorkerThread
    suspend fun update(entity: Entity) {
        daoEntity.update(entity)
    }

    fun getAllEntities(): LiveData<List<Entity>> {
        return daoEntity.getAllEntities()
    }

    fun fetchEntityById(entityId: Int): LiveData<Entity?> {
        return daoEntity.fetchEntityById(entityId)
    }

    suspend fun deleteEntityById(entityId: Int) {
        daoEntity.deleteById(entityId)
    }

    fun getLastEntity(): LiveData<Entity> {
        return daoEntity.fetchMostRecentEntity()
    }
}
