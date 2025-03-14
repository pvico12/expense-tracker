package com.cs446.expensetracker.dashboard

import android.content.Intent
import android.icu.text.DecimalFormat
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.result.launch
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.cs446.expensetracker.api.RetrofitInstance
import com.cs446.expensetracker.api.models.DealLocation
import com.cs446.expensetracker.api.models.DealRetrievalRequest
import com.cs446.expensetracker.api.models.DealRetrievalResponse
import com.cs446.expensetracker.mockData.mock_deal_json
import com.cs446.expensetracker.session.UserSession
import com.cs446.expensetracker.ui.ui.theme.*
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

class Deals {


    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @Composable
    fun DealsHost(dealsNavController: NavController) {
        val scrollState = rememberScrollState()

        val atasehir = LatLng(43.452969, -80.495064)
        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(atasehir, 15f)
        }


        var uiSettings = remember {
            mutableStateOf(MapUiSettings(zoomControlsEnabled = true))
        }

        var listOfDeals by remember { mutableStateOf<List<DealRetrievalResponse>>(emptyList()) }

        var listOfUpvotes by remember { mutableStateOf<List<Pair<Int, Int>>>(emptyList()) }

        var errorMessage = ""
        var isLoading by remember { mutableStateOf(true) }

        val deal_request = DealRetrievalRequest(
            user_id = UserSession.userId,
            location = DealLocation (
                longitude = -80.495064,
                latitude = 43.452969,
                distance = 100.0,
            )
        )

        // TODO: FOR UPVOTE AND DOWNVOTE FUNCTIONS, TELL IF THEY'VE BEEN UPVOTED ON INITIAL API
        fun onUpvote(deal_id: Int, index: Int) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val token = UserSession.access_token ?: ""
                    val response: Response<String> =
                        RetrofitInstance.apiService.upvoteDeal(deal_id.toString())
                    if (response.isSuccessful) {
                        val responseBody = response.body()
                        Log.d("Response", "Upvote Deals Response: $responseBody")
                        var second_half = emptyList<Pair<Int, Int>>()
                        if (index != listOfUpvotes.size-1) {
                            second_half = listOfUpvotes.slice(index+1..listOfUpvotes.size-1)
                        }
                        if (listOfUpvotes[index] == Pair(1,0)) {
                            listOfUpvotes = listOfUpvotes.slice(0..<index) + Pair(0,0) + second_half
                        } else {
                            val size = listOfUpvotes.size
                            listOfUpvotes = listOfUpvotes.slice(0..<index) + Pair(1,0) + second_half
                        }
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
                    val response: Response<String> =
                        RetrofitInstance.apiService.downvoteDeal(deal_id.toString())
                    if (response.isSuccessful) {
                        val responseBody = response.body()
                        Log.d("Response", "Downvote Deals Response: $responseBody")
                        var second_half = emptyList<Pair<Int, Int>>()
                        if (index != listOfUpvotes.size-1) {
                            second_half = listOfUpvotes.slice(index+1..listOfUpvotes.size-1)
                        }
                        if (listOfUpvotes[index] == Pair(0,1)) {
                            val size = listOfUpvotes.size
                            listOfUpvotes = listOfUpvotes.slice(0..<index) + Pair(0,0) + second_half
                        } else {
                            val size = listOfUpvotes.size
                            listOfUpvotes = listOfUpvotes.slice(0..<index) + Pair(0,1) + second_half
                        }
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

        @Composable
        fun DealListItem(deal: DealRetrievalResponse, index: Int) {
            var googleMapsOpened = remember {
                mutableStateOf("")
            }
            val format = DecimalFormat("#,###.00")
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
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
                            Text(text = "vendor", fontWeight = FontWeight.Bold, color= secondTextColor, modifier = Modifier.padding(end=14.dp), style = MaterialTheme.typography.titleLarge)
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
                                text = "${deal.upvotes + listOfUpvotes[index].first}",
                                style = Typography.titleSmall,
                                color = if (listOfUpvotes[index].first == 1) Color(0xFF69A42D) else MaterialTheme.colorScheme.secondary,
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
                                text = "${deal.downvotes + listOfUpvotes[index].second}",
                                style = Typography.titleSmall,
                                color = if (listOfUpvotes[index].second == 1) Color(0xFFD5030A) else MaterialTheme.colorScheme.secondary,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.padding(top=8.dp),
                            )
                        }
                    }
                }
            }
        }

        @Composable
        fun DealHistoryScreen(changedData: List<DealRetrievalResponse>) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(0.dp)
            ) {
                Spacer(modifier = Modifier.height(2.dp))

                for((i, deal) in changedData.withIndex()) {
                    DealListItem(deal, i)
                }

            }
        }

        @Composable
        fun apiFetchDeals() {
            // Load deals via API
            LaunchedEffect(Unit) {
                isLoading = true
                errorMessage = ""
                try {
                    val token = UserSession.access_token ?: ""
                    val response: Response<List<DealRetrievalResponse>> =
                        RetrofitInstance.apiService.getDeals(deal_request)
                    if (response.isSuccessful) {
                        val responseBody = response.body()
                        Log.d("Response", "Deals Response: $responseBody")
                        listOfDeals = responseBody?.map { x ->
                            DealRetrievalResponse(
                                id = x.id,
                                name = x.name,
                                description = x.description,
                                price = x.price,
                                date = x.date,
                                address = x.address,
                                longitude = x.longitude,
                                latitude = x.latitude,
                                upvotes = x.upvotes,
                                downvotes = x.downvotes
                            )
                        } ?: emptyList()
                        for((i, deal) in listOfDeals.withIndex()) {
                            listOfUpvotes = listOfUpvotes + Pair(0, 0)
                        }
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
        apiFetchDeals()

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
            Text(
                text = "Your Region is set to \n${mock_deal_json.current_location}",
                textAlign = TextAlign.Center,
                color = mainTextColor,
                style = Typography.titleMedium,
                modifier = Modifier
                    .padding(start = 16.dp, top = 16.dp)
            )
            TextButton(onClick = { var x = 1 },
                modifier = Modifier
                    .defaultMinSize(minWidth = 1.dp, minHeight = 8.dp),
                contentPadding = PaddingValues(0.dp),
            ) {
                Text(text = "Change Location", color = pieChartColor1, textDecoration = TextDecoration.Underline, style = MaterialTheme.typography.titleMedium)
            }
            GoogleMap(
                modifier = Modifier
                    .height(180.dp)
                    .fillMaxWidth(),
                cameraPositionState = cameraPositionState,
                uiSettings = uiSettings.value
            ) {
                Marker(
                    state = MarkerState(position = atasehir),
                    title = "One Marker"
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Button(
                modifier = Modifier
                    .padding(0.dp),
                onClick = { dealsNavController.navigate("addDealScreen")  },
                colors = ButtonDefaults.buttonColors(containerColor = mainTextColor)
            ) {
                Text(text = "Submit a Deal", modifier = Modifier.padding(0.dp))
            }
            DealHistoryScreen(listOfDeals)
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
            val time = splitted[1].split(":")
            splitted[0] + " , " + time[0] + ":" + time[1]
        } catch (e: Exception) {
            Log.d("Error", "Error Parsing Date: $e")
            "Invalid Date"
        }
    }


}
