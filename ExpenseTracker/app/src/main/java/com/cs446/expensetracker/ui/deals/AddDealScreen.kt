package com.cs446.expensetracker.ui.deals

import android.app.DatePickerDialog
import android.content.Context
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.cs446.expensetracker.api.RetrofitInstance
import com.cs446.expensetracker.api.models.DealCreationRequest
import com.cs446.expensetracker.api.models.DealRetrievalResponse
import com.cs446.expensetracker.session.UserSession
import com.cs446.expensetracker.ui.ui.theme.Typography
import com.cs446.expensetracker.ui.ui.theme.mainTextColor
import com.cs446.expensetracker.ui.ui.theme.secondTextColor
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs


@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDealScreen(navController: NavController, editVersion: Int, onChangeSuccess: () -> Unit ) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var itemName by remember { mutableStateOf("") }
    var vendor by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var goBack by remember { mutableStateOf(false) }

    var latlngPrediction by remember {mutableStateOf<LatLng?>(null)}

    // Date Picker State
    val calendar = Calendar.getInstance()
    val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
    var selectedDate by remember {
        mutableStateOf(
            isoFormat.format(calendar.time)
        )
    }

    // Date Picker Dialog
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            calendar.set(year, month, dayOfMonth, 12, 12, 12)
            selectedDate = isoFormat.format(calendar.time)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    var specificDealToEdit: DealRetrievalResponse? by remember { mutableStateOf(null) }



    @Composable
    fun allFieldInputs() {
        OutlinedTextField(
            value = itemName,
            onValueChange = { itemName = it },
            label = { Text("Item Name") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = secondTextColor,
                unfocusedIndicatorColor = mainTextColor)
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = vendor,
            onValueChange = { vendor = it },
            label = { Text("Vendor") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = secondTextColor,
                unfocusedIndicatorColor = mainTextColor)
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = secondTextColor,
                unfocusedIndicatorColor = mainTextColor)
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = price,
            onValueChange = { price = it },
            label = { Text("Price") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = secondTextColor,
                unfocusedIndicatorColor = mainTextColor)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Date Picker
        Text(text = "Date", style = MaterialTheme.typography.bodyLarge, color= mainTextColor)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clickable { datePickerDialog.show() }
                .padding(10.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(text = selectedDate.substringBeforeLast(("T")))
        }

        Spacer(modifier = Modifier.height(10.dp))

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
        },onTextChanged = { currentText ->
            if (currentText != address) latlngPrediction = null
        })

        Spacer(modifier = Modifier.height(10.dp))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            var titleText = "Submit a Deal"
            if (editVersion != -1) {
                titleText = "Edit Deal"
            }
            Text(
                text = titleText,
                color = mainTextColor,
                style = Typography.titleLarge,
            )
            Spacer(modifier = Modifier.weight(1.0f))
            TextButton(onClick = { navController.popBackStack() },
                contentPadding = PaddingValues(
                    start = 5.dp,
                    top = 0.dp,
                    end = 0.dp,
                    bottom = 10.dp,
                )) {
                Text("X",  style = Typography.titleLarge, color= Color(0xFF4B0C0C))
            }
        }
        LaunchedEffect(Unit) {
            if (editVersion != -1) {
                Log.d("Response", "Api fetch was called, but request not necessarily sent")
                CoroutineScope(Dispatchers.IO).launch {
                    isLoading = true
                    errorMessage = ""

                    val specificDeal = getDeal(editVersion.toString())
                    if (specificDeal != null) {
                        specificDealToEdit = specificDeal
                    } else {
                        errorMessage = "Failed to load data."
                    }
                    isLoading = false

                    itemName = specificDealToEdit?.name ?: ""
                    vendor = specificDealToEdit?.vendor ?: ""
                    description = specificDealToEdit?.description ?: ""
                    price = specificDealToEdit?.price.toString()
                    selectedDate = specificDealToEdit?.date ?: ""
                    address = specificDealToEdit?.address ?: ""
                    if (specificDealToEdit != null) {
                        latlngPrediction = LatLng(specificDealToEdit!!.latitude.toDouble(), specificDealToEdit!!.longitude.toDouble())
                    }
                }
            }
        }
        allFieldInputs()
        // Error Message Display
        if (errorMessage != null) {
            Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
//            Spacer(modifier = Modifier.height(10.dp))
        }

        // Save Expense Button
        Button(
            onClick = {
                CoroutineScope(Dispatchers.IO).launch {
                    isLoading = true
                    errorMessage = null
                    if (itemName == "") {
                        errorMessage = "Please add an item name\n"
                    }
                    if (vendor == "") {
                        errorMessage = errorMessage ?: ""
                        errorMessage += "Please add a vendor name\n"
                    }
                    if (description == "") {
                        errorMessage = errorMessage ?: ""
                        errorMessage += "Please add a description\n"
                    }
                    val amount = price.toDoubleOrNull()
                    if (amount == null) {
                        errorMessage = errorMessage ?: ""
                        errorMessage += "Please set a numerical price\n"
                    }
                    if (amount != null && amount < 0.0) {
                        errorMessage = errorMessage ?: ""
                        errorMessage += "Please have price be above 0\n"
                    }
                    if (selectedDate == "") {
                        errorMessage = errorMessage ?: ""
                        errorMessage += "Please add a date\n"
                    }
                    if (address == "" || latlngPrediction == null)  {
                        errorMessage = errorMessage ?: ""
                        errorMessage += "Please pick an address from the autocomplete dropdown\n"
                    }

                    if(errorMessage == null) {
                        if(editVersion != -1) {
                            if (updateDeal(
                                    editVersion.toString(),
                                    itemName,
                                    description,
                                    vendor,
                                    price.toDouble(),
                                    selectedDate,
                                    address,
                                    latlngPrediction?.longitude,
                                    latlngPrediction?.latitude)) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        context,
                                        "Deal updated successfully!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                onChangeSuccess()
                            } else {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        context,
                                        "Failed to edit deal. Please try again",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        } else {
                            if (createDeal(
                                    itemName,
                                    description,
                                    vendor,
                                    price.toDouble(),
                                    selectedDate,
                                    address,
                                    latlngPrediction?.longitude,
                                    latlngPrediction?.latitude)) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        context,
                                        "Deal added successfully!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                onChangeSuccess()
                            } else {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        context,
                                        "Failed to add deal. Please try again",
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
                var btnText = "Add"
                if (editVersion != -1) {
                    btnText = "Update"
                }
                Text(text = btnText)
            }
        }
    }
}

fun String?.isInvalid() = this.isNullOrBlank()
fun Double?.isInvalid() = this == null
fun Int?.isInvalid() = this == null

suspend fun createDeal(name : String,
                       description : String,
                       vendor : String,
                       price : Double,
                       date : String, // ISO 8601 format
                       address : String,
                       longitude : Double?,
                       latitude : Double?
) : Boolean {
    if (listOf(name, description, vendor, date, address).any { it.isInvalid() }
        || price.isInvalid() || longitude.isInvalid() || latitude.isInvalid()) {
        return false
    }
    if (!longitude.isInvalid() && !latitude.isInvalid()) {
        if (abs(longitude!!) > 180 || abs(latitude!!) > 90)
            return false
    }
    return try {
        val deal = DealCreationRequest (
            name = name,
            description = description,
            vendor = vendor,
            price = price,
            date = date, // ISO 8601 format
            address = address,
            longitude = longitude!!,
            latitude = latitude!!
        )

        val response = RetrofitInstance.apiService.addDeal(deal)
        if (response.isSuccessful) {
            Log.d("Response", "Add Deal Response: ${response.body()}")
            true
        } else {
            Log.d("Response", "Api request to add deal failed: ${response.body()}")
            false
        }
    } catch (e: Exception) {
        Log.d("Response", "Exception when adding deal: ${e.message}")
        false
    }
}

suspend fun updateDeal(id: String,
                       name : String,
                       description : String,
                       vendor : String,
                       price : Double,
                       date : String, // ISO 8601 format
                       address : String,
                       longitude : Double?,
                       latitude : Double?
) : Boolean {
    if (listOf(name, description, vendor, date, address).any { it.isInvalid() }
        || price.isInvalid() || longitude.isInvalid() || latitude.isInvalid()) {
        return false
    }
    if (!longitude.isInvalid() && !latitude.isInvalid()) {
        if (abs(longitude!!) > 180 || abs(latitude!!) > 90)
            return false
    }
    return try {
        val deal = DealCreationRequest (
            name = name,
            description = description,
            vendor = vendor,
            price = price,
            date = date,
            address = address,
            longitude = longitude!!,
            latitude = latitude!!
        )
        val response =
            RetrofitInstance.apiService.updateDeal(id, deal)
        if (response.isSuccessful) {
            Log.d("Response", "Edit Deal Response: $response")
            true
        } else {
            Log.d("Response", "Api request to edit deal failed: ${response.body()}")
            false
        }
    } catch (e: Exception) {
        Log.d("Response", "Exception when updating deal: ${e.message}")
        false
    }
}

suspend fun getDeal(id: String) : DealRetrievalResponse? {
    return try {
        val response = RetrofitInstance.apiService.getSpecificDeal(id)
        Log.d("Response", "Fetch Deal API Request actually called")
        if (response.isSuccessful) {
            response.body().also {
                Log.d("Response", "Deals Response: $it")
            }
        } else {
            Log.d("Error", "Deals API Response Was Unsuccessful: $response")
            null
        }
    } catch (e: Exception) {
        Log.d("Error", "Error Calling Deals API: $e")
        null
    }
}