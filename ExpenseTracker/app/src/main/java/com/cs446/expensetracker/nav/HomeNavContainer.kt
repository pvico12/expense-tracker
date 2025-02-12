package com.cs446.expensetracker.nav

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cs446.expensetracker.dashboard.Dashboard


class HomeNavContainer {

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