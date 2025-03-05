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
                    })
                }
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
    val focusRequester1 = FocusRequester()
    val focusRequester2 = FocusRequester()
    val focusRequester3 = FocusRequester()
    val keyboardController = LocalSoftwareKeyboardController.current

    val buttonShape = RoundedCornerShape(26.dp)
    val textFieldShape = RoundedCornerShape(12.dp)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(120.dp))
        Text(
            text = "Get Started",
            color = tileColor,
            style = Typography.headlineMedium,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Create your account",
            style = Typography.bodySmall,
            color = tileColor
        )
        Spacer(modifier = Modifier.height(64.dp))
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
        Spacer(modifier = Modifier.height(16.dp))
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
        Spacer(modifier = Modifier.height(16.dp))
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
        Spacer(modifier = Modifier.height(16.dp))
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
        Spacer(modifier = Modifier.height(165.dp))
        Button(
            onClick = {
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
                    Text("SIGNUP", style=Typography.headlineSmall)
                }
            }
        }
        errorMessage?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = it, color = Color(0xFFDBAD8C))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Already have an account? ", style = Typography.bodySmall)
            TextButton(onClick = onSignupSuccess) {
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