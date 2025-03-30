package com.cs446.expensetracker.ui.deals

import android.content.Context
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.toLowerCase
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import com.cs446.expensetracker.ui.ui.theme.mainTextColor
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Task
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FetchPlaceResponse
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class AutoCompleteInformation(var latLng: LatLng?, var address: String)

@Composable
fun AutoComplete(defaultValue: String, onSelect: (AutoCompleteInformation) -> Unit, onTextChanged: (String) -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var category by remember(defaultValue) {
        mutableStateOf(defaultValue)
    }

    var latLngFromApi: LatLng? = null

    val heightTextFields by remember {
        mutableStateOf(55.dp)
    }

    var textFieldSize by remember {
        mutableStateOf(Size.Zero)
    }

    var expanded by remember {
        mutableStateOf(false)
    }
    val interactionSource = remember {
        MutableInteractionSource()
    }

    var locationPredictions by remember { mutableStateOf<List<AutocompletePrediction>>(emptyList()) }

    // if you don't initialize with specifically newplacesapienabled it will not work
    Places.initializeWithNewPlacesApiEnabled(LocalContext.current, "AIzaSyCZy108qh22SL8tbbRqKlPCw1vdNimsRMc")
    val placesClient: PlacesClient = remember { Places.createClient(context) }

    suspend fun fetchPlaceAsync(placeId: String): LatLng? {
        try {
            // Specify the list of fields to return.
            val placeFields: List<Place.Field> = mutableListOf(Place.Field.LOCATION)

            // Construct a request object, passing the place ID and field list.
            val request = FetchPlaceRequest.newInstance(placeId, placeFields)

            // Await the result instead of using callbacks
            val placeTask: FetchPlaceResponse? = placesClient.fetchPlace(request).await()
            if (placeTask != null) {
                Log.d("TAG", "placeTask: ${placeTask.place.location}")
                return placeTask.place.location
            }
            return null
        } catch (e: Exception) {
            Log.e("TAG", "Error fetching place: ${e.message}")
            return null
        }
    }

    suspend fun getAddressPredictions(
        sessionToken: AutocompleteSessionToken = AutocompleteSessionToken.newInstance(),
        inputString: String,
        location: LatLng? = null,
        context: Context
    ) = suspendCoroutine<List<AutocompletePrediction>> {
        Log.d("TAG", "getAddressPredictions")

        placesClient.findAutocompletePredictions(
            FindAutocompletePredictionsRequest.builder()
                .setOrigin(LatLng(43.452969, -80.495064))
//                .setLocationBias(bias)
                // .setLocationRestriction(bounds)
                .setSessionToken(sessionToken)
                .setQuery(inputString)
                .build()
        ).addOnCompleteListener { completedTask ->
            if (completedTask.exception != null) {
                it.resume(listOf())
                val errorString = completedTask.exception?.stackTraceToString().orEmpty()
                Log.d("TAG", "Error in API $errorString")
            } else {
                it.resume(completedTask.result.autocompletePredictions)
                locationPredictions = completedTask.result.autocompletePredictions
                for (thing in locationPredictions) {
                    var temp = thing.getFullText(null).toString()
//                    Log.d("TAG", "Ok we made it here $temp")
                }
            }
        }

    }

    suspend fun onLocationTextChanged(newText: String, context: Context) {
        Log.d("TAG", "onLocationTextChanged: $newText")
        getAddressPredictions(AutocompleteSessionToken.newInstance(), newText, null, context)
    }

    // Category Field
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    expanded = false
                }
            )
    ) {

        Text(
            modifier = Modifier.padding(start = 3.dp, bottom = 2.dp),
            text = "Address",
            fontSize = 16.sp,
            color = mainTextColor,
            fontWeight = FontWeight.Medium
        )

        Column(modifier = Modifier.fillMaxWidth()) {

            Row(modifier = Modifier.fillMaxWidth()) {
                TextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(heightTextFields)
                        .border(
                            width = 1.8.dp,
                            color = mainTextColor,
                            shape = RoundedCornerShape(7.dp)
                        )
                        .onGloballyPositioned { coordinates ->
                            textFieldSize = coordinates.size.toSize()
                        },
                    value = category,
                    onValueChange = {
                        category = it
                        expanded = true
                        onTextChanged(it)
                        coroutineScope.launch {
                            onLocationTextChanged(it, context)
                        }
                    },
                    textStyle = TextStyle(
                        color = Color.Black,
                        fontSize = 16.sp
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { expanded = !expanded }) {
                            Icon(
                                modifier = Modifier.size(24.dp),
                                imageVector = Icons.Rounded.KeyboardArrowDown,
                                contentDescription = "arrow",
                                tint = mainTextColor
                            )
                        }
                    }
                )
            }

            AnimatedVisibility(visible = expanded) {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 5.dp)
                        .width(textFieldSize.width.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {

                    LazyColumn(
                        modifier = Modifier.heightIn(max = 150.dp),
                    ) {

                        if (category.isNotEmpty()) {
                            items(
                                locationPredictions.filter {
                                    it.getFullText(null).toString().lowercase()
                                        .contains(category.lowercase())
                                }
                            ) {
                                CategoryItems(title = it.getFullText(null).toString()) { title ->
                                    category = title
                                    expanded = false
                                    if(it != null) {
                                        coroutineScope.launch {
                                            latLngFromApi = fetchPlaceAsync(it.placeId)
                                            var sendToAddDealScreen = AutoCompleteInformation(latLngFromApi, it.getFullText(null).toString())
                                            onSelect(sendToAddDealScreen)
                                        }
                                    }
                                }
                            }
                        } else {
                            items(
                                locationPredictions
                            ) {
                                CategoryItems(title = it.getFullText(null).toString()) { title ->
                                    category = title
                                    expanded = false

                                    if(it != null) {
                                        coroutineScope.launch {
                                            latLngFromApi = fetchPlaceAsync(it.placeId)
                                        }
                                    }
                                    var sendToAddDealScreen = AutoCompleteInformation(latLngFromApi, it.getFullText(null).toString())
                                    onSelect(sendToAddDealScreen)
                                }
                            }
                        }

                    }

                }
            }

        }

    }

    // composable equivalent to the onDestroy of an activity or fragment
    // Fixes this error from google api: Previous channel {0} was garbage collected without being shut down!
    DisposableEffect(Unit) {
        onDispose {
            (placesClient as? AutoCloseable)?.close() // Ensure proper cleanup
        }
    }


}

@Composable
fun CategoryItems(
    title: String,
    onSelect: (String) -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onSelect(title)
            }
            .padding(10.dp)
    ) {
        Text(text = title, fontSize = 16.sp)
    }

}