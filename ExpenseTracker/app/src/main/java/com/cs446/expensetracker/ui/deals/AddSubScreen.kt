package com.cs446.expensetracker.ui.deals

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
import com.cs446.expensetracker.ui.ui.theme.Typography
import com.cs446.expensetracker.ui.ui.theme.mainTextColor
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs


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
                    titleText = "Edit Subscribed Address"
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
                    Log.d("Response", "Api fetch was called, but request not necessarily sent")
                    CoroutineScope(Dispatchers.IO).launch {
                        isLoading = true
                        errorMessage = ""

                        val specificSub = getSub(editVersion)
                        if (specificSub != null) {
                            specificSubToEdit = specificSub
                        } else {
                            errorMessage = "Failed to load data."
                        }
                        isLoading = false

                        address = specificSubToEdit?.address ?: ""
                        if (specificSubToEdit != null) {
                            latlngPrediction = LatLng(specificSubToEdit!!.latitude.toDouble(), specificSubToEdit!!.longitude.toDouble())
                        }
                    }
                }
            }

            AutoComplete(address, onSelect = { autoCompleteInfo ->
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        address = autoCompleteInfo.address
                        latlngPrediction = autoCompleteInfo.latLng
                        Log.d("TAG", "AutoComplete: $address $latlngPrediction")
                    } catch (e: Exception) {
                        Log.d("TAG", "Error getting Location from Autocomplete: $e")
                    }
                }
            }, onTextChanged = { currentText ->
                if (currentText != address) latlngPrediction = null
            })

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
                        if (address == "" || latlngPrediction == null) {
                            errorMessage = "Please pick an address from the autocomplete dropdown\n"
                        }

                        if (errorMessage == null) {
                            if (editVersion != -1) {
                                if (updateSub(
                                        editVersion,
                                        address,
                                        latlngPrediction?.longitude,
                                        latlngPrediction?.latitude)) {
                                    onDismissRequest()
                                } else {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(
                                            context,
                                            "Failed to edit sub. Please try again",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            } else {
                                if (createSub(
                                        address,
                                        latlngPrediction?.longitude,
                                        latlngPrediction?.latitude)) {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(
                                            context,
                                            "Address added!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    onDismissRequest()
                                } else {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(
                                            context,
                                            "Failed to add sub. Please try again",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        }

                        isLoading = false
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

suspend fun createSub(address : String,
                      longitude : Double?,
                      latitude : Double?
) : Boolean {
    if (address.isInvalid() || longitude.isInvalid() || latitude.isInvalid()) {
        return false
    }
    if (!longitude.isInvalid() && !latitude.isInvalid()) {
        if (abs(longitude!!) > 180 || abs(latitude!!) > 90)
            return false
    }
    return try {
        val sub = DealSubCreationRequest(
            address = address,
            longitude = longitude!!,
            latitude = latitude!!
        )

        val response = RetrofitInstance.apiService.addSub(sub)
        if (response.isSuccessful) {
            Log.d("Response","Add Sub Response: ${response.body()}")
            true
        } else {
            Log.d("Response","Api request to add sub failed: ${response.body()}")
            false
        }
    } catch (e: Exception) {
        Log.d("Response", "Exception when adding sub: ${e.message}")
        false
    }
}

suspend fun updateSub(id: Int,
                      address : String,
                      longitude : Double?,
                      latitude : Double?
) : Boolean {
    if (address.isInvalid() || longitude.isInvalid() || latitude.isInvalid()) {
        return false
    }
    if (!longitude.isInvalid() && !latitude.isInvalid()) {
        if (abs(longitude!!) > 180 || abs(latitude!!) > 90)
            return false
    }
    return try {
        val sub = DealSubCreationRequest(
            address = address,
            longitude = longitude!!,
            latitude = latitude!!
        )

        val response =
            RetrofitInstance.apiService.updateSub(id, sub)
        if (response.isSuccessful) {
            Log.d("Response","Edit Sub Response: ${response.body()}")
            true
        } else {
            Log.d("Response", "Api request to edit sub failed: ${response.body()}")
            false
        }
    } catch (e: Exception) {
        Log.d("Response", "Exception when updating sub: ${e.message}")
        false
    }
}

suspend fun getSub(id: Int) : DealSubRetrievalResponse? {
    return try {
        val response = RetrofitInstance.apiService.getSpecificSub(id)
        Log.d("Response", "Fetch Subs API Request actually called")
        if (response.isSuccessful) {
            response.body().also {
                Log.d("Response", "Subs Response: $it")
            }
        } else {
            Log.d("Error", "Subs API Response Was Unsuccessful: $response")
            null
        }
    } catch (e: Exception) {
        Log.d("Error", "Error Calling Subs API: $e")
        null
    }
}