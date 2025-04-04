package com.cs446.expensetracker.ui.dashboard

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
import com.cs446.expensetracker.api.models.GoalRetrievalResponse
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.request.NullRequestDataException
import com.cs446.expensetracker.api.RetrofitInstance
import com.cs446.expensetracker.api.models.Category
import com.cs446.expensetracker.api.models.GoalCreationRequest
import com.cs446.expensetracker.api.models.GoalRetrievalGoals
import com.cs446.expensetracker.api.models.GoalUpdateRequest
import com.cs446.expensetracker.api.models.LevelRequest
import com.cs446.expensetracker.api.models.SpendingSummaryResponse
import com.cs446.expensetracker.session.UserSession
import com.cs446.expensetracker.ui.ui.theme.Typography
import com.cs446.expensetracker.ui.ui.theme.mainTextColor
import com.cs446.expensetracker.ui.ui.theme.secondTextColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*


@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGoalScreen(navController: NavController, useMockApi: String = "false", createMockAddGoalApiRequests: Array<Response<out Any>>? = null, editVersion: Int) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Input states
    var limit by remember { mutableStateOf("") }
    var period by remember { mutableStateOf("Week") }
    var goal_type by remember { mutableStateOf("amount") }

    // Category List State
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    // Bottom Sheet State
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }

    var isSaveButtonLoading by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var preloadErrorMessage by remember { mutableStateOf<String>("") }
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

    var listOfGoals by remember { mutableStateOf<List<GoalRetrievalGoals>>(emptyList()) }

    // Date Picker Dialog
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            calendar.set(year, month, dayOfMonth, 0, 0, 0)
            selectedDate = isoFormat.format(calendar.time)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    var specificGoalToEdit: GoalRetrievalGoals? by remember { mutableStateOf(null) }

    @Composable
    fun GoalTypeDropdownMenu() {
        var expanded by remember { mutableStateOf(false) }
        Text(text = "Goal Type:", color = mainTextColor)
        Box(
        ) {
            Button(onClick = { expanded = !expanded },
                modifier = Modifier
                    .border(1.dp, mainTextColor, shape = RoundedCornerShape(3.dp)).testTag("GoalDropdownMenu"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(
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
                    modifier = Modifier.testTag("PercentageDropdownMenuItem"),
                    text = { Text("amount") },
                    onClick = {
                        goal_type="amount"
                        expanded = !expanded
                    }
                )
                DropdownMenuItem(
                    modifier = Modifier.testTag("PercentageDropdownMenuItem"),
                    text = { Text("percentage") },
                    onClick = {
                        goal_type="percentage"
                        expanded = !expanded
                    }
                )
            }
        }
    }

    @Composable
    fun PeriodTypeDropdownMenu() {
        var expanded by remember { mutableStateOf(false) }
        Text(text = "Time Period:", color = mainTextColor)
        Box(
        ) {
            Button(onClick = { expanded = !expanded },
                modifier = Modifier
                    .border(1.dp, mainTextColor, shape = RoundedCornerShape(3.dp)).testTag("PeriodDropdownMenu"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(
                    0xFFE7E0EC
                )
                ), shape = RoundedCornerShape(3.dp)) {
                Text(text = period, color = Color(0xFF606060))
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "More options", tint = mainTextColor)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    modifier = Modifier.testTag("PeriodDropdownMenuItem"),
                    text = { Text("Week") },
                    onClick = {
                        period="Week"
                        expanded = !expanded
                    }
                )
                DropdownMenuItem(
                    modifier = Modifier.testTag("PeriodDropdownMenuItem"),
                    text = { Text("Month") },
                    onClick = {
                        period="Month"
                        expanded = !expanded
                    }
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
                .padding(10.dp)
                .testTag("CategoryPicker")
            ,
            contentAlignment = Alignment.CenterStart
        ) {
            Text(text = selectedCategory?.name ?: "Select a Category")
        }

        Spacer(modifier = Modifier.height(10.dp))

        GoalTypeDropdownMenu()
        PeriodTypeDropdownMenu()

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = limit,
            onValueChange = { limit = it },
            label = { Text("Limit") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth().testTag("LimitField"),
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
                .padding(10.dp)
                .testTag("DatePicker")
            ,
            contentAlignment = Alignment.CenterStart
        ) {
            Text(text = selectedDate.substringBeforeLast(("T")))
        }

        Spacer(modifier = Modifier.height(10.dp))
    }

    // End of definitions, actual seen code starts here

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
                modifier = Modifier.testTag("TitleText")
            )
            Spacer(modifier = Modifier.weight(1.0f))
            TextButton(onClick = { navController.popBackStack() },
                contentPadding = PaddingValues(
                    start = 5.dp,
                    top = 0.dp,
                    end = 0.dp,
                    bottom = 10.dp,
                ),
                modifier = Modifier.testTag("ClosePage")
            ) {
                Text("X",  style = Typography.titleLarge, color= Color(0xFF4B0C0C))
            }
        }

        @Suppress("UNCHECKED_CAST")
        fun preloadData() {
            coroutineScope.launch {
                preloadErrorMessage = ""
                var categoriesResponse: Response<List<Category>>? = null
                var goalsResponse: Response<GoalRetrievalResponse>? = null
                var responses: List<Response<out Any>>
                if (useMockApi != "false") {
                    try {
                        if (createMockAddGoalApiRequests != null) {
                            if (createMockAddGoalApiRequests.size == 4) {
                                categoriesResponse = createMockAddGoalApiRequests[2] as Response<List<Category>>
                                goalsResponse = createMockAddGoalApiRequests[3] as Response<GoalRetrievalResponse>
                            } else {
                                throw NullRequestDataException()
                            }
                        }
                    } catch (e: Exception) {
                        errorMessage += "Error Fetching Mock User Data.\n"
                        Log.d("Dashboard Error", "Error Calling Mock API: ${e}")
                        isLoading = false
                    }
                } else {
                    try {
                        val categoriesDeferred = async(Dispatchers.IO) {
                            RetrofitInstance.apiService.getCategories()
                        }
                        val goalsDeferred = async(Dispatchers.IO) {
                            RetrofitInstance.apiService.getGoals()
                        }

                        // Await all calls to complete
                        withTimeout(5000) {
                            responses = awaitAll(categoriesDeferred, goalsDeferred)
                        }
                        categoriesResponse = responses[0] as Response<List<Category>>
                        goalsResponse = responses[1] as Response<GoalRetrievalResponse>

                    } catch (e: Exception) {
                        errorMessage += "Failed to load user data.\n"
                        Log.d("Add Goal Error", "Error Calling API: ${e.message}")
                        isLoading = false
                    }

                }
                try {
                    if (categoriesResponse?.isSuccessful == true) {
                        categories = categoriesResponse.body() as List<Category>
                    } else {
                        preloadErrorMessage += "Failed to load categories.\n"
                        Log.d("Error", "Categories API Response Was Unsuccessful: $categories")
                    }

                    if (goalsResponse?.isSuccessful == true) {
                        val responseBody = goalsResponse.body() as GoalRetrievalResponse
                        Log.d("Response", "Goals Response: $responseBody")
                        listOfGoals = responseBody.goals.map { x ->
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
                                amount_spent = x.amount_spent,
                            )
                        }

                        if (editVersion != -1) {
                            for (goal in listOfGoals) {
                                if (goal.id == editVersion) {
                                    specificGoalToEdit = goal
                                    specificGoalToEdit!!.category_string = categories.find { it.id == specificGoalToEdit!!.category_id }?.name ?: "Deleted Category"
                                }
                            }
                            period = specificGoalToEdit?.period.toString()
                            if(period == "31") {
                                period = "Month"
                            } else {
                                period = "Week"
                            }
                            goal_type = specificGoalToEdit?.goal_type ?: ""
                            limit = specificGoalToEdit?.limit.toString()
                            selectedDate = specificGoalToEdit?.start_date ?: ""
                            for(category in categories) {
                                if(category.id == specificGoalToEdit?.category_id) {
                                    selectedCategory = category
                                }
                            }
                        }

                    } else {
                        preloadErrorMessage = "Failed to load goals.\n"
                        Log.d("Error", "Goals API Response Was Unsuccessful: $goalsResponse")
                    }


                } catch (e: Exception) {
                    preloadErrorMessage += "Error Fetching User Data.\n"
                    Log.d("Error", "Error Calling Summary Spend API: ${e.message}")
                    isLoading = false
                }
            }

        }

        LaunchedEffect(Unit) {
            isLoading = true
            Log.d("Token", "Token: $UserSession.access_token")
            preloadData()
        }

        if (categories.isNotEmpty() && (if (editVersion == -1) true else (specificGoalToEdit != null))) {
            isLoading = false
        }
        if(preloadErrorMessage != "") {
            isLoading = false
        }

        when {
            isLoading -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                )
                {
                    CircularProgressIndicator(
                        modifier = Modifier.size(80.dp),
                        strokeWidth = 6.dp,
                        color = mainTextColor
                    )
                }
            }
            preloadErrorMessage != "" -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .testTag("ErrorMessage"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                )
                {
                    Text(text = preloadErrorMessage, color = MaterialTheme.colorScheme.error, style = Typography.headlineSmall)
                    Button(onClick = { preloadData() }, colors = ButtonDefaults.buttonColors(containerColor = mainTextColor)) { Text("Retry") }
                }
            }

            else -> {
                allFieldInputs()
                if (errorMessage != null) {
                    Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("ErrorMessage"))
                }
                // Save Expense Button
                Button(
                    onClick = {
                        isSaveButtonLoading = true
                        coroutineScope.launch {
                            errorMessage = null
                            try {
                                if(selectedCategory == null) {
                                    errorMessage = errorMessage ?: ""
                                    errorMessage += "Please select a category\n"
                                    isSaveButtonLoading = false
                                }
                                if (goal_type == "") {
                                    errorMessage = errorMessage ?: ""
                                    errorMessage += "Please pick a goal type\n"
                                    isSaveButtonLoading = false
                                }
                                val nullCheckLimit = limit.toDoubleOrNull()
                                Log.d("Response", "Amount: ${nullCheckLimit}")
                                if (nullCheckLimit == null) {
                                    errorMessage = errorMessage ?: ""
                                    errorMessage += "Please add a numerical limit\n"
                                    isSaveButtonLoading = false
                                }
                                if(nullCheckLimit != null && nullCheckLimit < 0) {
                                    errorMessage = errorMessage ?: ""
                                    errorMessage += "Please add a limit greater than 0"
                                    isSaveButtonLoading = false
                                }
                                if(nullCheckLimit != null && (nullCheckLimit > 100 && goal_type == "percentage")) {
                                    errorMessage = errorMessage ?: ""
                                    errorMessage += "If using percentage, please add a limit less than 100%\n"
                                    isSaveButtonLoading = false
                                }
                                if (period != "Week" && period != "Month") {
                                    errorMessage = errorMessage ?: ""
                                    errorMessage += "Please pick a time period\n"
                                    isSaveButtonLoading = false
                                }
                                if (selectedDate == "") {
                                    errorMessage = errorMessage ?: ""
                                    errorMessage += "Please add a date\n"
                                    isSaveButtonLoading = false
                                }

                                if(errorMessage == null) {
                                    if(editVersion != -1) {
                                        val updateGoalRequest = GoalUpdateRequest (
                                            category_id=selectedCategory?.id ?: 0,
                                            limit=limit.toDouble(),
                                            start_date=selectedDate,
                                            end_date=selectedDate,
                                            goal_type= goal_type,
                                        )

                                        Log.d("Response", "Edit Goal Request: ${updateGoalRequest}")

                                        var response: Response<GoalRetrievalGoals>? = null

                                        if(useMockApi == "false") {
                                            response = RetrofitInstance.apiService.updateGoal(editVersion.toString(), updateGoalRequest)
                                        }
                                        if (response?.isSuccessful == true || useMockApi == "default") {
                                            goBack = true
                                            isSaveButtonLoading = false
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "Failed to edit goal. Please try again",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            Log.d("Response", "Api request to edit goal failed: ${response?.body()}")
                                            errorMessage = errorMessage ?: ""
                                            errorMessage += "Editing goal failed. Please try again.\n"
                                            isSaveButtonLoading = false
                                        }
                                    } else {
                                        val goal = GoalCreationRequest (
                                            category_id=selectedCategory?.id ?: 0,
                                            goal_type= goal_type,
                                            limit=limit.toDouble(),
                                            start_date=selectedDate,
                                            period= if(period == "Week") 7.0 else 31.0,
                                        )
                                        Log.d("Response", "Create Goal Request: ${goal}")
                                        val token = UserSession.access_token
                                        var response: Response<GoalRetrievalResponse>? = null
                                        if(useMockApi == "false") {
                                            response = RetrofitInstance.apiService.addGoal(goal)
                                        }
                                        if (response?.isSuccessful == true || useMockApi == "default") {
                                            goBack = true
                                            Log.d("Response", "Add Goal Response: ${response?.body()}")
                                            isSaveButtonLoading = false
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "Failed to add goal. Please try again",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            Log.d("Response", "Api request to add goal failed: ${response?.body()}")
                                            errorMessage = errorMessage ?: ""
                                            errorMessage += "Adding goal failed. Please try again.\n"
                                            isSaveButtonLoading = false
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.d("Response", "Exception when adding goal: ${e.message}")
                                errorMessage = errorMessage ?: ""
                                errorMessage += "Exception happened when adding goal. Please try again.\n"
                                isSaveButtonLoading = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("SaveButton"),
                    colors = ButtonDefaults.buttonColors(
                        Color(0xFF4B0C0C),
                    ),
                    enabled = !isLoading
                ) {
                    if (isSaveButtonLoading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text(text = "Save Goal")
                    }
                }
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