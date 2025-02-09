package com.cs446.expensetracker.viewmodels

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

    fun initializeSession() {
        viewModelScope.launch {
            while (true) {
                if (UserSession.isLoggedIn) {
                    fetchUserProfile()
                }
                kotlinx.coroutines.delay(30000) // 1 minute delay
            }
        }
    }

    private suspend fun fetchUserProfile() {
        try {
            val response = apiService.getUserProfile(UserSession.userId)
            if (response.code() == 401) {
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