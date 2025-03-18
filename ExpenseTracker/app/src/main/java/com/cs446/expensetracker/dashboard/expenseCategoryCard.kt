package com.cs446.expensetracker.dashboard

import android.icu.text.DecimalFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.cs446.expensetracker.api.models.CategoryBreakdown
import com.cs446.expensetracker.ui.ui.theme.PurpleGrey40
import com.cs446.expensetracker.ui.ui.theme.Typography
import com.cs446.expensetracker.ui.ui.theme.mainTextColor
import com.cs446.expensetracker.ui.ui.theme.tileColor
import kotlin.math.round

@Composable
fun expenseCategoryCard(expense: CategoryBreakdown) {

    fun formatCurrency(amount: Double): String {
        if (amount == 0.0) { return "0.00"}
        val format = DecimalFormat("#,###.00")
        return format.format(amount)
    }

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
                    .background(Color(android.graphics.Color.parseColor((expense.custom_color))))
                    .align(Alignment.CenterStart)
            )
            Text(
                text = "${expense.category_name}: ",
                color = mainTextColor,
                style = Typography.labelSmall,
                modifier = Modifier
                    .padding(start=40.dp,top=8.dp,bottom=8.dp,end=8.dp)
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
                    .padding(start=1.dp,top=8.dp,bottom=8.dp,end=20.dp)
                    .align(Alignment.CenterEnd)
            )
        }

    }
}