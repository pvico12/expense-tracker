package com.cs446.expensetracker.nav

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cs446.expensetracker.ui.deals.Deals
import com.cs446.expensetracker.ui.deals.AddDealScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DealsContainer {

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @Composable
    fun DealsNavHost() {
        val dealsNavController = rememberNavController()
        val dealsPage = Deals()

        NavHost(dealsNavController, startDestination = "deals") {
            composable("deals") {
                dealsPage.DealsHost(dealsNavController)
            }
            composable("addDealScreen/{editVersion}") { backStackEntry ->
                AddDealScreen(
                    navController = dealsNavController,
                    editVersion=backStackEntry.arguments?.getString("editVersion")?.toInt() ?: -1
                ) {
                    CoroutineScope(Dispatchers.Main).launch {
                    dealsNavController.popBackStack()
                } }
            }
        }
    }

}
