package com.cs446.expensetracker.dashboard

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.icu.text.DecimalFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.cs446.expensetracker.models.Category
import com.cs446.expensetracker.models.Transaction
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

class Dashboard {
    val format = DecimalFormat("#,###.00")

    @Composable
    fun DashboardHost() {
        val scrollState = rememberScrollState()
        val coroutineScope = rememberCoroutineScope()
        var transactions by remember { mutableStateOf<List<Transaction>>(emptyList()) }
        var errorMessage = ""

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
                    modifier = Modifier.size(70.dp).padding(bottom=5.dp),
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
            Piechart()
            Text(
                text = "This Month's Expenses",
                color = mainTextColor,
                style = Typography.titleLarge,
                modifier = Modifier
                    .padding(start = 16.dp, top = 16.dp)
            )
            Text(
                text = "Total Spending: $${format.format(dashboard_mock_expense.total_spending)}",
                color = mainTextColor,
                style = Typography.titleLarge,
                fontSize = 27.sp,
                modifier = Modifier
                    .padding(start = 16.dp, top = 8.dp, bottom = 16.dp)
            )
            for(expense in dashboard_mock_expense.categories) {
                Card(
                    modifier = Modifier
                        .padding(4.dp)
                        .fillMaxSize()
                    ,
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    colors = CardDefaults.cardColors(containerColor = tileColor)
                ) {
                    Box (
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .padding(start = 10.dp, top = 10.dp, bottom = 10.dp, end = 10.dp)
                                .clip(CircleShape)
                                .background(expense.customColor as Color)
                                .align(Alignment.CenterStart)
                        )
                        Text(
                            text = "${expense.category}: ",
                            color = mainTextColor,
                            style = Typography.labelSmall,
                            modifier = Modifier
                                .padding(start=40.dp,top=8.dp,bottom=8.dp,end=8.dp)
                        )
                        Spacer(Modifier.fillMaxWidth(0.2f))
                        Text(
                            text = "${expense.percentage}%",
                            color = PurpleGrey40,
                            style = Typography.labelSmall,
                            modifier = Modifier
                                .padding(8.dp)
                                .align(Alignment.Center)
                        )
                        Text(
                            text = "$${format.format(expense.amount)}",
                            color = PurpleGrey40,
                            style = Typography.labelSmall,
                            modifier = Modifier
                                .padding(start=1.dp,top=8.dp,bottom=8.dp,end=20.dp)
                                .align(Alignment.CenterEnd)
                        )
                    }

                }
            }
        }
    }

    @Composable
    private fun Piechart() {
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

                    // TODO: if we need more colors, add a color jitterer
                    val colors: ArrayList<Int> = ArrayList()

                    // on below line we are creating array list and
                    // adding data to it to display in pie chart
                    val entries: ArrayList<PieEntry> = ArrayList()
                    for(expense in dashboard_mock_expense.categories) {
                        entries.add(PieEntry(expense.percentage, expense.category))
                        colors.add((expense.customColor as Color).toArgb())
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
