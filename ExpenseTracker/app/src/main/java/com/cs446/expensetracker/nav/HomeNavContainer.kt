package com.cs446.expensetracker.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

class HomeNavContainer {

    @Composable
    fun HomeNavHost() {
        val homeNavController = rememberNavController()
        NavHost(homeNavController, startDestination = "home") {
            composable("home") {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Welcome!")
                }
            }
        }
    }
}