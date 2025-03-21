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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auth0.jwt.JWT
import com.cs446.expensetracker.MainActivity
import com.cs446.expensetracker.R
import com.cs446.expensetracker.api.RetrofitInstance
import com.cs446.expensetracker.api.models.FcmTokenUploadRequest
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
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }, onCreateAccountClick = {
                    startActivity(Intent(this, SignupActivity::class.java))
                })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(onLoginSuccess: () -> Unit, onCreateAccountClick: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp

    val buttonShape = RoundedCornerShape(26.dp)
    val textFieldShape = RoundedCornerShape(12.dp)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding((screenHeight * 0.05f).dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height((screenHeight * 0.25f).dp))
        Text(
            text = "Welcome Back",
            color = tileColor,
            style = Typography.headlineMedium,
        )
        Spacer(modifier = Modifier.height((screenHeight * 0.01f).dp))
        Text(
            text = "Login to your account",
            style = Typography.bodySmall,
            color = tileColor
        )
        Spacer(modifier = Modifier.height((screenHeight * 0.07f).dp))
        TextField(
            value = username,
            onValueChange = { username = it.replace("\n", "") },
            label = { Text("Username") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = textFieldShape,
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(
                onNext = { focusRequester.requestFocus() }
            )
        )
        Spacer(modifier = Modifier.height((screenHeight * 0.02f).dp))
        TextField(
            value = password,
            onValueChange = { password = it.replace("\n", "") },
            label = { Text("Password") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
            visualTransformation = PasswordVisualTransformation(),
            shape = textFieldShape,
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = { keyboardController?.hide() }
            )
        )
        Spacer(modifier = Modifier.height((screenHeight * 0.2f).dp))
        Button(
            onClick = {
                if (username.isEmpty() && password.isEmpty()) {
                    errorMessage = "Missing username and password!"
                    return@Button
                }
                if (username.isEmpty()) {
                    errorMessage = "Missing username!"
                    return@Button
                }
                if (password.isEmpty()) {
                    errorMessage = "Missing password!"
                    return@Button
                }

                isLoading = true
                errorMessage = null
                CoroutineScope(Dispatchers.IO).launch {
                    val result = login(username, password)
                    withContext(Dispatchers.Main) {
                        isLoading = false
                        if (result) {
                            onLoginSuccess()
                        } else {
                            errorMessage = "Invalid credentials. Please try again."
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .graphicsLayer {
                    shadowElevation = 8f
                    shape = buttonShape
                    clip = true
                    translationX = 4f
                    translationY = 4f
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
                    Text(text = "LOGIN", style = Typography.headlineSmall)
                }
            }
        }
        errorMessage?.let {
            Spacer(modifier = Modifier.height((screenHeight * 0.005f).dp))
            Text(text = it, color = Color(0xFFDBAD8C))
        }
        Spacer(modifier = Modifier.height((screenHeight * 0.01f).dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Don't have an account? ", style = Typography.bodySmall)
            TextButton(onClick = onCreateAccountClick) {
                Text(
                    text = "Sign up",
                    color = tileColor,
                    style = Typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    textDecoration = TextDecoration.Underline
                )
            }
        }
    }
}

suspend fun login(username: String, password: String): Boolean {
    if (username.isEmpty() || password.isEmpty()) {
        return false
    }
    return try {
        val response = RetrofitInstance.apiService.login(LoginRequest(username, password))
        if (response.isSuccessful) {
            val loginResponse = response.body()
            if (loginResponse != null) {
                UserSession.isLoggedIn = true
                UserSession.access_token = loginResponse.access_token
                UserSession.refresh_token = loginResponse.refresh_token
                UserSession.role = loginResponse.role

                // decode access token to retrieve user id
                val jwt = JWT.decode(loginResponse.access_token)
                UserSession.userId = jwt.getClaim("user_id").asInt()

                // send FCM token if it exists
                if (UserSession.fcmToken != "") {
                    RetrofitInstance.apiService.uploadFcmToken(FcmTokenUploadRequest(UserSession.fcmToken))
                }
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