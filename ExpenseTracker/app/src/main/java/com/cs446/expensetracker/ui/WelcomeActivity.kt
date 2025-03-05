package com.cs446.expensetracker.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auth0.jwt.JWT
import com.cs446.expensetracker.R
import com.cs446.expensetracker.ui.ui.theme.mainTextColor
import com.cs446.expensetracker.ui.ui.theme.tileColor
import com.cs446.expensetracker.ui.ui.theme.Typography
import com.cs446.expensetracker.ui.ui.theme.Pink40
import com.cs446.expensetracker.ui.ui.theme.Purple40


class WelcomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Pink40
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.coin_half),
                        contentDescription = "Coin Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter),
                    )
                }
                WelcomeScreen(onLoginClick = {
                    startActivity(Intent(this, LoginActivity::class.java))
                }, onCreateAccountClick = {
                    startActivity(Intent(this, SignupActivity::class.java))
                })
            }
        }
    }
}

@Composable
fun WelcomeScreen(onLoginClick: () -> Unit, onCreateAccountClick: () -> Unit) {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp
    val buttonShape = RoundedCornerShape(26.dp)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height((screenHeight * 0.2f).dp))
        Text(
            text = "CoinTrail",
            color = tileColor,
            style = Typography.headlineLarge,
        )
        Spacer(modifier = Modifier.height((screenHeight * 0.01f).dp))
        Text(
            text = "Can't seem to keep your finances in check?\nLet us help you change that",
            style = Typography.bodySmall,
            color = tileColor,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height((screenHeight * 0.1f).dp))
        Button(
            onClick = onLoginClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .graphicsLayer {
                    shadowElevation = 8f
                    shape = buttonShape
                    clip = true
                    translationX = 4f
                    translationY = 4f
                },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = tileColor
            ),
            contentPadding = PaddingValues(0.dp),
            shape = buttonShape
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                mainTextColor,
                                Purple40
                            )
                        ),
                        shape = buttonShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text="LOGIN", style=Typography.headlineSmall)
            }
        }
        Spacer(modifier = Modifier.height((screenHeight * 0.02f).dp))
        Button(
            onClick = onCreateAccountClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = tileColor
            ),
            contentPadding = PaddingValues(0.dp),
            shape = buttonShape,
            border = BorderStroke(3.dp, mainTextColor)
        ) {
            Text(text="SIGN UP", style=Typography.headlineSmall)
        }
    }
}