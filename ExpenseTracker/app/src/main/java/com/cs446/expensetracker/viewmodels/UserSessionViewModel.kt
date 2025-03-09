package com.cs446.expensetracker.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cs446.expensetracker.api.BaseAPIService
import com.cs446.expensetracker.api.RetrofitInstance
import com.cs446.expensetracker.api.models.TokenRefreshRequest
import com.cs446.expensetracker.session.UserSession
import kotlinx.coroutines.launch

class UserSessionViewModel : ViewModel() {
    private val apiService: BaseAPIService = RetrofitInstance.apiService

    private val _isLoggedIn = MutableLiveData(UserSession.isLoggedIn)
    val isLoggedIn: LiveData<Boolean> get() = _isLoggedIn

    private val _fcmToken = MutableLiveData<String>()
    val fcmToken: LiveData<String> get() = _fcmToken

    fun initializeSession() {
        viewModelScope.launch {
            while (true) {
                if (UserSession.isLoggedIn) {
                    checkTokenValidity()
                }
                kotlinx.coroutines.delay(30000) // 30s delay
            }
        }
    }

    private fun decodeTokenExpiryTime(token: String): Long {
        val parts = token.split(".")
        if (parts.size != 3) throw IllegalArgumentException("Invalid JWT token")

        val payload = String(android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE))
        val json = org.json.JSONObject(payload)
        return json.getLong("exp") * 1000 // Convert to milliseconds
    }

    private suspend fun checkTokenValidity() {
        try {
            // Check expiration time of access token
            val tokenExpiryTime = decodeTokenExpiryTime(UserSession.access_token)
            val timeToExpiry = tokenExpiryTime - System.currentTimeMillis()
            // if access token expires in next 3 minutes, refresh it
            if (timeToExpiry > 0 && timeToExpiry < 180000) {
                refreshAccessToken()
            }
        } catch (e: Exception) {
            // Handle other exceptions if necessary
        }
    }

    private suspend fun refreshAccessToken() {
        try {
            val response = apiService.refreshToken(TokenRefreshRequest(UserSession.refresh_token))
            if (response.isSuccessful) {
                UserSession.access_token = response.body()?.access_token ?: ""
                UserSession.isLoggedIn = true
            } else {
                UserSession.isLoggedIn = false
            }
        } catch (e: Exception) {
            UserSession.isLoggedIn = false
        }
        _isLoggedIn.postValue(UserSession.isLoggedIn)
    }
}