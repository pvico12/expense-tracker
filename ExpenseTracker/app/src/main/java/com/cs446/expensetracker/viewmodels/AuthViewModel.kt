package com.cs446.expensetracker.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.cs446.expensetracker.api.RetrofitInstance
import com.cs446.expensetracker.api.models.LoginRequest
import com.cs446.expensetracker.api.models.RegistrationRequest

class AuthViewModel : ViewModel() {

    init {

    }

    fun login(username: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.apiService.login(LoginRequest(username, password))
                if (response.isSuccessful) {
                    // Handle successful login
                    onSuccess()
                } else {
                    onError("Login failed")
                }
            } catch (e: Exception) {
                onError(e.message ?: "An error occurred")
            }
        }
    }

    fun register(
        username: String,
        password: String,
        firstname: String,
        lastname: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.apiService.register(RegistrationRequest(username, password, firstname, lastname))
                if (response.isSuccessful) {
                    // Handle successful registration
                    onSuccess()
                } else {
                    onError("Registration failed")
                }
            } catch (e: Exception) {
                onError(e.message ?: "An error occurred")
            }
        }
    }
}