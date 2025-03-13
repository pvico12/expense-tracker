package com.cs446.expensetracker.nav.settings

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.cs446.expensetracker.api.RetrofitInstance
import com.cs446.expensetracker.api.models.UserProfileResponse
import com.cs446.expensetracker.api.models.UserProfileUpdateRequest
import com.cs446.expensetracker.session.UserSession
import kotlinx.coroutines.launch
import retrofit2.Response

class ProfilePageContainer {

    @OptIn(ExperimentalMaterial3Api::class)

    @Composable
    fun ProfilePageScreen(modifier: Modifier = Modifier, onBackClick: () -> Unit) {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()

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
                    title = { Text("User Profile") },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (hasUnsavedChanges) {
                                showUnsavedChangesDialog = true
                            } else {
                                onBackClick()
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            },
            bottomBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Button(
                        onClick = {
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
                        }
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
                ) {
                    username?.let {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                                .padding(10.dp)
                        ) {
                            Text(text = "Username", modifier = Modifier.weight(1f))
                            TextField(
                                value = it,
                                onValueChange = { username = it.replace("\n", "") },
                                singleLine = true
                            )
                        }
                    }
                    firstname?.let {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                                .padding(10.dp)
                        ) {
                            Text(text = "First Name", modifier = Modifier.weight(1f))
                            TextField(
                                value = it,
                                onValueChange = { firstname = it.replace("\n", "") },
                                singleLine = true
                            )
                        }
                    }
                    lastname?.let{
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                                .padding(10.dp)
                        ) {
                            Text(text = "Last Name", modifier = Modifier.weight(1f))
                            TextField(
                                value = it,
                                onValueChange = { lastname = it.replace("\n", "") },
                                singleLine = true
                            )
                        }
                    }

                }
            }
        )

        if (showUnsavedChangesDialog) {
            AlertDialog(
                onDismissRequest = { showUnsavedChangesDialog = false },
                title = { Text("Unsaved Changes") },
                text = { Text("You have unsaved changes. Are you sure you want to go back?") },
                dismissButton = {
                    Button(onClick = { showUnsavedChangesDialog = false }) {
                        Text("Cancel")
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        showUnsavedChangesDialog = false
                        onBackClick()
                    }) {
                        Text("Yes, go back")
                    }
                }
            )
        }
    }
}