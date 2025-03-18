package com.cs446.expensetracker.dashboard

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.icu.text.DecimalFormat
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.cs446.expensetracker.api.RetrofitInstance
import com.cs446.expensetracker.mockData.dashboard_mock_expense
import com.cs446.expensetracker.api.models.Category
import com.cs446.expensetracker.api.models.CategoryBreakdown
import com.cs446.expensetracker.api.models.DealRetrievalResponse
import com.cs446.expensetracker.api.models.SpendingSummaryResponse
import com.cs446.expensetracker.session.UserSession
import com.cs446.expensetracker.ui.ui.theme.*
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.MPPointF
import kotlinx.coroutines.launch
import retrofit2.Response
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.round
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerState
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

class Dashboard {

    private fun formatCurrency(amount: Double): String {
        if (amount == 0.0) { return "0.00"}
        val format = DecimalFormat("#,###.00")
        return format.format(amount)
    }

    private val default_colors = arrayOf("#FF9A3B3B", "#FFC08261", "#FFDBAD8C", "#FFDBAD8C", "#FFFFEBCF", "#FFFFCFAC", "#FFFFDADA", "#FFD6CBAF", "#FF8D5F2E")

    @OptIn(ExperimentalMaterial3Api::class)
    @RequiresApi(Build.VERSION_CODES.O)
    @Composable
    fun DashboardScreen(
        drawerState: DrawerState
    ) {
        val scrollState = rememberScrollState()
        val coroutineScope = rememberCoroutineScope()
        var spendingSummary by remember { mutableStateOf<List<CategoryBreakdown>>(emptyList()) }
        var totalSpending by remember { mutableDoubleStateOf(0.0) }
        var errorMessage = ""
        var isLoading by remember { mutableStateOf(true) }
        val currentDate = LocalDateTime.now()
        val monthName = currentDate.format(DateTimeFormatter.ofPattern("MMMM"))

        // Load transactions via API
        LaunchedEffect(Unit) {
            isLoading = true
            errorMessage = ""
            try {
                val token = UserSession.access_token ?: ""
                val firstDayOfMonth = currentDate.withDayOfMonth(1).format(DateTimeFormatter.ISO_DATE_TIME)
                val lastDayOfMonth = currentDate.withDayOfMonth(currentDate.toLocalDate().lengthOfMonth()).format(DateTimeFormatter.ISO_DATE_TIME)
                val response: Response<SpendingSummaryResponse> =
                    RetrofitInstance.apiService.getSpendingSummary(startDate = firstDayOfMonth, endDate = lastDayOfMonth)
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    totalSpending = responseBody?.total_spend ?: 0.0
                    Log.d("Response", "Summary Spend Response: $responseBody for $firstDayOfMonth to $lastDayOfMonth")
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

        val dollars = totalSpending.toInt()
        val cents = ((totalSpending - dollars) * 100).toInt()

        when {
            isLoading -> {
                CircularProgressIndicator()
            }
            errorMessage != "" -> {
                Text(text = errorMessage ?: "", color = MaterialTheme.colorScheme.error)
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
                                    Icon(Icons.Filled.Menu, contentDescription = "")
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
                                text = "$dollars",
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
                                text = ".$cents",
                                style = Typography.titleMedium,
                                color = Pink40
                            )
                        }
                        Piechart(spendingSummary)
                        for (expense in spendingSummary) {
                            Card(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .fillMaxSize(),
                                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                                colors = CardDefaults.cardColors(containerColor = tileColor)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .padding(
                                                start = 10.dp,
                                                top = 10.dp,
                                                bottom = 10.dp,
                                                end = 10.dp
                                            )
                                            .clip(CircleShape)
                                            .background(Color(android.graphics.Color.parseColor((expense.custom_color))))
                                            .align(Alignment.CenterStart)
                                    )
                                    Text(
                                        text = "${expense.category_name}: ",
                                        color = mainTextColor,
                                        style = Typography.labelSmall,
                                        modifier = Modifier
                                            .padding(
                                                start = 40.dp,
                                                top = 8.dp,
                                                bottom = 8.dp,
                                                end = 8.dp
                                            )
                                    )
                                    Spacer(Modifier.fillMaxWidth(0.2f))
                                    Text(
                                        text = "${round(expense.percentage)}%",
                                        color = PurpleGrey40,
                                        style = Typography.labelSmall,
                                        modifier = Modifier
                                            .padding(8.dp)
                                            .align(Alignment.Center)
                                    )
                                    Text(
                                        text = "$${formatCurrency(expense.total_amount)}",
                                        color = PurpleGrey40,
                                        style = Typography.labelSmall,
                                        modifier = Modifier
                                            .padding(
                                                start = 1.dp,
                                                top = 8.dp,
                                                bottom = 8.dp,
                                                end = 20.dp
                                            )
                                            .align(Alignment.CenterEnd)
                                    )
                                }

                            }
                        }
                        Spacer(Modifier.height(85.dp))
                    }
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
                    setExtraOffsets(5f, 0f, 5f, 5f)
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
