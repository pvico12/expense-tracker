package com.cs446.expensetracker.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cs446.expensetracker.api.RetrofitInstance
import kotlinx.coroutines.launch

class HealthcheckViewModel : ViewModel() {
    private val _status1 = MutableLiveData<ConnectionStatus>()
    private val _status2 = MutableLiveData<ConnectionStatus>()
    val status1: LiveData<ConnectionStatus> get() = _status1
    val status2: LiveData<ConnectionStatus> get() = _status2

    init {
        viewModelScope.launch {
            getConnectionStatuses()
        }
    }

    private suspend fun getConnectionStatuses() {
        try {
            val response1 = RetrofitInstance.apiService.getBackendConnectionStatus()
            val response2 = RetrofitInstance.apiService2.getBackendConnectionStatus()
            _status1.value = ConnectionStatus(response1.code(), response1.body()?.status ?: "Unknown")
            _status2.value = ConnectionStatus(response2.code(), response2.body()?.status ?: "Unknown")
        } catch (e: Exception) {
            _status1.value = ConnectionStatus(-1, "Unknown")
            _status2.value = ConnectionStatus(-1, "Unknown")
        }
    }
}

data class ConnectionStatus(
    val statusCode: Int,
    val status: String
)