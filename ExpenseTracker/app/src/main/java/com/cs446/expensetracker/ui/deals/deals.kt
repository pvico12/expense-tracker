package com.cs446.expensetracker.ui.deals

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.icu.text.DecimalFormat
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.MotionEvent
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabPosition
import androidx.compose.material3.TabRow
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInteropFilter
import com.cs446.expensetracker.api.models.DealSubRetrievalResponse
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import kotlinx.coroutines.withContext

class Deals {

    @OptIn(ExperimentalComposeUiApi::class)
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @Composable
    fun DealsHost(dealsNavController: NavController) {
        val context = LocalContext.current
        val scrollState = rememberScrollState()
        val scope = rememberCoroutineScope()

        val atasehir = LatLng(43.452969, -80.495064)
        val currentAddress = rememberSaveable  { mutableStateOf("")}
        val currentLatLng = rememberSaveable  { mutableStateOf<LatLng?>(null)}
        val defaultZoom = rememberSaveable  { mutableFloatStateOf(0f) }
        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom((currentLatLng.value ?: atasehir) as LatLng, defaultZoom.floatValue)
        }

        val uiSettings = remember {
            mutableStateOf(MapUiSettings(zoomControlsEnabled = true))
        }

        var listOfSubs by remember  { mutableStateOf<List<DealSubRetrievalResponse>>(emptyList()) }
        var listOfDeals by remember  { mutableStateOf<List<DealRetrievalResponse>>(emptyList()) }

        var viewingUserSubmittedDeals by rememberSaveable { mutableStateOf("See Your Submitted Deals") }

        var errorMessage: String
        var isLoading by remember { mutableStateOf(true) }

        var viewLocationPicker by remember { mutableStateOf(false) }

        val pagerState = rememberPagerState(pageCount = { 2 })

        // Search bar states
        var searchQuery by remember { mutableStateOf("") }
        var startDate by remember { mutableStateOf<Date?>(null) }
        var endDate by remember { mutableStateOf<Date?>(null) }
        // Filter transactions based on search query and date range.
        var filteredTransactions by remember { mutableStateOf<List<DealRetrievalResponse>>(emptyList())}

        var errorMessageForRegion by remember { mutableStateOf<String>("")}

        var deleteConfirmationDialogue by remember { mutableStateOf(false)}
        var deleteSubConfirmationDialogue by remember { mutableStateOf(false)}
        var idToDelete by remember { mutableStateOf(-1)}
        var subToDelete by remember { mutableIntStateOf(-1) }

        var showBottomSheet by remember { mutableStateOf(false) }
        var subToChange by remember { mutableIntStateOf(-1) }

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

        fun onEditSubClick(id: Int) {
            showBottomSheet = true
            subToChange = id
        }
        fun onDeleteSubClick(id: Int) {
            subToDelete = id
            deleteSubConfirmationDialogue = true
        }

        fun apiFetchSubs() {
            scope.launch {
                isLoading = true
                errorMessage = ""
                try {
                    val response: Response<List<DealSubRetrievalResponse>> =
                        RetrofitInstance.apiService.getSubs()
                    if (response.isSuccessful) {
                        val responseBody = response.body()
                        Log.d("Response", "Subs Response: $responseBody")
                        listOfSubs = responseBody?.map { x ->
                            DealSubRetrievalResponse(
                                id = x.id,
                                user_id = x.user_id,
                                address = x.address,
                                longitude = x.longitude,
                                latitude = x.latitude
                            )
                        } ?: emptyList()
                    } else {
                        Log.d("Error", "Subs API Response Was Unsuccessful: $response")
                    }
                } catch (e: Exception) {
                    errorMessage = "Error: ${e.message}"
                    Log.d("Error", "Error Calling Subs API: $errorMessage")
                } finally {
                    isLoading = false
                }
            }
        }
        fun apiFetchDeals(newLatLng: LatLng?) {
            viewingUserSubmittedDeals = "See User Submitted Deals"
            // Load deals via API
            Log.d("Response", "Api fetch was called, but request not necessarily sent")
            if (newLatLng != null) {
                scope.launch {
                    isLoading = true
                    errorMessage = ""
                    val dealRequest = DealRetrievalRequestWithLocation(
                        location = DealLocation (
                            longitude = newLatLng.longitude,
                            latitude = newLatLng.latitude,
                            distance = 100.0,
                        )
                    )
                    try {
                        val response: Response<List<DealRetrievalResponse>> =
                            RetrofitInstance.apiService.getDeals(dealRequest)
                        Log.d("Response", "Fetch Deals API Request actually called")
                        if (response.isSuccessful) {
                            val responseBody = response.body()
                            Log.d("Response", "Deals Response: $responseBody")
                            val unsorteddeals: List<DealRetrievalResponse> = responseBody?.map { x ->
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
            } else {
                listOfDeals = emptyList()
            }
        }
        fun apiFetchUserSubmittedDeals() {
            // Load deals via API
            Log.d("Response", "Api fetch was called, but request not necessarily sent")
            viewingUserSubmittedDeals = "See All Deals in Area"
            CoroutineScope(Dispatchers.IO).launch {
                isLoading = true
                errorMessage = ""
                val dealRequest = DealRetrievalRequestWithUser(
                    user_id = UserSession.userId,
                )
                try {
                    val response: Response<List<DealRetrievalResponse>> =
                        RetrofitInstance.apiService.getDeals(dealRequest)
                    Log.d("Response", "Fetch User Submitted Deals API Request actually called")
                    if (response.isSuccessful) {
                        val responseBody = response.body()
                        Log.d("Response", "User Submitted Deals Response: $responseBody")
                        val unsorteddeals: List<DealRetrievalResponse> = responseBody?.map { x ->
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
                val updatedDeals = upvote(listOfDeals, deal_id.toString(), index)
                if (updatedDeals != null) {
                    listOfDeals = updatedDeals
                } else {
                    errorMessage = "Failed to Upvote"
                }
            }
        }
        fun onDownvote(deal_id: Int, index: Int) {
            CoroutineScope(Dispatchers.IO).launch {
                val updatedDeals = downvote(listOfDeals, deal_id.toString(), index)
                if (updatedDeals != null) {
                    listOfDeals = updatedDeals
                } else {
                    errorMessage = "Failed to Downvote"
                }
            }
        }

        fun onConfirm(id: Int, context: Context) {
            deleteConfirmationDialogue = false
            CoroutineScope(Dispatchers.IO).launch {
                isLoading = true
                if (deleteDeal(id.toString())) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            "Deal Deleted",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            "Failed to Delete Deal, Please Try Again",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                isLoading = false
                apiFetchUserSubmittedDeals()
            }
        }
        fun onConfirmSub(id: Int, context: Context) {
            deleteSubConfirmationDialogue = false
            CoroutineScope(Dispatchers.IO).launch {
                isLoading = true
                if (deleteSub(id)) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            "Address Deleted",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            "Failed to Delete Address, Please Try Again",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                isLoading = false
                apiFetchSubs()
            }
        }

        @Composable
        fun openGoogleMaps(address: String)  {
            val intentUri = Uri.parse("geo:0,0?q=${address}")
            val mapIntent = Intent(Intent.ACTION_VIEW, intentUri)
//        mapIntent.setPackage("com.google.android.apps.maps")
            context.startActivity(mapIntent)
        }

        if (deleteConfirmationDialogue) {
            AlertDialog(
                onDismissRequest = { deleteConfirmationDialogue = false },
                title = { Text("Are you sure?") },
                text = { Text("Do you really want to delete?") },
                confirmButton = {
//                    val context = LocalContext.current
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

        if (deleteSubConfirmationDialogue) {
            AlertDialog(
                onDismissRequest = { deleteSubConfirmationDialogue = false },
                title = { Text("Are you sure?") },
                text = { Text("Delete this address?") },
                confirmButton = {
//                    val context = LocalContext.current
                    TextButton(onClick = { onConfirmSub(subToDelete, context) }) {
                        Text("Proceed")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { deleteSubConfirmationDialogue = false }) {
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
                                openGoogleMaps(googleMapsOpened.value)
                                googleMapsOpened.value = ""
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

        LaunchedEffect(currentLatLng.value) {
            apiFetchSubs()
            apiFetchDeals(currentLatLng.value)
        }

        @Composable
        fun SearchAddressScreen() {
            Row(modifier = Modifier.padding(start = 24.dp, bottom = 6.dp, end = 24.dp)) {
                AutoComplete("", onSelect = { autoCompleteInfo ->
                    CoroutineScope(Dispatchers.IO).launch {
                        currentAddress.value = autoCompleteInfo.address
                        currentLatLng.value = autoCompleteInfo.latLng
                        viewLocationPicker = false
                        defaultZoom.value = 15f
                    }
                }, onTextChanged = {} )
            }
        }

        @Composable
        fun SavedAddressItem(savedAddress: DealSubRetrievalResponse) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp)
                    .clickable {
                        CoroutineScope(Dispatchers.IO).launch {
                            currentAddress.value = savedAddress.address
                            currentLatLng.value = LatLng(
                                savedAddress.latitude.toDouble(),
                                savedAddress.longitude.toDouble()
                            )
                            viewLocationPicker = false
                            defaultZoom.value = 15f
                        }
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = savedAddress.address,
                    modifier = Modifier.weight(1f),
                    fontSize = 16.sp,
                    color = mainTextColor,
                    fontWeight = FontWeight.Medium
                )

                IconButton(
                    onClick = { onEditSubClick(savedAddress.id) },
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Address",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = { onDeleteSubClick(savedAddress.id) }
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Address",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        @Composable
        fun SavedAddressesScreen() {
            Column(Modifier.padding(8.dp)) {
                listOfSubs.forEach { address ->
                    SavedAddressItem(address)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable {
                            showBottomSheet = true
                            subToChange = -1
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Address",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    Text(
                        text = "Add new address",
                        modifier = Modifier.weight(1f),
                        fontSize = 16.sp,
                        color = mainTextColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        var columnScrollingEnabled by remember { mutableStateOf(true) }
        LaunchedEffect(cameraPositionState.isMoving) {
            if (!cameraPositionState.isMoving) {
                columnScrollingEnabled = true
            }
        }

        if (showBottomSheet) {
            AddSubScreen(
                subToChange,
                onDismissRequest = {
                    showBottomSheet = false
                    apiFetchSubs()
                })
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(mainBackgroundColor)
                .verticalScroll(scrollState, columnScrollingEnabled),
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
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        TabRow(selectedTabIndex = pagerState.currentPage,
                            containerColor = Color(0xFF6E2317),
                            modifier = Modifier
                                .padding(vertical = 1.dp, horizontal = 8.dp)
                                .clip(RoundedCornerShape(50))
                                .width(300.dp),
                            indicator = { tabPositions: List<TabPosition> ->
                                Box {}
                            }
                        ) {
                            listOf("Search", "Subscribed").forEachIndexed { index, text ->
                                val selected = pagerState.currentPage == index
                                Tab(
                                    modifier = if (selected) Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(Color.White)
                                    else Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(Color(0xFF6E2317)),
                                    selected = selected,
                                    onClick = {
                                        scope.launch { pagerState.animateScrollToPage(index) }
                                    },
                                    text = { Text(text = text, color = Color(0xFF9A6E7C), fontSize = 16.sp) }
                                )
                            }
                        }
                    }
                    HorizontalPager(state = pagerState) { page ->
                        when (page) {
                            0 -> SearchAddressScreen()
                            1 -> SavedAddressesScreen()
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
                    .fillMaxWidth()
                    .pointerInteropFilter(
                        onTouchEvent = {
                            when (it.action) {
                                MotionEvent.ACTION_DOWN -> {
                                    columnScrollingEnabled = false
                                    false
                                }

                                else -> {
                                    true
                                }
                            }
                        }
                    ),
                cameraPositionState = cameraPositionState,
                uiSettings = uiSettings.value
            ) {
                if (defaultZoom.value != 0f) {
                    Marker(
                        state = MarkerState(position = (currentLatLng.value ?: atasehir) as LatLng),
                        title = "Current Location"
                    )
                    listOfDeals.forEach { deal ->
                        Marker(
                            state = MarkerState(position = LatLng(deal.latitude.toDouble(), deal.longitude.toDouble())),
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
                            title = deal.name,
                            snippet = deal.description
                        )
                    }
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
                    onClick = { dealsNavController.navigate("addDealScreen/${-1}") },
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
                        if (viewingUserSubmittedDeals == "See User Submitted Deals") {
                            viewingUserSubmittedDeals = "See All Deals in Area"
                        } else {
                            if (currentLatLng.value == null) {
                                errorMessageForRegion = "Please set a region first"
                            } else {
                                errorMessageForRegion = ""
                                viewingUserSubmittedDeals = "See User Submitted Deals"
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
//        val context = LocalContext.current

        // ----- Start Date Picker Implementation -----
//        val startCalendar = Calendar.getInstance()
//        var selectedStartDate by remember {
//            mutableStateOf(
//                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(startCalendar.time)
//            )
//        }
//        val startDatePickerDialog = DatePickerDialog(
//            context,
//            { _, year, month, dayOfMonth ->
//                selectedStartDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
//                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
//                onStartDateChange(sdf.parse(selectedStartDate))
//            },
//            startCalendar.get(Calendar.YEAR),
//            startCalendar.get(Calendar.MONTH),
//            startCalendar.get(Calendar.DAY_OF_MONTH)
//        )

        // ----- End Date Picker Implementation -----
//        val endCalendar = Calendar.getInstance()
//        var selectedEndDate by remember {
//            mutableStateOf(
//                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(endCalendar.time)
//            )
//        }
//        val endDatePickerDialog = DatePickerDialog(
//            context,
//            { _, year, month, dayOfMonth ->
//                selectedEndDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
//                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
//                onEndDateChange(sdf.parse(selectedEndDate))
//            },
//            endCalendar.get(Calendar.YEAR),
//            endCalendar.get(Calendar.MONTH),
//            endCalendar.get(Calendar.DAY_OF_MONTH)
//        )

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

    // Helper to format date
    private fun formatTransactionDate(isoDate: String): String {
        val splitted = isoDate.split("T")
        return try {
            splitted[0]
        } catch (e: Exception) {
            Log.d("Error", "Error Parsing Date: ${e.message}")
            "Invalid Date"
        }
    }

}

suspend fun deleteDeal(id: String): Boolean {
    if (id.isInvalid()) {
        return false
    }

    return try {
        val response: Response<String> = RetrofitInstance.apiService.deleteDeal(id)
        Log.d("Response", "Delete Deal API Request actually called")
        if (response.isSuccessful) {
            Log.d("Response", "Deals Response: ${response.body()}")
            true
        } else {
            Log.d("Error", "Deals API Response Was Unsuccessful: $response")
            false
        }
    } catch (e: Exception) {
        Log.d("Response", "Exception when deleting deal: ${e.message}")
        false
    }
}

suspend fun deleteSub(id: Int): Boolean {
    if (id.isInvalid()) {
        return false
    }

    return try {
        val response: Response<String> = RetrofitInstance.apiService.deleteSub(id)
        if (response.isSuccessful) {
            Log.d("Response", "Subs Response: ${response.body()}")
            true
        } else {
            Log.d("Error", "Subs API Response Was Unsuccessful: $response")
            false
        }
    } catch (e: Exception) {
        Log.d("Response", "Exception when deleting address: ${e.message}")
        false
    }
}

suspend fun upvote(listOfDeals: List<DealRetrievalResponse>, deal_id: String, index: Int): List<DealRetrievalResponse>? {
    try {
        val response: Response<String> = if (listOfDeals[index].user_vote == 1) {
            RetrofitInstance.apiService.cancelvoteDeal(deal_id)
        } else {
            RetrofitInstance.apiService.upvoteDeal(deal_id)
        }
        if (response.isSuccessful) {
            when (listOfDeals[index].user_vote) {
                1 -> {
                    return listOfDeals.mapIndexed { i, deal ->
                        if (i == index) deal.copy(user_vote = 0, upvotes = --deal.upvotes)
                        else deal
                    }
                }
                0 -> {
                    return listOfDeals.mapIndexed { i, deal ->
                        if (i == index) deal.copy(user_vote = 1, upvotes = ++deal.upvotes)
                        else deal
                    }
                }
                -1 -> {
                    return listOfDeals.mapIndexed { i, deal ->
                        if (i == index) deal.copy(user_vote = 1, upvotes = ++deal.upvotes, downvotes = --deal.downvotes)
                        else deal
                    }
                }
            }
        } else {
            Log.d("Error", "Upvote Failed Response: ${response.body()}")
        }
    } catch (e: Exception) {
        Log.d("Error", "Failed to Upvote: ${e.message}")
    }
    return null
}

suspend fun downvote(listOfDeals: List<DealRetrievalResponse>, deal_id: String, index: Int): List<DealRetrievalResponse>? {
    try {
        val response: Response<String> = if (listOfDeals[index].user_vote == -1) {
            RetrofitInstance.apiService.cancelvoteDeal(deal_id)
        } else {
            RetrofitInstance.apiService.downvoteDeal(deal_id)
        }
        if (response.isSuccessful) {
            when (listOfDeals[index].user_vote) {
                -1 -> {
                    return listOfDeals.mapIndexed { i, deal ->
                        if (i == index) deal.copy(user_vote = 0, downvotes = --deal.downvotes)
                        else deal
                    }
                }
                0 -> {
                    return listOfDeals.mapIndexed { i, deal ->
                        if (i == index) deal.copy(user_vote = -1, downvotes = ++deal.downvotes)
                        else deal
                    }
                }
                1 -> {
                    return listOfDeals.mapIndexed { i, deal ->
                        if (i == index) deal.copy(user_vote = -1, downvotes = ++deal.downvotes, upvotes = --deal.upvotes)
                        else deal
                    }
                }
            }
        } else {
            Log.d("Error", "Downvote Failed Response: ${response.body()}")
        }
    } catch (e: Exception) {
        Log.d("Error", "Failed to Downvote: ${e.message}")
    }
    return null
}
