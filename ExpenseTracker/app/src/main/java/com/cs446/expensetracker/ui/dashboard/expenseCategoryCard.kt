package com.cs446.expensetracker.ui.dashboard

import android.icu.text.DecimalFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
fun ExpenseCategoryCard(expense: CategoryBreakdownDecorator) {

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
        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 3.dp, bottom = 3.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        )
        {
            Row(horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.width(190.dp))
            {
                Icon(
                    imageVector = Icons.Filled.Circle,
                    contentDescription = "Favorite",
                    modifier = Modifier.size(28.dp),
                    tint = Color(android.graphics.Color.parseColor((expense.color)))
                )
                Text(
                    text = "${expense.category_name}: ",
                    color = mainTextColor,
                    style = Typography.labelSmall,
                    modifier = Modifier
                        .padding(start=8.dp, top=8.dp,bottom=8.dp,end=8.dp)
                )
            }
            Row(horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.width(180.dp))
            {
                Column(modifier = Modifier.width(65.dp), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.Center) {
                    Text(
                        text = "${round(expense.percentage)}%",
                        color = PurpleGrey40,
                        style = Typography.labelSmall,
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.width(100.dp), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.Center) {
                    Text(
                        text = "$${formatCurrency(expense.total_amount)}",
                        color = PurpleGrey40,
                        style = Typography.labelSmall,
                    )
                }
            }
        }

    }
}