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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.cs446.expensetracker.MainActivity
import com.cs446.expensetracker.api.RetrofitInstance
import com.cs446.expensetracker.api.models.RegistrationRequest
import com.cs446.expensetracker.session.UserSession
import com.cs446.expensetracker.ui.ui.theme.Pink40
import com.cs446.expensetracker.ui.ui.theme.Purple40
import com.cs446.expensetracker.ui.ui.theme.Typography
import com.cs446.expensetracker.ui.ui.theme.mainTextColor
import com.cs446.expensetracker.ui.ui.theme.tileColor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SignupActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Pink40
            ) {
                ExpenseTrackerTheme {
                    SignupScreen(onSignupSuccess = {
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    }, onLoginClick = {
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    })
                }
            }
        }
    }
}

@Composable
fun SignupScreen(onSignupSuccess: () -> Unit, onLoginClick: () -> Unit) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val focusRequester1 = FocusRequester()
    val focusRequester2 = FocusRequester()
    val focusRequester3 = FocusRequester()
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
        Spacer(modifier = Modifier.height((screenHeight * 0.12f).dp))
        Text(
            text = "Get Started",
            color = tileColor,
            style = Typography.headlineMedium,
        )
        Spacer(modifier = Modifier.height((screenHeight * 0.01f).dp))
        Text(
            text = "Create your account",
            style = Typography.bodySmall,
            color = tileColor
        )
        Spacer(modifier = Modifier.height((screenHeight * 0.07).dp))
        TextField(
            value = firstName,
            onValueChange = { firstName = it.replace("\n", "") },
            label = { Text("First Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = textFieldShape,
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(
                onNext = { focusRequester1.requestFocus() }
            )
        )
        Spacer(modifier = Modifier.height((screenHeight * 0.02f).dp))
        TextField(
            value = lastName,
            onValueChange = { lastName = it.replace("\n", "") },
            label = { Text("Last Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester1),
            shape = textFieldShape,
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(
                onNext = { focusRequester2.requestFocus() }
            )
        )
        Spacer(modifier = Modifier.height((screenHeight * 0.02f).dp))
        TextField(
            value = username,
            onValueChange = { username = it.replace("\n", "") },
            label = { Text("Username") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester2),
            shape = textFieldShape,
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(
                onNext = { focusRequester3.requestFocus() }
            )
        )
        Spacer(modifier = Modifier.height((screenHeight * 0.02f).dp))
        TextField(
            value = password,
            onValueChange = { password = it.replace("\n", "") },
            label = { Text("Password") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester3),
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
        Spacer(modifier = Modifier.height((screenHeight * 0.03f).dp))
        if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .height((screenHeight * 0.08f).dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = errorMessage ?: "", color = Color(0xFFDBAD8C), style = Typography.bodySmall)
            }
        } else {
            Spacer(modifier = Modifier.height((screenHeight * 0.08f).dp))
        }
        Spacer(modifier = Modifier.height((screenHeight * 0.03f).dp))
        Button(
            onClick = {
                if (firstName.isEmpty() || lastName.isEmpty() || username.isEmpty() || password.isEmpty()) {
                    errorMessage = "Please fill in all fields."
                    return@Button
                }

                // password complexity check (at least 8 characters, 1 uppercase, 1 lowercase, 1 number)
                if (!Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d]{8,}\$").matches(password)) {
                    errorMessage = "Password must be at least 8 characters long and contain at least 1 uppercase letter, 1 lowercase letter, and 1 number."
                    return@Button
                }

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
                    Text("SIGN UP", style=Typography.headlineSmall)
                }
            }
        }

        Spacer(modifier = Modifier.height((screenHeight * 0.01f).dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Already have an account? ", style = Typography.bodySmall)
            TextButton(onClick = onLoginClick) {
                Text(
                    text = "Login",
                    color = tileColor,
                    style = Typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    textDecoration = TextDecoration.Underline
                )
            }
        }
    }
    SnackbarHost(hostState = snackbarHostState)
}

suspend fun signup(firstName: String, lastName: String, username: String, password: String): Boolean {
    if (username.isEmpty() || password.isEmpty() || firstName.isEmpty() || lastName.isEmpty()) {
        return false
    }
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