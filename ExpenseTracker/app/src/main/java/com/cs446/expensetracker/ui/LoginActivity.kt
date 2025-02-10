package com.cs446.expensetracker.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.auth0.jwt.JWT
import com.cs446.expensetracker.MainActivity
import com.cs446.expensetracker.api.RetrofitInstance
import com.cs446.expensetracker.api.models.LoginRequest
import com.cs446.expensetracker.session.UserSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LoginScreen(onLoginSuccess = {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }, onCreateAccountClick = {
                startActivity(Intent(this, SignupActivity::class.java))
            })
        }
    }
}

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit, onCreateAccountClick: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") }
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            isLoading = true
            errorMessage = null
            CoroutineScope(Dispatchers.IO).launch {
                val result = login(username, password)
                withContext(Dispatchers.Main) {
                    isLoading = false
                    if (result) {
                        onLoginSuccess()
                    } else {
                        errorMessage = "Login failed. Please try again."
                    }
                }
            }
        }) {
            if (isLoading) {
                CircularProgressIndicator()
            } else {
                Text("Login")
            }
        }
        errorMessage?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onCreateAccountClick) {
            Text("Create Account")
        }
    }
}

suspend fun login(username: String, password: String): Boolean {
    return try {
        val response = RetrofitInstance.apiService.login(LoginRequest(username, password))
        if (response.isSuccessful) {
            val loginResponse = response.body()
            if (loginResponse != null) {
                UserSession.isLoggedIn = true
                UserSession.access_token = loginResponse.access_token
                UserSession.refresh_token = loginResponse.refresh_token

                // decode access token to retrieve user id
                val jwt = JWT.decode(loginResponse.access_token)
                UserSession.userId = jwt.getClaim("user_id").asInt()
                true
            } else {
                false
            }
        } else {
            false
        }
    } catch (e: Exception) {
        false
    }
}