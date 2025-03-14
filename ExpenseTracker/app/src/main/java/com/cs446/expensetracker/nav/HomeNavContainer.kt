package com.cs446.expensetracker.nav

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cs446.expensetracker.dashboard.Dashboard


class HomeNavContainer {

    @RequiresApi(Build.VERSION_CODES.O)
    @Composable
    fun HomeNavHost() {
        val homeNavController = rememberNavController()
        val dashboard = Dashboard()

        NavHost(homeNavController, startDestination = "home") {
            composable("home") {
                dashboard.DashboardHost()
            }
        }
    }

}