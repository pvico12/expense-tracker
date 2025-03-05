package com.cs446.expensetracker.nav

import com.cs446.expensetracker.ui.AddExpenseScreen
import android.content.Context
import android.content.Intent
import android.icu.text.DecimalFormat
import android.location.Address
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
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
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat.startActivity
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cs446.expensetracker.mockData.Deal
import com.cs446.expensetracker.mockData.*
import com.cs446.expensetracker.mockData.mockTransactions
import com.cs446.expensetracker.ui.AddDealScreen
import com.cs446.expensetracker.ui.ui.theme.*
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import java.text.SimpleDateFormat
import java.util.*

class DealsContainer {


    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @Composable
    fun DealsNavHost() {
        val dealsNavController = rememberNavController()
        val scrollState = rememberScrollState()

        val atasehir = LatLng(43.452969, -80.495064)
        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(atasehir, 15f)
        }


        var uiSettings = remember {
            mutableStateOf(MapUiSettings(zoomControlsEnabled = true))
        }

        var changedData = remember {
            mutableStateOf(mock_deal_json.mock_deals)
        }

        NavHost(dealsNavController, startDestination = "deals") {
            composable("deals") {
                changedData.value = mock_deal_json.mock_deals
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
                    DealHistoryScreen(changedData)
                }
            }
            composable("addDealScreen") {
                AddDealScreen(navController = dealsNavController)
            }
        }
    }


    @Composable
    fun DealHistoryScreen(changedData: MutableState<List<Deal>>) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(0.dp)
        ) {
            Spacer(modifier = Modifier.height(2.dp))

            for(deal in changedData.value) {
                DealListItem(deal)
            }

        }
    }


    @Composable
    fun DealListItem(deal: Deal) {
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
                        Text(text = "${deal.item}", fontWeight = FontWeight.Bold, color= mainTextColor, style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = "$${format.format(deal.cost)}",
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
                        Text(text = "${deal.location}", fontWeight = FontWeight.Bold, color= secondTextColor, modifier = Modifier.padding(end=14.dp), style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.weight(2f))
                        Text(
                            text = deal.address,
                            style = MaterialTheme.typography.titleMedium,
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
                                .defaultMinSize(minWidth = 1.dp, minHeight = 8.dp),
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
                            onClick = { /* ... */ },
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
                            color = MaterialTheme.colorScheme.secondary,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.padding(top=8.dp),
                        )
                        TextButton(
                            onClick = { /* ... */ },
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
                            color = MaterialTheme.colorScheme.secondary,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.padding(top=8.dp),
                        )
                    }
                }
            }
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
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = inputFormat.parse(isoDate)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            "Invalid Date"
        }
    }
}
