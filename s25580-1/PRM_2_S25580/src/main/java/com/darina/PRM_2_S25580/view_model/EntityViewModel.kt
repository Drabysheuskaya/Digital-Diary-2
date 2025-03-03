package com.darina.PRM_2_S25580.view_model

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.darina.PRM_2_S25580.model.Entity
import com.darina.PRM_2_S25580.rep.EntityRep
import kotlinx.coroutines.launch

class EntityViewModel(private val repository: EntityRep) : ViewModel() {
    val allEntities: LiveData<List<Entity>> = repository.getAllEntities()

    fun insert(entity: Entity, onInserted: () -> Unit) {
        viewModelScope.launch {
            repository.insert(entity)
            onInserted()
        }
    }


    fun getEntityById(id: Int): LiveData<Entity?> {
        return repository.fetchEntityById(id)
    }

    fun update(entity: Entity) = viewModelScope.launch {
        repository.update(entity)
    }

    fun delete(entityId: Int) = viewModelScope.launch {
        repository.deleteEntityById(entityId)
    }

    fun getLastEntity(): LiveData<Entity> {
        return repository.getLastEntity()
    }
}

