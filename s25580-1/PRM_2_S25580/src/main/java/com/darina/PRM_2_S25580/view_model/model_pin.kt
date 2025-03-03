package com.darina.PRM_2_S25580.view_model


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class model_pin @Inject constructor(
    private val pinDao: PinDao
) : ViewModel() {

    suspend fun getPin(): Pin? = withContext(Dispatchers.IO) {
        pinDao.getPin()
    }

    fun setPin(pinHash: String) {
        viewModelScope.launch(Dispatchers.IO) {
            pinDao.insertPin(Pin(pinHash = pinHash))
        }
    }
}