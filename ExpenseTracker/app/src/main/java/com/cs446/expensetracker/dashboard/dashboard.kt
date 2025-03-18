package com.cs446.expensetracker.dashboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.icu.text.DecimalFormat
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddToPhotos
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.cs446.expensetracker.api.RetrofitInstance
import com.cs446.expensetracker.api.models.Transaction
import com.cs446.expensetracker.mockData.dashboard_mock_expense
import com.cs446.expensetracker.api.models.Category
import com.cs446.expensetracker.api.models.CategoryBreakdown
import com.cs446.expensetracker.api.models.DealLocation
import com.cs446.expensetracker.api.models.DealRetrievalRequestWithLocation
import com.cs446.expensetracker.api.models.DealRetrievalResponse
import com.cs446.expensetracker.api.models.GoalRetrievalGoals
import com.cs446.expensetracker.api.models.GoalRetrievalResponse
import com.cs446.expensetracker.api.models.SpendingSummaryResponse
import com.cs446.expensetracker.api.models.TransactionResponse
import com.cs446.expensetracker.session.UserSession
import com.cs446.expensetracker.ui.ui.theme.*
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.MPPointF
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.round

class Dashboard {

    private fun formatCurrency(amount: Double): String {
        if (amount == 0.0) { return "0.00"}
        val format = DecimalFormat("#,###.00")
        return format.format(amount)
    }

    private val default_colors = arrayOf("#FF9A3B3B", "#FFC08261", "#FFDBAD8C", "#FFDBAD8C", "#FFFFEBCF", "#FFFFCFAC", "#FFFFDADA", "#FFD6CBAF", "#FF8D5F2E")

    @RequiresApi(Build.VERSION_CODES.O)
    @Composable
    fun DashboardHost(dashboardNavController: NavController) {
        val scrollState = rememberScrollState()
        val coroutineScope = rememberCoroutineScope()
        var spendingSummary by remember { mutableStateOf<List<CategoryBreakdown>>(emptyList()) }
        var totalSpending by remember { mutableDoubleStateOf(0.0) }
        var errorMessage = ""
        var isLoading by remember { mutableStateOf(true) }

        var viewSpendingOrGoals by rememberSaveable { mutableStateOf("View Goals") }

        val currentDate = LocalDateTime.now().plusHours(4) // plus 4 for utv
        val stringCurrentDate = currentDate.format(DateTimeFormatter.ISO_DATE_TIME)
        val oneYearAgo = currentDate.minusMonths(1)
        val stringOneYearAgoDate = oneYearAgo.format(DateTimeFormatter.ISO_DATE_TIME)

        var listOfGoals by remember { mutableStateOf<List<GoalRetrievalGoals>>(emptyList()) }

        var deleteConfirmationDialogue by remember { mutableStateOf(false)}
        var idToDelete by remember { mutableStateOf(-1)}

        // Load transactions via API
        LaunchedEffect(Unit) {
            isLoading = true
            errorMessage = ""
            try {
                val token = UserSession.access_token ?: ""
                val response: Response<SpendingSummaryResponse> =
                    RetrofitInstance.apiService.getSpendingSummary(startDate = stringOneYearAgoDate, endDate = stringCurrentDate)
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    totalSpending = responseBody?.total_spend ?: 0.0
                    Log.d("Response", "Summary Spend Response: $responseBody for $stringOneYearAgoDate to $stringCurrentDate")
                    spendingSummary = responseBody?.category_breakdown?.map { x ->
                        CategoryBreakdown(
                            category_name = x.category_name,
                            total_amount = x.total_amount.toDouble(),
                            percentage = x.percentage.toDouble(),
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
                    errorMessage = "Failed to load data."
                    Log.d("Error", "Summary Spend API Response Was Unsuccessful: $response")
                }
            } catch (e: Exception) {
                errorMessage = "Error: ${e.message}"
                Log.d("Error", "Error Calling Summary Spend API: $errorMessage")
            } finally {
                isLoading = false
            }
        }

        Log.d("FCM Token", UserSession.fcmToken)

        fun apiFetchGoals(startDate: String, endDate: String) {
            // Load deals via API
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
            }
        }

        fun onConfirm(id: Int, context: Context) {
            deleteConfirmationDialogue = false
            CoroutineScope(Dispatchers.IO).launch {
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
                    apiFetchGoals(stringOneYearAgoDate, stringCurrentDate)
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

        fun onEditButtonClick(id: Int) {
            dashboardNavController.navigate("addGoalScreen/$id")
        }
        fun onDeleteButtonClick(id: Int) {
            idToDelete = id
            deleteConfirmationDialogue = true
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(mainBackgroundColor)
                .verticalScroll(scrollState)
            ,
            verticalArrangement = Arrangement.Top
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(0.dp),
            ) {
                Text(
                    text = "Home Dashboard",
                    color = mainTextColor,
                    style = Typography.titleLarge,
                    modifier = Modifier
                        .padding(start = 16.dp, top = 16.dp)
                )
                Spacer(Modifier.weight(1f))
                TextButton(
                    onClick = { /* ... */ },
                    shape = CircleShape,
                    modifier = Modifier
                        .size(70.dp)
                        .padding(bottom = 5.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Favorite",
                        modifier = Modifier.size(45.dp),
                        tint = mainTextColor
                    )
                }
            }
            when {
                isLoading -> {
                    CircularProgressIndicator()
                }
                errorMessage != "" -> {
                    Text(text = errorMessage ?: "", color = MaterialTheme.colorScheme.error)
                }
                else -> {
                    Piechart(spendingSummary)
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
                        Text(
                            text = "This Month's Expenses:",
                            color = mainTextColor,
                            style = Typography.titleLarge,
                            modifier = Modifier
                                .padding(start = 16.dp, top = 16.dp)
                        )
                        Text(
                            text = "Total Spending: $${formatCurrency(totalSpending)}",
                            color = mainTextColor,
                            style = Typography.titleLarge,
                            fontSize = 27.sp,
                            modifier = Modifier
                                .padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
                        )
                        for(expense in spendingSummary) {
                            expenseCategoryCard(expense)
                        }
                    } else {
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
                        apiFetchGoals(stringOneYearAgoDate, stringCurrentDate)
                        Text(
                            text = "Total Spending: $${formatCurrency(totalSpending)}",
                            color = mainTextColor,
                            style = Typography.titleLarge,
                            fontSize = 27.sp,
                            modifier = Modifier
                                .padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
                        )
                        for((i, goal) in listOfGoals.withIndex()) {
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
                                            tint = Color(0xFF9D9D9D)
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
                                            tint = Color(0xFF9D9D9D)
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
                                    var type_of_goal_string = ""
                                    if(goal.goal_type == "amount") {
                                        type_of_goal_string = "$" + formatCurrency(goal.limit)
                                    } else {
                                        type_of_goal_string = formatCurrency(goal.limit) + "%"
                                    }
                                    Column {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(text ="Spend less than ${type_of_goal_string} on Entertainment", fontWeight = FontWeight.Bold, color= mainTextColor, style = Typography.titleSmall)
                                        }
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                        ) {
                                            Text(text = "Time Left: ${goal.period} days", fontWeight = FontWeight.Bold, color= secondTextColor, modifier = Modifier.padding(end=14.dp), style = MaterialTheme.typography.titleLarge)
                                            Spacer(modifier = Modifier.weight(2f))
                                        }
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                        ) {
                                            Text(
                                                text = "   Current Spending this week: $54.39",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.secondary,
                                                textAlign = TextAlign.Right,
                                                modifier = Modifier.padding(top=2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                    }
                    Spacer(Modifier.height(85.dp))
                }
            }
        }
    }

    @Composable
    private fun Piechart(spendingSummary: List<CategoryBreakdown>) {
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp)
                .padding(16.dp),
            factory = { context ->
                PieChart(context).apply {
                    // Customize the PieChart here
                    setExtraOffsets(5f, 10f, 5f, 5f)
                    setDragDecelerationFrictionCoef(0.95f)
                    getDescription().setEnabled(false)

                    setDrawHoleEnabled(true)
                    setHoleColor(mainBackgroundColor.toArgb())
                    setTransparentCircleColor(tileColor.toArgb())
                    setTransparentCircleAlpha(110)
                    setHoleRadius(61f)
                    setTransparentCircleRadius(70f)
                    setDrawCenterText(true)

                    // rotation of the pieChart by touch
                    setRotationEnabled(true)
                    setRotationAngle(0f)
                    setHighlightPerTapEnabled(true)

                    animateY(1400, Easing.EaseInOutQuad)

                    legend.isEnabled = false

                    val colors: ArrayList<Int> = ArrayList()

                    // on below line we are creating array list and
                    // adding data to it to display in pie chart
                    val entries: ArrayList<PieEntry> = ArrayList()
                    for(expense in spendingSummary) {
                        entries.add(PieEntry(expense.percentage.toFloat(), expense.category_name))
                        colors.add(Color(android.graphics.Color.parseColor((expense.custom_color))).toArgb())
                    }

                    setNoDataText("Add your first transactions to see the pie chart")

                    // on below line we are setting pie data set
                    val dataSet = PieDataSet(entries, "Mobile OS")

                    dataSet.setDrawIcons(true)
                    dataSet.iconsOffset = MPPointF(0f, 40f)
                    dataSet.selectionShift = 5f
                    dataSet.sliceSpace = 3f

                    // on below line we are setting colors.
                    dataSet.colors = colors

                    // on below line we are setting pie data set
                    val data = PieData(dataSet)
                    data.setValueFormatter(categoryValueFormatter())
                    data.setValueTextSize(1f)
                    setEntryLabelColor(Color.Black.toArgb())
                    setEntryLabelTextSize(18f)
                    setEntryLabelTypeface(Typeface.DEFAULT_BOLD)
                    data.setValueTextColor(Color.Black.toArgb())
                    data.setValueTypeface(Typeface.DEFAULT_BOLD)
                    setData(data)


                }
            },
            update = { pieChart ->
                // refresh
                pieChart.invalidate()
            }
        )
    }

    class categoryValueFormatter : ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            return ""
        }

        fun drawBackground(canvas: Canvas, label: String, x: Float, y: Float, paint: Paint) {
            val padding = 8f
            val backgroundRect = RectF(x - padding, y - padding, x + paint.measureText(label) + padding, y + paint.textSize + padding)
            paint.color = 0xFFDDDDDD.toInt() // Background color
            canvas.drawRect(backgroundRect, paint)
        }
    }

}
