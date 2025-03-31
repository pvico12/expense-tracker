package com.cs446.expensetracker.ui.dashboard

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cs446.expensetracker.api.RetrofitInstance
import com.cs446.expensetracker.api.models.Category
import com.cs446.expensetracker.api.models.CategoryBreakdown
import com.cs446.expensetracker.api.models.SpendingSummaryResponse
import com.cs446.expensetracker.session.UserSession
import com.cs446.expensetracker.ui.ui.theme.*
import kotlinx.coroutines.launch
import retrofit2.Response
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerState
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.cs446.expensetracker.api.models.GoalRetrievalGoals
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.filled.AddToPhotos
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavController
import coil.request.NullRequestDataException
import com.cs446.expensetracker.api.models.GoalRetrievalResponse
import com.cs446.expensetracker.api.models.GoalRetrievalStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.cs446.expensetracker.api.models.LevelRequest
import com.cs446.expensetracker.utils.formatCurrency
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout


class Dashboard {

    private val defaultCategoryColours = arrayOf("#FF9A3B3B", "#FFC08261", "#FFDBAD8C", "#FFFFEBCF", "#FFFFCFAC", "#FFFFDADA", "#FFD6CBAF", "#FF8D5F2E")

    @OptIn(ExperimentalMaterial3Api::class)
    @RequiresApi(Build.VERSION_CODES.O)
    @Composable
    fun DashboardScreen(
        drawerState: DrawerState,
        dashboardNavController: NavController,
        useMockApi: String = "false",
        createMockApiRequests: Array<Response<out Any>>? = null
    ) {
        // variable definitions
        val scrollState = rememberScrollState()
        // needed for coroutines, cache scope
        val coroutineScope = rememberCoroutineScope()
        var errorMessage by remember { mutableStateOf("") }
        var isLoading by remember { mutableStateOf(true) }
        // for spending summary
        var spendingSummary by remember { mutableStateOf<List<CategoryBreakdown>?>(null) }
        var categories by remember { mutableStateOf<List<Category>?>(null) }
        var totalSpending by remember { mutableDoubleStateOf(0.0) }
        // for goals editing
        var deleteConfirmationDialogue by remember { mutableStateOf(false)}
        var idToDelete by remember { mutableIntStateOf(-1) }
        // for goals
        var viewSpendingOrGoals by rememberSaveable { mutableStateOf("View Goals") }
        var listOfGoals by remember { mutableStateOf<List<GoalRetrievalGoals>?>(null) }
        var goalStats by remember { mutableStateOf<GoalRetrievalStats?>(null) }
        // for levels
        var levelStats by remember { mutableStateOf<LevelRequest?>(null) }
        var playPetAnimation by remember { mutableStateOf(false) }
        // for search
        var searchQuery by remember { mutableStateOf("") }
        var filteredSearchlistOfGoals by remember { mutableStateOf<List<GoalRetrievalGoals>>(emptyList())}
        // for Scaffold
        var currentDate by remember { mutableStateOf(LocalDateTime.now()) }
        val monthName by remember { mutableStateOf(currentDate.format(DateTimeFormatter.ofPattern("MMMM"))) }
        // for api calls
        val firstDayOfMonth by remember { mutableStateOf(currentDate.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0).format(DateTimeFormatter.ISO_DATE_TIME)) }
        val lastDayOfMonth by remember { mutableStateOf(currentDate.withDayOfMonth(currentDate.toLocalDate().lengthOfMonth()).withHour(23).withMinute(59).withSecond(59).format(DateTimeFormatter.ISO_DATE_TIME)) }

        fun assignSpendingResponse(responseBody: SpendingSummaryResponse) {
            totalSpending = responseBody.total_spend
            Log.d("Response", "Summary Spend Response: $responseBody for $firstDayOfMonth to $lastDayOfMonth")
            spendingSummary = responseBody.category_breakdown.map { x ->
                CategoryBreakdown(
                    category_name = x.category_name,
                    total_amount = x.total_amount,
                    percentage = x.percentage,
                    color = x.color,
                )
            }
            var colorIter = -1
            for (expense in spendingSummary!!) {
                if (expense.color == null) {
                    colorIter = (colorIter + 1) % defaultCategoryColours.size
                    expense.color = defaultCategoryColours[colorIter]
                }
            }
        }

        fun assignGoalResponse(responseBody: GoalRetrievalResponse) {
            Log.d("Response", "Goals Response: $responseBody for $firstDayOfMonth to $lastDayOfMonth")
            goalStats = responseBody.stats
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
            } // should automatically make an empty list if no goals are found

            for (goal in listOfGoals!!) {
                if (categories != null) {
                    goal.category_string = categories!!.find { it.id == goal.category_id }?.name ?: "Deleted Category"
                }
            }
        }

        fun asyncFetchGoals() {
            coroutineScope.launch {
                if (useMockApi == "false") {
                    errorMessage = ""
                    try {
//                    val token = UserSession.access_token ?: ""
                        val response: Response<GoalRetrievalResponse> =
                            RetrofitInstance.apiService.getGoals(firstDayOfMonth, lastDayOfMonth)
                        Log.d("Response", "Fetch Goals API Request actually called")
                        if (response.isSuccessful) {
                            val responseBody = response.body()
                            if (responseBody != null) {
                                assignGoalResponse(responseBody)
                            }
                        } else {
                            errorMessage += "Failed to load data."
                            Log.d("Error", "Goals API Response Was Unsuccessful: $response")
                        }
                    } catch (e: Exception) {
                        errorMessage += "Error: ${e.message}"
                        Log.d("Error", "Error Calling Goals API: $errorMessage")
                    }
                }
            }
        }

        // function definitions

        fun onConfirm(id: Int, context: Context) {
            deleteConfirmationDialogue = false
            coroutineScope.launch {
                isLoading = true
                try {
                    if(useMockApi == "false") {
                        val response: Response<String> =
                            RetrofitInstance.apiService.deleteGoal(id.toString())
                        Log.d("Response", "Fetch Goals API Request actually called")
                        if (response.isSuccessful) {
                            val responseBody = response.body()
                            Log.d("Response", "Goals Response: $responseBody")
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    context,
                                    "Goal Deleted",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else {
                            Log.d("Error", "Goals API Response Was Unsuccessful: $response")
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    context,
                                    "Failed to Delete Goal, Please Try Again",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        asyncFetchGoals()
                    }
                } catch (e: Exception) {
                    Log.d("Error", "Error Calling Goals API: $e")
                } finally {
                    isLoading = false
                }
            }
        }

        fun onEditButtonClick(id: Int) {
            dashboardNavController.navigate("addGoalScreen/$id")
        }
        fun onDeleteButtonClick(id: Int) {
            idToDelete = id
            deleteConfirmationDialogue = true
        }

        @RequiresApi(Build.VERSION_CODES.O)
        @Composable
        fun goalCard(goal: GoalRetrievalGoals) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 8.dp)
                    .testTag("GoalCard")
                ,
                colors = CardDefaults.cardColors(
                    containerColor = tileColor,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(
                        onClick = { onDeleteButtonClick(goal.id) },
                        shape = CircleShape,
                        modifier = Modifier.size(30.dp).testTag("DeleteButton"),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "delete",
                            modifier = Modifier.size(20.dp),
                            tint = secondaryIconColor
                        )
                    }
                    TextButton(
                        onClick = { onEditButtonClick(goal.id) },
                        shape = CircleShape,
                        modifier = Modifier.size(30.dp).testTag("EditButton"),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "edit",
                            modifier = Modifier.size(20.dp),
                            tint = secondaryIconColor
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 16.dp,
                            end = 16.dp,
                            top = 4.dp,
                            bottom = 16.dp
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val mainGoalText: String
                    var secondaryGoalText: String
                    var periodLength = "week"
                    if (goal.period != 7) {
                        periodLength = "month"
                    }
                    if(goal.goal_type == "amount") {
                        mainGoalText = "Spend less than $" + formatCurrency(goal.limit) + " on ${goal.category_string}"
                        secondaryGoalText = "$${formatCurrency(goal.amount_spent)} amount spent in set $periodLength"
                    } else {
                        mainGoalText = "Spend " + formatCurrency(goal.limit) + "% less than last $periodLength on ${goal.category_string}"
                        secondaryGoalText = "${formatCurrency(goal.amount_spent)}% less spent than last set $periodLength"
                    }
                    val icon: ImageVector
                    val iconColor: Color
                    val formatter = DateTimeFormatter.ISO_DATE_TIME
                    val endDateTime = LocalDateTime.parse(goal.end_date, formatter)
                    var secondaryGoalTextColor: Color = MaterialTheme.colorScheme.secondary

                    if (currentDate.isBefore(endDateTime)) {
                        icon = Icons.Filled.Circle
                        iconColor = neutralOrange
                        secondaryGoalTextColor = if (goal.on_track) {
                            secondaryPositiveGreen
                        } else {
                            secondaryNegativeRed
                        }
                        secondaryGoalText += " so far"
                    } else if(goal.on_track && endDateTime.isBefore(currentDate)){
                        icon = Icons.Filled.Check
                        iconColor = positiveGreen
                    } else {
                        icon = Icons.Filled.Close
                        iconColor = negativeRed
                    }
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = mainGoalText, fontWeight = FontWeight.Bold, color= mainTextColor, style = Typography.titleSmall, modifier = Modifier.testTag("MainGoalCardText"))
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(35.dp)
                                    ,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Time Left: ${goal.time_left} days",
                                        fontWeight = FontWeight.Bold,
                                        color = secondTextColor,
                                        modifier = Modifier.padding(end = 14.dp),
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = "Favorite",
                                        modifier = Modifier.size(35.dp),
                                        tint = iconColor
                                    )
                                }
                                Text(
                                    text = secondaryGoalText,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = secondaryGoalTextColor,
                                    textAlign = TextAlign.Left,
                                    modifier = Modifier.padding(top=2.dp).testTag("SecondaryGoalCardText")
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(26.dp)
                                    ,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = "${goal.start_date.substringBefore("T")} to ${
                                            goal.end_date.substringBefore(
                                                "T"
                                            )
                                        }",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.secondary,
                                        textAlign = TextAlign.Right,
                                        modifier = Modifier.padding(top = 3.dp)
                                    )
                                    Text(
                                        text = "+ ${if (goal.period == 7) 5 else 20 } xp",
                                        fontWeight = FontWeight.Bold,
                                        color = secondTextColor,
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        @Composable
        fun goalHeader() {
            Row(modifier = Modifier
                .fillMaxWidth()
                .testTag("GoalHeader")
                ,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "Goals:",
                    color = mainTextColor,
                    style = Typography.titleLarge,
                    modifier = Modifier
                        .padding(start = 16.dp, top = 16.dp)
                )
                TextButton(
                    shape = CircleShape,
                    modifier = Modifier
                        .size(50.dp)
                        .padding(end = 12.dp, top = 12.dp)
                        .testTag("AddGoalButton")
                    ,
                    contentPadding = PaddingValues(0.dp),
                    onClick = {
                        dashboardNavController.navigate("addGoalScreen/${(-1)}")
                    },
                ) {
                    Icon(
                        imageVector = Icons.Filled.AddToPhotos,
                        contentDescription = "Add New Deal",
                        modifier = Modifier.size(40.dp),
                        tint = mainTextColor
                    )
                }
            }
            Row(modifier = Modifier
                .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween) {
                Row(modifier = Modifier
                    .padding(start=12.dp, end=12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Favorite",
                        modifier = Modifier.size(35.dp),
                        tint = positiveGreen
                    )
                    Text(
                        text = ": ${goalStats?.completed}",
                        color = secondTextColor,
                        style = Typography.titleMedium,
                        modifier = Modifier
                            .padding(start = 16.dp)
                    )
                }
                Row(modifier = Modifier
                    .padding(start=12.dp, end=15.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Icon(
                        imageVector = Icons.Filled.Circle,
                        contentDescription = "Favorite",
                        modifier = Modifier.size(35.dp),
                        tint = neutralOrange
                    )
                    Text(
                        text = ": ${goalStats?.incompleted}",
                        color = secondTextColor,
                        style = Typography.titleMedium,
                        modifier = Modifier
                            .padding(start = 16.dp)
                    )
                }
                Row(modifier = Modifier
                    .padding(start=12.dp, end=15.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Favorite",
                        modifier = Modifier.size(35.dp),
                        tint = negativeRed
                    )
                    Text(
                        text = ": ${goalStats?.failed}",
                        color = secondTextColor,
                        style = Typography.titleMedium,
                        modifier = Modifier
                            .padding(start = 16.dp)
                    )
                }
            }
        }

        @Composable
        fun GoalSearchBar(
            searchQuery: String,
            onSearchQueryChange: (String) -> Unit,
        ) {

            Column(modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp)) {
                // Text field for note search.
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    label = { Text("Search...") },
                    modifier = Modifier.fillMaxWidth().testTag("GoalSearchBar"),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = secondTextColor,
                        unfocusedIndicatorColor = mainTextColor
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        @Suppress("UNCHECKED_CAST")
        fun preloadData() {
            coroutineScope.launch {
                errorMessage = ""
                var spendingResponse: Response<SpendingSummaryResponse>? = null
                var levelResponse: Response<LevelRequest>? = null
                var categoriesResponse: Response<List<Category>>? = null
                var goalsResponse: Response<GoalRetrievalResponse>? = null
                var responses: List<Response<out Any>>
                if (useMockApi != "false") {
                    if (useMockApi == "slowdown") {
                        delay(2000)
                    }
                    try {
                        if (createMockApiRequests != null) {
                            if (createMockApiRequests.size == 4) {
                                spendingResponse = createMockApiRequests[0] as Response<SpendingSummaryResponse>
                                levelResponse = createMockApiRequests[1] as Response<LevelRequest>
                                categoriesResponse = createMockApiRequests[2] as Response<List<Category>>
                                goalsResponse = createMockApiRequests[3] as Response<GoalRetrievalResponse>

                                currentDate = LocalDateTime.now().withYear(2025).withMonth(3).withDayOfMonth(28)
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
                        val spendingDeferred = async(Dispatchers.IO) {
                            RetrofitInstance.apiService.getSpendingSummary(
                                firstDayOfMonth,
                                lastDayOfMonth
                            )
                        }
                        val levelDeferred = async(Dispatchers.IO) {
                            RetrofitInstance.apiService.getLevel()
                        }
                        val categoriesDeferred = async(Dispatchers.IO) {
                            RetrofitInstance.apiService.getCategories()
                        }
                        val goalsDeferred = async(Dispatchers.IO) {
                            RetrofitInstance.apiService.getGoals(firstDayOfMonth, lastDayOfMonth)
                        }

                        // Await all calls to complete
                        withTimeout(5000) {
                            responses = awaitAll(
                                spendingDeferred,
                                levelDeferred,
                                categoriesDeferred,
                                goalsDeferred
                            )
                        }

                        // Assign responses to the outer scope variables
                        spendingResponse = responses[0] as Response<SpendingSummaryResponse>
                        levelResponse = responses[1] as Response<LevelRequest>
                        categoriesResponse = responses[2] as Response<List<Category>>
                        goalsResponse = responses[3] as Response<GoalRetrievalResponse>
                    } catch (e: Exception) {
                        errorMessage += "Failed to load user data.\n"
                        Log.d("Dashboard Error", "Error Calling API: ${e.message}")
                        isLoading = false
                    }
                }
                try {
                    if (spendingResponse != null) {
                        if (spendingResponse.isSuccessful) {
                            val responseBody = spendingResponse.body() as SpendingSummaryResponse
                            assignSpendingResponse(responseBody)
                        } else {
                            errorMessage += "Failed to load expense data.\n"
                            Log.d("Dashboard Error", "Summary Spend API Response Was Unsuccessful: $spendingResponse")
                        }
                    }

                    if (levelResponse != null) {
                        if (levelResponse.isSuccessful) {
                            levelStats = levelResponse.body() as LevelRequest
                        } else {
                            errorMessage += "Failed to load levels.\n"
                            Log.d("Dashboard Error", "Levels API Response Was Unsuccessful: $levelResponse")
                        }
                    }

                    if (categoriesResponse != null) {
                        if (categoriesResponse.isSuccessful) {
                            categories = categoriesResponse.body() as List<Category>
                        } else {
                            errorMessage += "Failed to load categories.\n"
                            Log.d("Dashboard Error", "Categories API Response Was Unsuccessful: $categories")
                        }
                    }

                    if (goalsResponse != null) {
                        if (goalsResponse.isSuccessful) {
                            val responseBody = goalsResponse.body() as GoalRetrievalResponse
                            assignGoalResponse(responseBody)
                        } else {
                            errorMessage += "Failed to load goals.\n"
                            Log.d("Dashboard Error", "Goals API Response Was Unsuccessful: $goalsResponse")
                        }
                    }

                } catch (e: Exception) {
                    errorMessage += "Error Assigning User Data.\n"
                    Log.d("Dashboard Error", "Error Assigning API results: ${e.message}")
                    isLoading = false
                }
            }

        }
        // Definitions end here

        // Preload all data
        LaunchedEffect(Unit) {
            isLoading = true
            Log.d("Token", "Token: $UserSession.access_token")
            preloadData()
        }

        // Actual visuals for home page
        if(categories != null && levelStats != null && spendingSummary != null && listOfGoals != null) {
            isLoading = false
        }
        if(errorMessage != "") {
            isLoading = false
        }

        when {
            isLoading -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(mainBackgroundColor)
                        .height(900.dp)
                        .testTag("LoadingSpinner"),
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
            errorMessage != "" -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(mainBackgroundColor)
                        .height(600.dp)
                        .testTag("ErrorMessage"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                )
                {
                    Text(text = errorMessage, color = MaterialTheme.colorScheme.error, style = Typography.headlineSmall)
                    Button(onClick = { preloadData() }, colors = ButtonDefaults.buttonColors(containerColor = mainTextColor)) { Text("Retry") }
                }
            }

            else -> {
                Scaffold(
                    modifier = Modifier
                        .testTag("Scaffold"),
                    topBar = {
                        CenterAlignedTopAppBar(
                            navigationIcon = {
                                IconButton(onClick = {
                                    coroutineScope.launch {
                                        drawerState.open()
                                    }
                                }) {
                                    Icon(Icons.Filled.Menu, contentDescription = "Menu Button")
                                }
                            },
                            title = {
                                Text(
                                    monthName.uppercase(),
                                    style = Typography.headlineSmall,
                                    color = mainTextColor
                                )
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = mainBackgroundColor)
                        )
                    },
                ) { padding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(mainBackgroundColor)
                            .verticalScroll(scrollState)
                            .padding(padding)
                            .testTag("Scrollbar"),
                        verticalArrangement = Arrangement.Top
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(start = 16.dp, top = 8.dp).testTag("TotalSpending")
                        ) {
                            Text(
                                text = "$",
                                style = Typography.titleMedium,
                                color = Pink40,
                                modifier = Modifier
                                    .padding(end = 6.dp)
                            )
                            Text(
                                text = "${totalSpending.toInt()}",
                                style = TextStyle(
                                    fontFamily = FontFamily.Default,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 40.sp,
                                    lineHeight = 24.sp,
                                    letterSpacing = 0.sp
                                ),
                                color = mainTextColor
                            )
                            Text(
                                text = ".${((totalSpending - totalSpending.toInt()) * 100).toInt()}",
                                style = Typography.titleMedium,
                                color = Pink40
                            )
                        }
                        Box(
                            modifier = Modifier
                                .height(410.dp)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Column(
                                modifier = Modifier
                                    .height(340.dp)
                                    .testTag("CatGif"),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            )
                            {
                                GifPlayer(context = LocalContext.current, playPetAnimation)
                            }
                            if (spendingSummary != null) {
                                Piechart(spendingSummary!!)
                            }
                            Column(
                                modifier = Modifier
                                    .height(340.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            )
                            {
                                // putting launched effect here makes sure animation cancels before starting new one
                                LaunchedEffect(playPetAnimation) {
                                    if (playPetAnimation) {
                                        delay(2000)
                                        playPetAnimation = false
                                    }
                                }
                                Button(
                                    modifier = Modifier
                                        .height(150.dp)
                                        .testTag("CatGifButton")
                                    ,
                                    onClick = {
                                        playPetAnimation = true
                                              },
                                    colors = ButtonDefaults.buttonColors(containerColor = transparencyColor)
                                ) {
                                    Text(text = "xxxx xxxx", modifier = Modifier.padding(0.dp), color = transparencyColor)
                                }
                            }
                            if(levelStats != null) {
                                LevelBar(levelStats)
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Button(
                                modifier = Modifier
                                    .padding(0.dp)
                                    .testTag("ToggleSpendingGoalsButton")
                                ,
                                onClick = {
                                    viewSpendingOrGoals = if(viewSpendingOrGoals == "View Goals") {
                                        "View Spending"
                                    } else {
                                        "View Goals"
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = mainTextColor)
                            ) {
                                Text(text = viewSpendingOrGoals, modifier = Modifier.padding(0.dp))
                            }

                        }
                        if(viewSpendingOrGoals == "View Goals") {
                            for(expense in spendingSummary!!) {
                                val decoratedCategoryBreakdown = CategoryBreakdownDecorator(expense)
                                ExpenseCategoryCard(decoratedCategoryBreakdown)
                            }
                        } else {
                            if (deleteConfirmationDialogue) {
                                AlertDialog(
                                    onDismissRequest = { deleteConfirmationDialogue = false },
                                    title = { Text("Are you sure?") },
                                    text = { Text("Do you really want to delete?") },
                                    confirmButton = {
                                        val context = LocalContext.current
                                        TextButton(onClick = { onConfirm(idToDelete, context) }, colors = ButtonDefaults.buttonColors(containerColor = mainTextColor),  modifier = Modifier.testTag("ConfirmDeleteDialog")) {
                                            Text("Proceed")
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { deleteConfirmationDialogue = false }, colors = ButtonDefaults.buttonColors(containerColor = mainTextColor)) {
                                            Text("Cancel")
                                        }
                                    }
                                )
                            }
                            goalHeader()
                            GoalSearchBar(
                                searchQuery = searchQuery,
                                onSearchQueryChange = { searchQuery = it },
                            )
                            LaunchedEffect(listOfGoals, searchQuery) {
                                filteredSearchlistOfGoals = listOfGoals!!.filter { goal ->
                                    val matchesQuery = if (searchQuery.isNotBlank())
                                        goal.category_string?.contains(searchQuery, ignoreCase = true)
                                    else true
                                    matchesQuery == true
                                }
                            }
                            for(goal in listOfGoals!!) {
                                if (goal in filteredSearchlistOfGoals) {
                                    goalCard(goal)
                                }
                            }
                        }
                        Spacer(Modifier.height(85.dp).testTag("BottomDashboard"))
                    }
                }
            }
        }
    }

}
