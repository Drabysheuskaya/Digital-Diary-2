package com.darina.PRM_2_S25580.view_model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.darina.PRM_2_S25580.rep.EntityRep

class EntityViewModelStorage(private val repository: EntityRep) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EntityViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EntityViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
