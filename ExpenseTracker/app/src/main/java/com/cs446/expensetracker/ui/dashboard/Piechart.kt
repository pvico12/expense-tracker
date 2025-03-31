package com.cs446.expensetracker.ui.dashboard

import android.graphics.Typeface
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.cs446.expensetracker.api.models.CategoryBreakdown
import com.cs446.expensetracker.ui.ui.theme.mainBackgroundColor
import com.cs446.expensetracker.ui.ui.theme.tileColor
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.MPPointF

@Composable
fun Piechart(spendingSummary: List<CategoryBreakdown>) {
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(340.dp)
            .padding(start=5.dp, end=5.dp)
            .testTag("Piechart"),
        factory = { context ->
            PieChart(context).apply {
                // Customize the PieChart here
                setExtraOffsets(5f, 0f, 5f, 5f)
                setDragDecelerationFrictionCoef(0.95f)
                getDescription().setEnabled(false)

                setDrawHoleEnabled(true)
                setHoleColor(Color(0x10F3E3E1).toArgb())
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
                    colors.add(Color(android.graphics.Color.parseColor((expense.color))).toArgb())
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

                // on below line we are setting pie data UI
                val data = PieData(dataSet)
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