package com.cs446.expensetracker.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auth0.jwt.JWT
import com.cs446.expensetracker.MainActivity
import com.cs446.expensetracker.R
import com.cs446.expensetracker.api.RetrofitInstance
import com.cs446.expensetracker.api.models.LoginRequest
import com.cs446.expensetracker.session.UserSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.cs446.expensetracker.ui.ui.theme.mainTextColor
import com.cs446.expensetracker.ui.ui.theme.tileColor
import com.cs446.expensetracker.ui.ui.theme.Typography
import com.cs446.expensetracker.ui.ui.theme.Pink40
import com.cs446.expensetracker.ui.ui.theme.Purple40


class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Pink40
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.coin_half),
                        contentDescription = "Coin Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter),
                    )
                }
                LoginScreen(onLoginSuccess = {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }, onCreateAccountClick = {
                    startActivity(Intent(this, SignupActivity::class.java))
                })
            }
        }
    }
}

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit, onCreateAccountClick: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val buttonShape = RoundedCornerShape(26.dp)
    val textFieldShape = RoundedCornerShape(12.dp)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(250.dp))
        Text(
            text = "Welcome Back",
            color = tileColor,
            style = Typography.headlineLarge,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Login to your account",
            style = Typography.bodySmall,
            color = tileColor
        )
        Spacer(modifier = Modifier.height(64.dp))
        TextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth(),
            shape = textFieldShape,
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            shape = textFieldShape,
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )
        Spacer(modifier = Modifier.height(180.dp))
        Button(
            onClick = {
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
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .graphicsLayer {
                    shadowElevation = 8f  // Elevation of the shadow
                    shape = buttonShape    // Consistent shape for shadow
                    clip = true            // Clip to the shape to prevent overflow
                    translationX = 4f      // Move shadow right (adjust for more/less)
                    translationY = 4f      // Move shadow down (adjust for more/less)
                },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = tileColor
            ),
            contentPadding = PaddingValues(0.dp),
            shape = buttonShape
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                mainTextColor,
                                Purple40
                            )
                        ),
                        shape = buttonShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator()
                } else {
                    Text(text="LOGIN", style=Typography.headlineSmall)
                }
            }
        }
        errorMessage?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = it, color = Color(0xFFC08261))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Don't have an account? ")
            TextButton(onClick = onCreateAccountClick) {
                Text(
                    text = "Sign up",
                    color = tileColor,
                    fontWeight = FontWeight.Bold,
                    textDecoration = TextDecoration.Underline
                )
            }
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