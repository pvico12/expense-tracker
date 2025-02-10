package com.cs446.expensetracker.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.cs446.expensetracker.ui.ui.theme.ExpenseTrackerTheme
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.cs446.expensetracker.MainActivity
import com.cs446.expensetracker.api.RetrofitInstance
import com.cs446.expensetracker.api.models.RegistrationRequest
import com.cs446.expensetracker.session.UserSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SignupActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ExpenseTrackerTheme {
                SignupScreen(onSignupSuccess = {
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                })
            }
        }
    }
}

@Composable
fun SignupScreen(onSignupSuccess: () -> Unit) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextField(
            value = firstName,
            onValueChange = { firstName = it },
            label = { Text("First Name") }
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = lastName,
            onValueChange = { lastName = it },
            label = { Text("Last Name") }
        )
        Spacer(modifier = Modifier.height(8.dp))
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
                val result = signup(firstName, lastName, username, password)
                withContext(Dispatchers.Main) {
                    isLoading = false
                    if (result) {
                        snackbarHostState.showSnackbar("Signup successful!")
                        onSignupSuccess()
                    } else {
                        errorMessage = "Signup failed. Please try again."
                    }
                }
            }
        }) {
            if (isLoading) {
                CircularProgressIndicator()
            } else {
                Text("Signup")
            }
        }
        errorMessage?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }
    }
    SnackbarHost(hostState = snackbarHostState)
}

suspend fun signup(firstName: String, lastName: String, username: String, password: String): Boolean {
    return try {
        val response = RetrofitInstance.apiService.register(
            RegistrationRequest(
                username = username,
                password = password,
                firstname = firstName,
                lastname = lastName
            ))
        if (response.isSuccessful) {
            true
        } else {
            false
        }
    } catch (e: Exception) {
        false
    }
}