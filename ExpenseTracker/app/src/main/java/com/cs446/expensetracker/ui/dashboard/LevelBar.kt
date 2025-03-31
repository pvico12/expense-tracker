package com.cs446.expensetracker.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.cs446.expensetracker.api.models.LevelRequest
import com.cs446.expensetracker.ui.ui.theme.Typography
import com.cs446.expensetracker.ui.ui.theme.pieChartColor1
import com.cs446.expensetracker.ui.ui.theme.pieChartColor2

@Composable
fun LevelBar(levelStats: LevelRequest?) {
    Column(
    modifier = Modifier
    .height(410.dp)
    .testTag("LevelBar")
        ,
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Bottom
    )
    {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 50.dp, end = 75.dp, top = 200.dp)
                .height(110.dp),
            contentAlignment = Alignment.Center,
        ) {
            LinearProgressIndicator(
                progress = { levelStats!!.current_xp.toFloat() / levelStats.total_xp_for_next_level.toFloat() },
                modifier = Modifier.fillMaxWidth().height(20.dp)
                    .padding(start = 58.dp).clip(
                        RoundedCornerShape(10.dp)
                    ),
                color = pieChartColor1,
                trackColor = pieChartColor2
            )
            Text(
                text = "${levelStats!!.current_xp} / ${levelStats.total_xp_for_next_level} xp",
                color = Color.Black,
                style = Typography.titleSmall,
                modifier = Modifier
                    .padding(start = 40.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = "Level",
                        modifier = Modifier.size(100.dp)
                            .padding(bottom = 10.dp).testTag("LevelIcon"),
                        tint = Color(0xFFEE8B38)
                    )
                    Text(
                        text = "${levelStats.level}",
                        color = Color.Black,
                        style = Typography.titleLarge,
                        modifier = Modifier
                            .padding(bottom = 5.dp)
                    )
                }
            }
        }
    }
}