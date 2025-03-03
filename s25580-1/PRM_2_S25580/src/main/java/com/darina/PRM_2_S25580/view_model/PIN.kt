package com.darina.PRM_2_S25580.view_model



import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@HiltViewModel
class PinViewModel @Inject constructor(
    private val pinDao: PinDaobase
) : ViewModel() {

    suspend fun getPin(): Pin? = withContext(Dispatchers.IO) {
        pinDao.getPin()
    }

    fun setPin(pinHash: String) {
        viewModelScope.launch(Dispatchers.IO) {
            pinDao.insertPin(Pindaobase(pinHash = pinHash))
        }
    }