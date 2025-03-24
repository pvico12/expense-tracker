package com.cs446.expensetracker.ui.dashboard

import android.app.DatePickerDialog
import android.icu.text.DecimalFormat
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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavController
import com.cs446.expensetracker.api.models.GoalRetrievalResponse
import com.cs446.expensetracker.api.models.GoalRetrievalStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import coil.compose.AsyncImage
import com.cs446.expensetracker.api.models.DealRetrievalResponse
import com.cs446.expensetracker.api.models.LevelRequest
import com.cs446.expensetracker.mockData.formatCurrency
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


class Dashboard {

    private val default_colors = arrayOf("#FF9A3B3B", "#FFC08261", "#FFDBAD8C", "#FFDBAD8C", "#FFFFEBCF", "#FFFFCFAC", "#FFFFDADA", "#FFD6CBAF", "#FF8D5F2E")

    @OptIn(ExperimentalMaterial3Api::class)
    @RequiresApi(Build.VERSION_CODES.O)
    @Composable
    fun DashboardScreen(
        drawerState: DrawerState,
        dashboardNavController: NavController
    ) {
        // variable definitions
        val scrollState = rememberScrollState()
        // needed for coroutines, cache scope
        val coroutineScope = rememberCoroutineScope()
        var errorMessage by remember { mutableStateOf("") }
        var isLoading by remember { mutableStateOf(true) }
        // for spending summary
        var spendingSummary by remember { mutableStateOf<List<CategoryBreakdown>>(emptyList()) }
        var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
        var totalSpending by remember { mutableDoubleStateOf(0.0) }
        // for goals editing
        var deleteConfirmationDialogue by remember { mutableStateOf(false)}
        var idToDelete by remember { mutableIntStateOf(-1) }
        // for goals
        var viewSpendingOrGoals by rememberSaveable { mutableStateOf("View Goals") }
        var listOfGoals by remember { mutableStateOf<List<GoalRetrievalGoals>>(emptyList()) }
        var goalStats by remember { mutableStateOf<GoalRetrievalStats?>(null) }
        // for levels
        var levelStats by remember { mutableStateOf<LevelRequest?>(null) }
        var playPetAnimation by remember { mutableStateOf<Boolean>(false) }
        // for search
        var searchQuery by remember { mutableStateOf("") }
        var filteredSearchlistOfGoals by remember { mutableStateOf<List<GoalRetrievalGoals>>(emptyList())}
        // for Scaffold
        val currentDate = LocalDateTime.now()
        val monthName = currentDate.format(DateTimeFormatter.ofPattern("MMMM"))
        // for api calls
        val firstDayOfMonth = currentDate.withDayOfMonth(1).format(DateTimeFormatter.ISO_DATE_TIME)
        val lastDayOfMonth = currentDate.withDayOfMonth(currentDate.toLocalDate().lengthOfMonth()).format(DateTimeFormatter.ISO_DATE_TIME)

        fun apiFetchGoals(startDate: String, endDate: String) {
            coroutineScope.launch {
                try {
                    val token = UserSession.access_token ?: ""
                    val response: Response<GoalRetrievalResponse> =
                        RetrofitInstance.apiService.getGoals()
                    Log.d("Response", "Fetch Goals API Request actually called")
                    if (response.isSuccessful) {
                        val responseBody = response.body()
                        Log.d("Response", "Goals Response: $responseBody")
                        goalStats = responseBody?.stats
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
                                amount_spent = x.amount_spent,
                            )
                        } ?: emptyList()

                        for (goal in listOfGoals) {
                            goal.category_string = categories.find { it.id == goal.category_id }?.name ?: "Deleted Category"
                        }

                    } else {
                        errorMessage = "Failed to load data."
                        Log.d("Error", "Goals API Response Was Unsuccessful: $response")
                    }
                } catch (e: Exception) {
                    errorMessage = "Error: ${e.message}"
                    Log.d("Error", "Error Calling Goals API: $errorMessage")
                }
            }
        }

        // function definitions

        fun onConfirm(id: Int, context: Context) {
            deleteConfirmationDialogue = false
            coroutineScope.launch {
                isLoading = true
                try {
                    val token = UserSession.access_token ?: ""
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
                    apiFetchGoals(firstDayOfMonth, lastDayOfMonth)
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
                    .padding(vertical = 8.dp, horizontal = 8.dp),
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
                        modifier = Modifier.size(30.dp),
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
                        modifier = Modifier.size(30.dp),
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
                    var main_goal_text: String
                    var secondary_goal_text: String
                    var period_length = "week"
                    if (goal.period != 7) {
                        period_length = "month"
                    }
                    if(goal.goal_type == "amount") {
                        main_goal_text = "Spend less than $" + formatCurrency(goal.limit) + " on ${goal.category_string}"
                        secondary_goal_text = "$${formatCurrency(goal.amount_spent)} amount spent this $period_length so far"
                    } else {
                        main_goal_text = "Spend " + formatCurrency(goal.limit) + "% less than last $period_length on ${goal.category_string}"
                        secondary_goal_text = "${formatCurrency(goal.amount_spent)}% less spent than last $period_length so far"
                    }
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = main_goal_text, fontWeight = FontWeight.Bold, color= mainTextColor, style = Typography.titleSmall)
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            var icon: ImageVector
                            var color: Color
                            val formatter = DateTimeFormatter.ISO_DATE_TIME
                            val dateTime = LocalDateTime.parse(goal.end_date, formatter)
                            val currentDateTime = LocalDateTime.now()

                            if (goal.on_track && dateTime.isBefore(currentDateTime)) {
                                icon = Icons.Filled.Check
                                color = positiveGreen
                            } else if(goal.on_track) {
                                icon = Icons.Filled.Circle
                                color = neutralOrange
                            } else {
                                icon = Icons.Filled.Close
                                color = negativeRed
                            }
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
                                        tint = color
                                    )
                                }
                                Text(
                                    text = secondary_goal_text,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.secondary,
                                    textAlign = TextAlign.Left,
                                    modifier = Modifier.padding(top=2.dp)
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
                .fillMaxWidth(),
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
                        .padding(end = 12.dp, top = 12.dp),
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
                        text = ": ${goalStats?.in_progress}",
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
                        text = ": ${goalStats?.incompleted}",
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
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = secondTextColor,
                        unfocusedIndicatorColor = mainTextColor
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        fun preloadData() {
            coroutineScope.launch {
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
                        RetrofitInstance.apiService.getGoals()
                    }

                    // Await all calls to complete
                    val (spendingResponse, levelResponse, categoriesResponse, goalsResponse) =
                        awaitAll(spendingDeferred, levelDeferred, categoriesDeferred, goalsDeferred)

                    if (spendingResponse.isSuccessful) {
                        val responseBody = spendingResponse.body() as SpendingSummaryResponse
                        totalSpending = responseBody.total_spend ?: 0.0
                        Log.d("Response", "Summary Spend Response: $responseBody for $firstDayOfMonth to $lastDayOfMonth")
                        spendingSummary = responseBody.category_breakdown.map { x ->
                            CategoryBreakdown(
                                category_name = x.category_name,
                                total_amount = x.total_amount,
                                percentage = x.percentage,
                                custom_color = null //TODO: change backend
                            )
                        } ?: emptyList()
                        var color_iter = -1
                        for (expense in spendingSummary) {
                            if (expense.custom_color == null) {
                                if (color_iter < default_colors.size) {
                                    color_iter += 1
                                } else {
                                    color_iter = 0
                                }
                                expense.custom_color = default_colors[color_iter]
                            }
                        }
                    } else {
                        errorMessage += "Failed to load expense data."
                        Log.d("Error", "Summary Spend API Response Was Unsuccessful: $spendingResponse")
                    }

                    if (levelResponse.isSuccessful) {
                        levelStats = levelResponse.body() as LevelRequest
                    } else {
                        errorMessage += "Failed to load levels."
                        Log.d("Error", "Levels API Response Was Unsuccessful: $levelResponse")
                    }

                    if (categoriesResponse.isSuccessful) {
                        categories = categoriesResponse.body() as List<Category> ?: emptyList()
                    } else {
                        errorMessage += "Failed to load categories."
                        Log.d("Error", "Categories API Response Was Unsuccessful: $categories")
                    }

                    if (goalsResponse.isSuccessful) {
                        val responseBody = goalsResponse.body() as GoalRetrievalResponse
                        Log.d("Response", "Goals Response: $responseBody")
                        goalStats = responseBody?.stats
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
                                amount_spent = x.amount_spent,
                            )
                        } ?: emptyList()

                        for (goal in listOfGoals) {
                            goal.category_string = categories.find { it.id == goal.category_id }?.name ?: "Deleted Category"
                        }

                    } else {
                        errorMessage = "Failed to load data."
                        Log.d("Error", "Goals API Response Was Unsuccessful: $goalsResponse")
                    }


                } catch (e: Exception) {
                    errorMessage += "Error Fetching User Data"
                    Log.d("Error", "Error Calling Summary Spend API: ${e.message}")
                    isLoading = false
                }
            }

        }
        // Definitions end here

        // Preload all data
        LaunchedEffect(Unit) {
            isLoading = true
            val token = UserSession.access_token ?: ""
            preloadData()
        }

        // Actual visuals for home page
        if(categories.isNotEmpty() && levelStats != null && spendingSummary.isNotEmpty() && listOfGoals.isNotEmpty()) {
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
                        .height(600.dp),
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
                        .height(600.dp),
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
                            .padding(padding),
                        verticalArrangement = Arrangement.Top
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(start = 16.dp, top = 8.dp)
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
                                    .height(340.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            )
                            {
                                GifPlayer(context = LocalContext.current, playPetAnimation)
                            }
                            Piechart(spendingSummary)
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
                                        .height(150.dp),
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
                                    .padding(0.dp),
                                onClick = {
                                    if(viewSpendingOrGoals == "View Goals") {
                                        viewSpendingOrGoals = "View Spending"
                                    } else {
                                        viewSpendingOrGoals = "View Goals"
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = mainTextColor)
                            ) {
                                Text(text = viewSpendingOrGoals, modifier = Modifier.padding(0.dp))
                            }

                        }
                        if(viewSpendingOrGoals == "View Goals") {
                            for(expense in spendingSummary) {
                                expenseCategoryCard(expense)
                            }
                        } else {
                            if (deleteConfirmationDialogue) {
                                AlertDialog(
                                    onDismissRequest = { deleteConfirmationDialogue = false },
                                    title = { Text("Are you sure?") },
                                    text = { Text("Do you really want to delete?") },
                                    confirmButton = {
                                        val context = LocalContext.current
                                        TextButton(onClick = { onConfirm(idToDelete, context) }, colors = ButtonDefaults.buttonColors(containerColor = mainTextColor)) {
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
                                filteredSearchlistOfGoals = listOfGoals.filter { goal ->
                                    val matchesQuery = if (searchQuery.isNotBlank())
                                        goal.category_string?.contains(searchQuery, ignoreCase = true)
                                    else true
                                    matchesQuery == true
                                }
                            }
                            for(goal in listOfGoals) {
                                if (goal in filteredSearchlistOfGoals) {
                                    goalCard(goal)
                                }
                            }
                        }
                        Spacer(Modifier.height(85.dp))
                    }
                }
            }
        }
    }

}
