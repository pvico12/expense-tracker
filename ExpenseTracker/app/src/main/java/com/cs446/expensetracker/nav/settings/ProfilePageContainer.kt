package com.cs446.expensetracker.nav.settings

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.cs446.expensetracker.api.RetrofitInstance
import com.cs446.expensetracker.api.models.UserProfileResponse
import com.cs446.expensetracker.api.models.UserProfileUpdateRequest
import com.cs446.expensetracker.session.UserSession
import com.cs446.expensetracker.ui.ui.theme.*
import kotlinx.coroutines.launch
import retrofit2.Response

class ProfilePageContainer {

    @OptIn(ExperimentalMaterial3Api::class)

    @Composable
    fun ProfilePageScreen(modifier: Modifier = Modifier, onBackClick: () -> Unit) {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()

        val textFieldShape = RoundedCornerShape(12.dp)

        var username by remember { mutableStateOf("") }
        var firstname by remember { mutableStateOf("") }
        var lastname by remember { mutableStateOf("") }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        var originalUsername by remember { mutableStateOf<String?>(null) }
        var originalFirstname by remember { mutableStateOf<String?>(null) }
        var originalLastname by remember { mutableStateOf<String?>(null) }

        var showUnsavedChangesDialog by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            errorMessage = null
            try {
                val token = UserSession.access_token ?: ""
                val userId = UserSession.userId
                val response: Response<UserProfileResponse> = RetrofitInstance.apiService.getUserProfile(userId)
                if (response.isSuccessful) {
                    val profileResponse = response.body()
                    username = profileResponse?.username?.takeIf { it.isNotEmpty() } ?: ""
                    firstname = profileResponse?.firstname?.takeIf { it.isNotEmpty() } ?: ""
                    lastname = profileResponse?.lastname?.takeIf { it.isNotEmpty() } ?: ""

                    originalUsername = profileResponse?.username
                    originalFirstname = profileResponse?.firstname
                    originalLastname = profileResponse?.lastname
                } else {
                    errorMessage = "Failed to load profile info."
                }
            } catch (e: Exception) {
                errorMessage = "Error: ${e.message}"
            }
        }

        val hasUnsavedChanges = username != originalUsername || firstname != originalFirstname || lastname != originalLastname

        BackHandler(enabled = hasUnsavedChanges) {
            showUnsavedChangesDialog = true
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("User Profile", color = mainTextColor) },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (hasUnsavedChanges) {
                                showUnsavedChangesDialog = true
                            } else {
                                onBackClick()
                            }
                        }, colors = IconButtonDefaults.iconButtonColors(contentColor = mainTextColor)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = mainBackgroundColor)
                )
            },
            bottomBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(mainBackgroundColor)
                        .padding(16.dp),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Button(
                        onClick = {
                            if (username.isEmpty() || firstname.isEmpty() || lastname.isEmpty()) {
                                errorMessage = "Please fill in all fields!"
                                return@Button
                            }

                            coroutineScope.launch {
                                val requestBody = UserProfileUpdateRequest(
                                    firstname = firstname,
                                    lastname = lastname,

                                    username = username
                                )
                                val response = RetrofitInstance.apiService.updateUserProfile(requestBody)
                                if (response.isSuccessful) {
                                    originalUsername = username
                                    originalFirstname = firstname
                                    originalLastname = lastname
                                    Log.d("Profile","Profile changes saved successfully: $username, $firstname, $lastname")
                                    Toast.makeText(context, "Profile saved successfully!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Log.d("Profile","Failed to save profile changes: ${response.errorBody()?.string()}")
                                    Toast.makeText(context, "Failed to save profile. Please try again.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = mainTextColor, contentColor = Color(0xFFF6F3F3))
                    ) {
                        Text("Save")
                    }
                }
            },
            content = { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(mainBackgroundColor)
                ) {
                    username.let {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                                .padding(10.dp)
                        ) {
                            Text(text = "Username", color = mainTextColor, modifier = Modifier.weight(1f))
                            TextField(
                                value = it,
                                onValueChange = { username = it.replace("\n", "") },
                                singleLine = true,
                                shape = textFieldShape,
                                modifier = Modifier.semantics { contentDescription = "Username Input" },
                                colors = TextFieldDefaults.colors(
                                    unfocusedContainerColor = Color(0xFFFAF2ED),
                                    focusedContainerColor = Color(0xFFFAF2ED),
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                )
                            )
                        }
                    }
                    firstname.let {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                                .padding(10.dp)
                        ) {
                            Text(text = "First Name", color = mainTextColor, modifier = Modifier.weight(1f))
                            TextField(
                                value = it,
                                onValueChange = { firstname = it.replace("\n", "") },
                                singleLine = true,
                                shape = textFieldShape,
                                modifier = Modifier.semantics { contentDescription = "First Name Input" },
                                colors = TextFieldDefaults.colors(
                                    unfocusedContainerColor = Color(0xFFFAF2ED),
                                    focusedContainerColor = Color(0xFFFAF2ED),
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                )
                            )
                        }
                    }
                    lastname.let{
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                                .padding(10.dp)
                        ) {
                            Text(text = "Last Name", color = mainTextColor, modifier = Modifier.weight(1f))
                            TextField(
                                value = it,
                                onValueChange = { lastname = it.replace("\n", "") },
                                singleLine = true,
                                shape = textFieldShape,
                                modifier = Modifier.semantics { contentDescription = "Last Name Input" },
                                colors = TextFieldDefaults.colors(
                                    unfocusedContainerColor = Color(0xFFFAF2ED),
                                    focusedContainerColor = Color(0xFFFAF2ED),
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                )
                            )
                        }
                    }

                    errorMessage?.let {
                        Text(
                            text = it,
                            color = Color.Red,
                            modifier = Modifier.padding(10.dp)
                        )
                    }

                }
            }
        )

        if (showUnsavedChangesDialog) {
            AlertDialog(
                onDismissRequest = { showUnsavedChangesDialog = false },
                title = { Text("Unsaved Changes", color = mainTextColor) },
                text = { Text("You have unsaved changes. Are you sure you want to go back?", color = mainTextColor) },
                dismissButton = {
                    Button(
                        onClick = { showUnsavedChangesDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = mainTextColor, contentColor = Color(0xFFF6F3F3))
                    ) {
                        Text("Cancel")
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showUnsavedChangesDialog = false
                            onBackClick()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = mainTextColor, contentColor = Color(0xFFF6F3F3))) {
                        Text("Yes, go back")
                    }
                }
            )
        }
    }
}