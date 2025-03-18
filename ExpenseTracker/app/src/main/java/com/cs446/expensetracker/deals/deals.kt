package com.cs446.expensetracker.dashboard

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.icu.text.DecimalFormat
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.AddToPhotos
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.cs446.expensetracker.api.RetrofitInstance
import com.cs446.expensetracker.api.models.DealLocation
import com.cs446.expensetracker.api.models.DealRetrievalRequestWithLocation
import com.cs446.expensetracker.api.models.DealRetrievalRequestWithUser
import com.cs446.expensetracker.api.models.DealRetrievalResponse
import com.cs446.expensetracker.deals.AutoComplete
import com.cs446.expensetracker.session.UserSession
import com.cs446.expensetracker.ui.ui.theme.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.compose.material3.AlertDialog
import kotlinx.coroutines.withContext

class Deals {

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @Composable
    fun DealsHost(dealsNavController: NavController) {
        val scrollState = rememberScrollState()

        val atasehir = LatLng(43.452969, -80.495064)
        var currentAddress = rememberSaveable  { mutableStateOf("")}
        var currentLatLng = rememberSaveable  { mutableStateOf<LatLng?>(null)}
        var defaultZoom = rememberSaveable  { mutableStateOf(0f)}
        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom((currentLatLng.value ?: atasehir) as LatLng, defaultZoom.value)
        }

        var uiSettings = remember {
            mutableStateOf(MapUiSettings(zoomControlsEnabled = true))
        }

        var listOfDeals by rememberSaveable  { mutableStateOf<List<DealRetrievalResponse>>(emptyList()) }

        var viewingUserSubmittedDeals by rememberSaveable { mutableStateOf("See Your Submitted Deals") }

        var errorMessage = ""
        var isLoading by remember { mutableStateOf(true) }

        var viewLocationPicker by remember { mutableStateOf(false) }

        // Search bar states
        var searchQuery by remember { mutableStateOf("") }
        var startDate by remember { mutableStateOf<Date?>(null) }
        var endDate by remember { mutableStateOf<Date?>(null) }
        // Filter transactions based on search query and date range.
        var filteredTransactions by remember { mutableStateOf<List<DealRetrievalResponse>>(emptyList())}

        var errorMessageForRegion by remember { mutableStateOf<String>("")}

        var deleteConfirmationDialogue by remember { mutableStateOf(false)}
        var idToDelete by remember { mutableStateOf(-1)}


        fun onEditButtonClick(id: Int) {
            dealsNavController.navigate("addDealScreen/$id")
        }
        fun onDeleteButtonClick(id: Int) {
            idToDelete = id
            deleteConfirmationDialogue = true
        }
        fun onChangeLocationClick() {
            viewLocationPicker = true

        }

        fun apiFetchDeals(newLatLng: LatLng?) {
            viewingUserSubmittedDeals = "See User Submitted Deals"
            // Load deals via API
            Log.d("Response", "Api fetch was called, but request not necessarily sent")
            if (newLatLng != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    isLoading = true
                    errorMessage = ""
                    val deal_request = DealRetrievalRequestWithLocation(
                        location = DealLocation (
                            longitude = newLatLng.longitude,
                            latitude = newLatLng.latitude,
                            distance = 100.0,
                        )
                    )
                    try {
                        val token = UserSession.access_token ?: ""
                        val response: Response<List<DealRetrievalResponse>> =
                            RetrofitInstance.apiService.getDeals(deal_request)
                        Log.d("Response", "Fetch Deals API Request actually called")
                        if (response.isSuccessful) {
                            val responseBody = response.body()
                            Log.d("Response", "Deals Response: $responseBody")
                            var unsorteddeals: List<DealRetrievalResponse>
                            unsorteddeals = responseBody?.map { x ->
                                DealRetrievalResponse(
                                    id = x.id,
                                    name = x.name,
                                    description = x.description,
                                    vendor = x.vendor,
                                    price = x.price,
                                    date = x.date,
                                    address = x.address,
                                    longitude = x.longitude,
                                    latitude = x.latitude,
                                    upvotes = x.upvotes,
                                    downvotes = x.downvotes,
                                    user_vote = x.user_vote,
                                    maps_link = x.maps_link
                                )
                            } ?: emptyList()
                            listOfDeals = unsorteddeals.sortedByDescending { it.upvotes - it.downvotes }
                        } else {
                            errorMessage = "Failed to load data."
                            Log.d("Error", "Deals API Response Was Unsuccessful: $response")
                        }
                    } catch (e: Exception) {
                        errorMessage = "Error: ${e.message}"
                        Log.d("Error", "Error Calling Deals API: $errorMessage")
                    } finally {
                        isLoading = false
                    }
                }
            }
        }
        fun apiFetchUserSubmittedDeals() {
            // Load deals via API
            Log.d("Response", "Api fetch was called, but request not necessarily sent")
            viewingUserSubmittedDeals = "See All Deals in Area"
            CoroutineScope(Dispatchers.IO).launch {
                isLoading = true
                errorMessage = ""
                val deal_request = DealRetrievalRequestWithUser(
                    user_id = UserSession.userId,
                )
                try {
                    val token = UserSession.access_token ?: ""
                    val response: Response<List<DealRetrievalResponse>> =
                        RetrofitInstance.apiService.getDeals(deal_request)
                    Log.d("Response", "Fetch User Submitted Deals API Request actually called")
                    if (response.isSuccessful) {
                        val responseBody = response.body()
                        Log.d("Response", "User Submitted Deals Response: $responseBody")
                        var unsorteddeals: List<DealRetrievalResponse>
                        unsorteddeals = responseBody?.map { x ->
                            DealRetrievalResponse(
                                id = x.id,
                                name = x.name,
                                description = x.description,
                                vendor = x.vendor,
                                price = x.price,
                                date = x.date,
                                address = x.address,
                                longitude = x.longitude,
                                latitude = x.latitude,
                                upvotes = x.upvotes,
                                downvotes = x.downvotes,
                                user_vote = x.user_vote,
                                maps_link = x.maps_link
                            )
                        } ?: emptyList()
                        listOfDeals = unsorteddeals.sortedByDescending { it.upvotes - it.downvotes }
                    } else {
                        errorMessage = "Failed to load data."
                        Log.d("Error", "User Submitted Deals API Response Was Unsuccessful: $response")
                    }
                } catch (e: Exception) {
                    errorMessage = "Error: ${e.message}"
                    Log.d("Error", "Error Calling User Submitted Deals API: $errorMessage")
                } finally {
                    isLoading = false
                }
            }
        }

        fun onUpvote(deal_id: Int, index: Int) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val token = UserSession.access_token ?: ""
                    val response: Response<String>
                    if(listOfDeals[index].user_vote == 1) {
                        response = RetrofitInstance.apiService.cancelvoteDeal(deal_id.toString())
                    } else {
                        response = RetrofitInstance.apiService.upvoteDeal(deal_id.toString())
                    }
                    if (response.isSuccessful) {
                        val responseBody = response.body()
                        Log.d("Response", "Upvote Deals Response: $responseBody")
                        var second_half = emptyList<DealRetrievalResponse>()
                        if (index != listOfDeals.size-1) {
                            second_half = listOfDeals.slice(index+1..listOfDeals.size-1)
                        }
                        listOfDeals = listOfDeals + listOfDeals[index] // I don't know why this makes the refresh work but please don't remove
                        var updatedDealDownvote: DealRetrievalResponse = listOfDeals[index]
                        if (listOfDeals[index].user_vote == 1) {
                            updatedDealDownvote.user_vote = 0
                            updatedDealDownvote.upvotes -= 1
                            listOfDeals = listOfDeals.slice(0..<index) + updatedDealDownvote + second_half
                        } else if (listOfDeals[index].user_vote == 0) {
                            updatedDealDownvote.user_vote = 1
                            updatedDealDownvote.upvotes += 1
                            listOfDeals = listOfDeals.slice(0..<index) + updatedDealDownvote + second_half
                        } else if (listOfDeals[index].user_vote == -1) {
                            updatedDealDownvote.user_vote = 1
                            updatedDealDownvote.upvotes += 1
                            updatedDealDownvote.downvotes -= 1
                            listOfDeals = listOfDeals.slice(0..<index) + updatedDealDownvote + second_half
                        }
                        Log.d("Response", "New Upvoted Deals: $listOfDeals")
                    } else {
                        errorMessage = "Failed to Upvote"
                        Log.d("Error", "Failed to Upvote $errorMessage")
                    }
                } catch (e: Exception) {
                    errorMessage = "Error: ${e.message}"
                    Log.d("Error", "Failed to Upvote $errorMessage")
                }
            }
        }
        fun onDownvote(deal_id: Int, index: Int) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val token = UserSession.access_token ?: ""
                    val response: Response<String>
                    if(listOfDeals[index].user_vote == -1) {
                        response = RetrofitInstance.apiService.cancelvoteDeal(deal_id.toString())
                    } else {
                        response = RetrofitInstance.apiService.downvoteDeal(deal_id.toString())
                    }
                    if (response.isSuccessful) {
                        val responseBody = response.body()
                        Log.d("Response", "Downvote Deals Response: $responseBody")
                        var second_half = emptyList<DealRetrievalResponse>()
                        if (index != listOfDeals.size-1) {
                            second_half = listOfDeals.slice(index+1..listOfDeals.size-1)
                        }
                        listOfDeals = listOfDeals + listOfDeals[index] // I don't know why this makes the refresh work but please don't remove
                        var updatedDealDownvote: DealRetrievalResponse = listOfDeals[index]
                        if (listOfDeals[index].user_vote == -1) {
                            updatedDealDownvote.user_vote = 0
                            updatedDealDownvote.downvotes -= 1
                            listOfDeals = listOfDeals.slice(0..<index) + updatedDealDownvote + second_half
                        } else if (listOfDeals[index].user_vote == 0) {
                            updatedDealDownvote.user_vote = -1
                            updatedDealDownvote.downvotes += 1
                            listOfDeals = listOfDeals.slice(0..<index) + updatedDealDownvote + second_half
                        } else if (listOfDeals[index].user_vote == 1) {
                            updatedDealDownvote.user_vote = -1
                            updatedDealDownvote.downvotes += 1
                            updatedDealDownvote.upvotes -= 1
                            listOfDeals = listOfDeals.slice(0..<index) + updatedDealDownvote + second_half
                        }
                        Log.d("Response", "New Downvoted Deals: $listOfDeals")
                    } else {
                        errorMessage = "Failed to Upvote"
                        Log.d("Error", "Failed to Downvote $response")
                    }
                } catch (e: Exception) {
                    errorMessage = "Error: ${e.message}"
                    Log.d("Error", "Failed to Downvote $errorMessage")
                }
            }
        }

        fun onConfirm(id: Int, context: Context) {
            deleteConfirmationDialogue = false
            CoroutineScope(Dispatchers.IO).launch {
                isLoading = true
                try {
                    val token = UserSession.access_token ?: ""
                    val response: Response<String> =
                        RetrofitInstance.apiService.deleteDeal(id.toString())
                    Log.d("Response", "Fetch Deals API Request actually called")
                    if (response.isSuccessful) {
                        val responseBody = response.body()
                        Log.d("Response", "Deals Response: $responseBody")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                "Deal Deleted",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        Log.d("Error", "Deals API Response Was Unsuccessful: $response")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                "Failed to Delete Deal, Please Try Again",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    apiFetchUserSubmittedDeals()
                } catch (e: Exception) {
                    Log.d("Error", "Error Calling Deals API: $e")
                } finally {
                    isLoading = false
                }
            }
        }

        if (deleteConfirmationDialogue) {
            AlertDialog(
                onDismissRequest = { deleteConfirmationDialogue = false },
                title = { Text("Are you sure?") },
                text = { Text("Do you really want to delete?") },
                confirmButton = {
                    val context = LocalContext.current
                    TextButton(onClick = { onConfirm(idToDelete, context) }) {
                        Text("Proceed")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { deleteConfirmationDialogue = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        @Composable
        fun DealListItem(deal: DealRetrievalResponse, index: Int) {
            var googleMapsOpened = remember {
                mutableStateOf("")
            }
            val format = DecimalFormat("#,###.00")
            Log.d("Response", "Refreshed Cards")
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = tileColor,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                if (viewingUserSubmittedDeals == "See All Deals in Area") {
                    Row(modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(
                            onClick = { onDeleteButtonClick(deal.id) },
                            shape = CircleShape,
                            modifier = Modifier.size(30.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "delete",
                                modifier = Modifier.size(20.dp),
                                tint = Color(0xFF9D9D9D)
                            )
                        }
                        TextButton(
                            onClick = { onEditButtonClick(deal.id) },
                            shape = CircleShape,
                            modifier = Modifier.size(30.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "edit",
                                modifier = Modifier.size(20.dp),
                                tint = Color(0xFF9D9D9D)
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = deal.name, fontWeight = FontWeight.Bold, color= mainTextColor, style = MaterialTheme.typography.titleLarge)
                            Text(
                                text = "$${format.format(deal.price)}",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge,
                                color = mainTextColor
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(text = deal.vendor, fontWeight = FontWeight.Bold, color= secondTextColor, modifier = Modifier.padding(end=14.dp), style = MaterialTheme.typography.titleLarge)
                            Spacer(modifier = Modifier.weight(2f))
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = deal.address,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.padding(top=2.dp)
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = deal.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.padding(top=3.dp)
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            TextButton(onClick = { googleMapsOpened.value = deal.address },
                                modifier = Modifier
                                    .defaultMinSize(minWidth = 1.dp, minHeight = 2.dp),
                                contentPadding = PaddingValues(0.dp),
                            ) {
                                Text(text = "Open Google Maps", color = pieChartColor1, textDecoration = TextDecoration.Underline, style = MaterialTheme.typography.titleMedium)
                            }
                            if (googleMapsOpened.value != "") {
                                googleMapsOpened.value = ""
                                openGoogleMaps(deal.address)
                            }
                            Text(
                                text = formatTransactionDate(deal.date), // format date
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(top=12.dp)
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(0.dp),
                        ) {
                            TextButton(
                                onClick = { onUpvote(deal.id, index) },
                                shape = CircleShape,
                                modifier = Modifier.size(40.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.KeyboardArrowUp,
                                    contentDescription = "Favorite",
                                    modifier = Modifier.size(40.dp),
                                    tint = Color(0xFF69A42D)
                                )
                            }
                            Text(
                                text = "${deal.upvotes}",
                                style = Typography.titleSmall,
                                color = if (deal.user_vote == 1) Color(0xFF69A42D) else MaterialTheme.colorScheme.secondary,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.padding(top=8.dp),
                            )
                            TextButton(
                                onClick = { onDownvote(deal.id, index) },
                                shape = CircleShape,
                                modifier = Modifier.size(40.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.KeyboardArrowDown,
                                    contentDescription = "Favorite",
                                    modifier = Modifier.size(40.dp),
                                    tint = Color(0xFFD5030A)
                                )
                            }
                            Text(
                                text = "${deal.downvotes}",
                                style = Typography.titleSmall,
                                color = if (deal.user_vote == -1) Color(0xFFD5030A) else MaterialTheme.colorScheme.secondary,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.padding(top=8.dp),
                            )
                        }
                    }
                }
            }
        }

        @Composable
        fun DealHistoryScreen(changedData: List<DealRetrievalResponse>, listOfDeals: List<DealRetrievalResponse>) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(0.dp)
            ) {
                Spacer(modifier = Modifier.height(2.dp))

                for((i, deal) in listOfDeals.withIndex()) {
                    if (deal in changedData) {
                        DealListItem(deal, i)
                    }
                }

            }
        }
        LaunchedEffect(currentLatLng.value) { apiFetchDeals(currentLatLng.value) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(mainBackgroundColor)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Community Deals",
                color = mainTextColor,
                style = Typography.titleLarge,
                modifier = Modifier
                    .padding(start = 16.dp, top = 16.dp)
                    .fillMaxWidth()
            )
            if(currentLatLng.value != null) {
                Text(
                    text = "Your region is set to \n${currentAddress.value}",
                    textAlign = TextAlign.Center,
                    lineHeight = 35.sp,
                    color = mainTextColor,
                    style = Typography.titleMedium,
                    modifier = Modifier
                        .padding(start = 16.dp, top = 16.dp)
                )
            } else {
                Text(
                    text = "Please set a region",
                    textAlign = TextAlign.Center,
                    color = mainTextColor,
                    style = Typography.titleMedium,
                    modifier = Modifier
                        .padding(start = 16.dp, top = 16.dp)
                )
            }
            if(viewLocationPicker) {
                Row(modifier = Modifier.padding(start = 24.dp, bottom = 6.dp, end = 24.dp)) {
                    AutoComplete("") {
                        CoroutineScope(Dispatchers.IO).launch {
                            currentAddress.value = it.address
                            currentLatLng.value = it.latLng
                            viewLocationPicker = false
                            defaultZoom.value = 15f
                        }
                    }
                }
            } else {
                TextButton(onClick = { onChangeLocationClick() },
                    modifier = Modifier
                        .defaultMinSize(minWidth = 1.dp, minHeight = 8.dp),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Text(text = "Change Location", color = pieChartColor1, textDecoration = TextDecoration.Underline, style = MaterialTheme.typography.titleMedium)
                }
            }
            LaunchedEffect(currentLatLng.value) {
                currentLatLng.value?.let { latLng ->
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngZoom(latLng, 1f),
                        durationMs = 1000 // Optional animation duration
                    )
                }
            }
            GoogleMap(
                modifier = Modifier
                    .height(180.dp)
                    .fillMaxWidth(),
                cameraPositionState = cameraPositionState,
                uiSettings = uiSettings.value
            ) {
                if (defaultZoom.value != 0f) {
                    Marker(
                        state = MarkerState(position = (currentLatLng.value ?: atasehir) as LatLng),
                        title = "One Marker"
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            // row below map
            Row(modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp),
                    contentPadding = PaddingValues(0.dp),
                    onClick = {
                        if (currentLatLng.value == null) {
                            errorMessageForRegion = "Please set a region first"
                        } else {
                            errorMessageForRegion = ""
                            dealsNavController.navigate("addDealScreen/${-1}")
                        }
                              },
                ) {
                    Icon(
                        imageVector = Icons.Filled.AddToPhotos,
                        contentDescription = "Add New Deal",
                        modifier = Modifier.size(40.dp),
                        tint = mainTextColor
                    )
                }
                Button(
                    modifier = Modifier
                        .padding(0.dp),
                    onClick = {
                        if (currentLatLng.value == null) {
                            errorMessageForRegion = "Please set a region first"
                        } else {
                            if (viewingUserSubmittedDeals == "See All Deals in Area") {
                                viewingUserSubmittedDeals = "See User Submitted Deals"
                            } else {
                                errorMessageForRegion = ""
                                viewingUserSubmittedDeals = "See All Deals in Area"
                            }
                        }
                              },
                    colors = ButtonDefaults.buttonColors(containerColor = mainTextColor)
                ) {
                    Text(text = viewingUserSubmittedDeals, modifier = Modifier.padding(0.dp))
                }
                TextButton(
                    onClick = {
                        if (currentLatLng.value == null) {
                            errorMessageForRegion = "Please set a region first"
                        } else {
                            errorMessageForRegion = ""
                            currentLatLng.value = currentLatLng.value?.let { LatLng(it.latitude, it.longitude) }
                            apiFetchDeals(currentLatLng.value)
                        }
                    },
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Autorenew,
                        contentDescription = "Reload",
                        modifier = Modifier.size(40.dp),
                        tint = mainTextColor
                    )
                }
            }
            LaunchedEffect(viewingUserSubmittedDeals) {
                if (viewingUserSubmittedDeals == "See All Deals in Area") {
                    apiFetchUserSubmittedDeals()
                } else {
                    apiFetchDeals(currentLatLng.value)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            if (errorMessageForRegion != "" && currentLatLng.value == null) {
                Text(text = errorMessageForRegion, color = MaterialTheme.colorScheme.error)
            }
            DealSearchBar(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                startDate = startDate,
                onStartDateChange = { startDate = it },
                endDate = endDate,
                onEndDateChange = { endDate = it }
            )
            LaunchedEffect(currentLatLng.value) {
                currentLatLng.value?.let { latLng ->
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngZoom(latLng, 15f),
                        durationMs = 1000 // Optional animation duration
                    )
                }
            }
            LaunchedEffect(listOfDeals) {
                filteredTransactions = listOfDeals.filter { deal ->
                    val matchesQuery = if (searchQuery.isNotBlank())
                        deal.name.contains(searchQuery, ignoreCase = true)
                    else true
                    matchesQuery
                }
            }
            LaunchedEffect(searchQuery) {
                filteredTransactions = listOfDeals.filter { deal ->
                    val matchesQuery = if (searchQuery.isNotBlank())
                        deal.name.contains(searchQuery, ignoreCase = true)
                    else true
                    matchesQuery
                }
            }
            DealHistoryScreen(filteredTransactions, listOfDeals)
        }

    }

    @Composable
    fun DealSearchBar(
        searchQuery: String,
        onSearchQueryChange: (String) -> Unit,
        startDate: Date?,
        onStartDateChange: (Date?) -> Unit,
        endDate: Date?,
        onEndDateChange: (Date?) -> Unit
    ) {
        val context = LocalContext.current

        // ----- Start Date Picker Implementation -----
        val startCalendar = Calendar.getInstance()
        var selectedStartDate by remember {
            mutableStateOf(
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(startCalendar.time)
            )
        }
        val startDatePickerDialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                selectedStartDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                onStartDateChange(sdf.parse(selectedStartDate))
            },
            startCalendar.get(Calendar.YEAR),
            startCalendar.get(Calendar.MONTH),
            startCalendar.get(Calendar.DAY_OF_MONTH)
        )

        // ----- End Date Picker Implementation -----
        val endCalendar = Calendar.getInstance()
        var selectedEndDate by remember {
            mutableStateOf(
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(endCalendar.time)
            )
        }
        val endDatePickerDialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                selectedEndDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                onEndDateChange(sdf.parse(selectedEndDate))
            },
            endCalendar.get(Calendar.YEAR),
            endCalendar.get(Calendar.MONTH),
            endCalendar.get(Calendar.DAY_OF_MONTH)
        )

        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp)) {
            // Text field for note search.
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                label = { Text("Search...") },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = secondTextColor,
                    unfocusedIndicatorColor = mainTextColor
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Start and End Date Pickers (UI commented out)
        }
    }

    @Composable
    fun openGoogleMaps(address: String)  {
        val context = LocalContext.current
        val intentUri = Uri.parse("geo:0,0?q=${address}")
        val mapIntent = Intent(Intent.ACTION_VIEW, intentUri)
        mapIntent.setPackage("com.google.android.apps.maps")
        context.startActivity(mapIntent)
    }

    // Helper to format date
    private fun formatTransactionDate(isoDate: String): String {
        val splitted = isoDate.split("T")
        return try {
            splitted[0]
        } catch (e: Exception) {
            Log.d("Error", "Error Parsing Date: $e")
            "Invalid Date"
        }
    }


}
