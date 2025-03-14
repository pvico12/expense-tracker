package com.cs446.expensetracker.ui

import android.app.DatePickerDialog
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
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
import com.cs446.expensetracker.mockData.Deal
import com.cs446.expensetracker.mockData.mockDeals
import com.cs446.expensetracker.mockData.mock_deal_json
import com.cs446.expensetracker.api.models.Category
import com.cs446.expensetracker.api.models.DealCreationRequest
import com.cs446.expensetracker.api.models.Transaction
import com.cs446.expensetracker.session.UserSession
import com.cs446.expensetracker.ui.ui.theme.Typography
import com.cs446.expensetracker.ui.ui.theme.mainTextColor
import com.cs446.expensetracker.ui.ui.theme.secondTextColor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDealScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var itemName by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var goBack by remember { mutableStateOf(false) }

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
            selectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Submit a Deal",
                color = mainTextColor,
                style = Typography.titleLarge,
                modifier = Modifier
                    .fillMaxWidth()
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
            value = location,
            onValueChange = { location = it },
            label = { Text("Location") },
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
            Text(text = selectedDate)
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("Address") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = secondTextColor,
                unfocusedIndicatorColor = mainTextColor)
        )

        Spacer(modifier = Modifier.height(20.dp))

//        // Error Message Display
//        if (errorMessage != null) {
//            Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
//            Spacer(modifier = Modifier.height(10.dp))
//        }

        // Save Expense Button
        Button(
            onClick = {
                CoroutineScope(Dispatchers.IO).launch {
                    isLoading = true
                    errorMessage = null
                    try {
//                        val amount = expenseAmount.toDoubleOrNull()
//                        if (amount == null || selectedCategory == null) {
//                            errorMessage = "Please fill in all fields correctly."
//                            return@launch
//                        }

                        val deal = DealCreationRequest (
                            name = itemName,
                            description = "",
                            price = price.toDouble(),
                            date = selectedDate, // ISO 8601 format
                            address = address,
                            longitude = 0.0,
                            latitude = 0.0
                        )

                        val token = UserSession.access_token ?: ""
                        val response =
                            RetrofitInstance.apiService.addDeal(deal)
                        if (response.isSuccessful) {
                            goBack = true
                            Log.d("Response", "Add Deal Response: ${response}")
                        } else {
                            errorMessage = "Failed to add deal. Please try again."
                        }
                    } catch (e: Exception) {
                        errorMessage = "Error: ${e.message}"
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
                Text(text = "Save Transaction")
            }
        }
        if (goBack) {
            goBack = false
            Toast.makeText(
                context,
                "Deal added successfully!",
                Toast.LENGTH_SHORT
            ).show()
            navController.popBackStack()
        }
    }
}
