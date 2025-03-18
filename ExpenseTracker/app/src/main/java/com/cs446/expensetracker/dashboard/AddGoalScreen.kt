package com.cs446.expensetracker.dashboard

import android.app.DatePickerDialog
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Settings
import com.cs446.expensetracker.api.models.GoalRetrievalResponse
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
import com.cs446.expensetracker.api.models.Category
import com.cs446.expensetracker.api.models.DealCreationRequest
import com.cs446.expensetracker.api.models.DealRetrievalResponse
import com.cs446.expensetracker.api.models.GoalCreationRequest
import com.cs446.expensetracker.api.models.GoalRetrievalGoals
import com.cs446.expensetracker.api.models.GoalUpdateRequest
import com.cs446.expensetracker.session.UserSession
import com.cs446.expensetracker.ui.ui.theme.Typography
import com.cs446.expensetracker.ui.ui.theme.mainTextColor
import com.cs446.expensetracker.ui.ui.theme.secondTextColor
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*


@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGoalScreen(navController: NavController, editVersion: Int) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var limit by remember { mutableStateOf("") }
    var period by remember { mutableStateOf("") }
    var goal_type by remember { mutableStateOf("amount") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var goBack by remember { mutableStateOf(false) }

    // Category List State
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    // Bottom Sheet State
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }

    // Date Picker State
    val calendar = Calendar.getInstance()
    val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
    var selectedDate by remember {
        mutableStateOf(
            isoFormat.format(calendar.time)
        )
    }

    var listOfGoals by remember { mutableStateOf<List<GoalRetrievalGoals>>(emptyList()) }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                val response = RetrofitInstance.apiService.getCategories()
                if (response.isSuccessful) {
                    categories = response.body() ?: emptyList()
                } else {
                    errorMessage = "Failed to load categories."
                }
            } catch (e: Exception) {
                errorMessage = "Error: ${e.message}"
            }
        }
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

    var specificGoalToEdit: GoalRetrievalGoals? by remember { mutableStateOf(null) }

    fun apiFetchSpecificGoal() {
        // Load deals via API
        Log.d("Response", "Api fetch was called, but request not necessarily sent")
        errorMessage = ""
        Log.d("Response", "Api fetch was called, but request not necessarily sent")
        CoroutineScope(Dispatchers.IO).launch {
//                isLoading = true
//                errorMessage = ""
            try {
                val token = UserSession.access_token ?: ""
                val response: Response<GoalRetrievalResponse> =
                    RetrofitInstance.apiService.getGoals()
                Log.d("Response", "Fetch Goals API Request actually called")
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    Log.d("Response", "Goals Response: $responseBody")
                    listOfGoals = responseBody?.goals?.map { x ->
                        GoalRetrievalGoals(
                            id = x.id,
                            category_id = x.category_id,
                            goal_type = x.goal_type,
                            limit = x.limit,
                            start_date = x.start_date,
                            end_date = x.end_date,
                            period = x.period,
                            on_track = x.on_track,
                            time_left = x.time_left,
                        )
                    } ?: emptyList()
                    for (goal in listOfGoals) {
                        if (goal.id == editVersion) {
                            specificGoalToEdit = goal
                        }
                    }
                } else {
                    errorMessage = "Failed to load data."
                    Log.d("Error", "Deals API Response Was Unsuccessful: $response")
                }
            } catch (e: Exception) {
                errorMessage = "Error: ${e.message}"
                Log.d("Error", "Error Calling Deals API: $errorMessage")
            } finally {
                //                    isLoading = false
            }
            period = specificGoalToEdit?.period.toString()
            goal_type = specificGoalToEdit?.goal_type ?: ""
            limit = specificGoalToEdit?.limit.toString()
            selectedDate = specificGoalToEdit?.start_date ?: ""
            for(category in categories) {
                if(category.id == specificGoalToEdit?.category_id) {
                    selectedCategory = category
                }
            }
        }


    }

    @Composable
    fun GoalTypeDropdownMenu() {
        var expanded by remember { mutableStateOf(false) }
        Text(text = "Goal Type:", color = mainTextColor)
        Box(
        ) {
            Button(onClick = { expanded = !expanded },
                modifier = Modifier
                    .border(1.dp, mainTextColor, shape = RoundedCornerShape(3.dp)), colors = ButtonDefaults.buttonColors(containerColor = Color(
                    0xFFE7E0EC
                )
                ), shape = RoundedCornerShape(3.dp)) {
                Text(text = goal_type, color = Color(0xFF606060))
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "More options", tint = mainTextColor)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("amount") },
                    onClick = { goal_type="amount" }
                )
                DropdownMenuItem(
                    text = { Text("percentage") },
                    onClick = { goal_type="percentage" }
                )
            }
        }
    }

    @Composable
    fun allFieldInputs() {
        // Category Box (Tap to Open Bottom Sheet)
        Text(text = "Category", style = MaterialTheme.typography.bodyLarge)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clickable { showBottomSheet = true }
                .padding(10.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(text = selectedCategory?.name ?: "Select a Category")
        }

        Spacer(modifier = Modifier.height(10.dp))

        GoalTypeDropdownMenu()

        OutlinedTextField(
            value = period,
            onValueChange = { period = it },
            label = { Text("Period") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = secondTextColor,
                unfocusedIndicatorColor = mainTextColor)
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = limit,
            onValueChange = { limit = it },
            label = { Text("Limit") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = secondTextColor,
                unfocusedIndicatorColor = mainTextColor)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Date Picker
        Text(text = "Start Date", style = MaterialTheme.typography.bodyLarge, color= mainTextColor)
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
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            var titleText = "Submit a Goal"
            if (editVersion != -1) {
                titleText = "Edit Goal"
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
        LaunchedEffect(categories) {
            if (editVersion != -1) {
                apiFetchSpecificGoal()
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
//                    isLoading = true
                    errorMessage = null
                    try {
                        if(selectedCategory == null) {
                            errorMessage = errorMessage ?: ""
                            errorMessage += "Please select a category\n"
                        }
                        if (goal_type == "") {
                            errorMessage = errorMessage ?: ""
                            errorMessage += "Please pick a goal type\n"
                        }
                        var nullCheckLimit = limit.toDoubleOrNull()
                        Log.d("Response", "Amount: ${nullCheckLimit}")
                        if (nullCheckLimit == null) {
                            errorMessage = errorMessage ?: ""
                            errorMessage += "Please add a numerical limit\n"
                        }
                        if(nullCheckLimit != null && (nullCheckLimit > 100 && goal_type == "percentage")) {
                            errorMessage = errorMessage ?: ""
                            errorMessage += "If using percentage, please add a limit less than 100%\n"
                        }
                        var nullCheckPeriod = period.toDoubleOrNull()
                        if (nullCheckPeriod == null) {
                            errorMessage = errorMessage ?: ""
                            errorMessage += "Please add a numerical period\n"
                        }
                        if (selectedDate == "") {
                            errorMessage = errorMessage ?: ""
                            errorMessage += "Please add a date\n"
                        }

                        if(errorMessage == null) {
                            val goal = GoalCreationRequest (
                                category_id=selectedCategory?.id ?: 0,
                                goal_type= goal_type,
                                limit=limit.toDouble(),
                                start_date=selectedDate,
                                period=period.toDouble(),
                            )
                            if(editVersion != -1) {
                                Log.d("Response", "Edit Goal Request: ${goal}")

                                val updateGoalRequest = GoalUpdateRequest (
                                    limit=limit.toDouble(),
                                    start_date=selectedDate,
                                    end_date=selectedDate,
                                    goal_type= goal_type,
                                )

                                val token = UserSession.access_token ?: ""
                                val response =
                                    RetrofitInstance.apiService.updateGoal(editVersion.toString(), updateGoalRequest)
                                if (response.isSuccessful) {
                                    goBack = true
                                    Log.d("Response", "Edit Goal Response: ${response}")
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Failed to edit goal. Please try again",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    Log.d("Response", "Api request to edit goal failed: ${response.body()}")
                                }
                            } else {
                                val token = UserSession.access_token ?: ""
                                val response: Response<GoalRetrievalResponse> =
                                    RetrofitInstance.apiService.addGoal(goal)
                                if (response.isSuccessful) {
                                    goBack = true
                                    Log.d("Response", "Add Goal Response: ${response.body()}")
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Failed to add goal. Please try again",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    Log.d("Response", "Api request to add goal failed: ${response.body()}")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.d("Response", "Exception when adding goal: ${e.message}")
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
                Text(text = "Save Goal")
            }
        }
        if (goBack) {
            goBack = false
            var toastText = "added"
            if (editVersion != -1) {
                toastText = "updated"
            }
            Toast.makeText(
                context,
                "Goal ${toastText} successfully!",
                Toast.LENGTH_SHORT
            ).show()
            navController.popBackStack()
        }
        // Bottom Sheet Implementation
        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = sheetState
            ) {
                Text(
                    text = "Select a Category",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(16.dp)
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp) // Limits height for scrolling
                ) {
                    items(categories) { category ->
                        ListItem(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedCategory = category
                                    showBottomSheet = false
                                }
                                .padding(8.dp),
                            headlineContent = {
                                Text(
                                    text = category.name,
                                    color = if (selectedCategory?.id == category.id) Color.Blue else Color.Black
                                )
                            }
                        )
                    }
                }
            }
        }
    }

}