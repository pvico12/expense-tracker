package com.cs446.expensetracker.deals

import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.cs446.expensetracker.api.RetrofitInstance
import com.cs446.expensetracker.api.models.DealSubCreationRequest
import com.cs446.expensetracker.api.models.DealSubRetrievalResponse
import com.cs446.expensetracker.session.UserSession
import com.cs446.expensetracker.ui.ui.theme.Typography
import com.cs446.expensetracker.ui.ui.theme.mainTextColor
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response


@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AddSubScreen(editVersion: Int, onDismissRequest: () -> Unit) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var address by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var latlngPrediction by remember { mutableStateOf<LatLng?>(null) }

    var specificSubToEdit: DealSubRetrievalResponse? by remember { mutableStateOf(null) }

    fun apiFetchSpecificSub() {
        Log.d("Response", "Api fetch was called, but request not necessarily sent")
        CoroutineScope(Dispatchers.IO).launch {
            isLoading = true
            errorMessage = ""
            try {
                val token = UserSession.access_token ?: ""
                val response: Response<DealSubRetrievalResponse> =
                    RetrofitInstance.apiService.getSpecificSub(editVersion)
                Log.d("Response", "Fetch Subs API Request actually called")
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    Log.d("Response", "Subs Response: $responseBody")
                    specificSubToEdit = responseBody
                } else {
                    errorMessage = "Failed to load data."
                    Log.d("Error", "Subs API Response Was Unsuccessful: $response")
                }
            } catch (e: Exception) {
                errorMessage = "Error: ${e.message}"
                Log.d("Error", "Error Calling Subs API: $errorMessage")
            } finally {
                isLoading = false
            }
            address = specificSubToEdit?.address ?: ""
            if (specificSubToEdit != null) {
                latlngPrediction = LatLng(specificSubToEdit!!.latitude.toDouble(), specificSubToEdit!!.longitude.toDouble())
            }
        }

    }

    ModalBottomSheet( onDismissRequest ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scrollState),
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                var titleText = "Add New Address"
                if (editVersion != -1) {
                    titleText = "Edit Subscribed Location"
                }
                Text(
                    text = titleText,
                    color = mainTextColor,
                    style = Typography.titleMedium,
                )
                Spacer(modifier = Modifier.weight(1.0f))
            }
            LaunchedEffect(Unit) {
                if (editVersion != -1) {
                    apiFetchSpecificSub()
                }
            }

            AutoComplete(address) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        address = it.address
                        latlngPrediction = it.latLng
                        Log.d("TAG", "AutoComplete: $address $latlngPrediction")
                    } catch (e: Exception) {
                        Log.d("TAG", "Error getting Location from Autocomplete: $e")
                    }
                }
            }

            if (errorMessage != null) {
                Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
            }
            Spacer(modifier = Modifier.height(6.dp))

            // Subscribe Button
            Button(
                onClick = {
                    CoroutineScope(Dispatchers.IO).launch {
                        isLoading = true
                        errorMessage = null
                        try {
                            Log.d("TAG", "SUBSCRIBE: $address $latlngPrediction")
                            if (address == "" || latlngPrediction == null) {
                                errorMessage = errorMessage ?: ""
                                errorMessage += "Please pick an address from the autocomplete dropdown\n"
                            }

                            if (errorMessage == null) {
                                val sub = DealSubCreationRequest(
                                    address = address,
                                    longitude = latlngPrediction?.longitude ?: -80.495064,
                                    latitude = latlngPrediction?.latitude ?: 43.452969
                                )

                                if (editVersion != -1) {
                                    val token = UserSession.access_token ?: ""
                                    val response =
                                        RetrofitInstance.apiService.updateSub(editVersion, sub)
                                    if (response.isSuccessful) {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(
                                                context,
                                                "Edit successful!",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                        Log.d(
                                            "Response",
                                            "Edit Sub Response: ${response.body()}"
                                        )
                                        onDismissRequest()
                                    } else {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(
                                                context,
                                                "Failed to edit sub. Please try again",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                        Log.d(
                                            "Response",
                                            "Api request to edit sub failed: ${response.body()}"
                                        )
                                    }
                                } else {
                                    val token = UserSession.access_token ?: ""
                                    val response = RetrofitInstance.apiService.addSub(sub)
                                    if (response.isSuccessful) {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(
                                                context,
                                                "Address added!",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                        Log.d(
                                            "Response",
                                            "Add Sub Response: ${response.body()}"
                                        )
                                        onDismissRequest()
                                    } else {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(
                                                context,
                                                "Failed to add sub. Please try again",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                        Log.d(
                                            "Response",
                                            "Api request to add sub failed: ${response.body()}"
                                        )
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.d("Response", "Exception when adding/editing sub: ${e.message}")
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    Color(0xFF4B0C0C),
                ),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    if (editVersion == -1)
                        Text(text = "Subscribe")
                    else
                        Text(text = "Save")
                }
            }
        }
    }
}