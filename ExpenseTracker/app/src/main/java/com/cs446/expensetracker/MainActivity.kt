package com.cs446.expensetracker

import com.cs446.expensetracker.ui.LoginActivity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cs446.expensetracker.nav.HomeNavContainer
import com.cs446.expensetracker.ui.theme.ExpenseTrackerTheme
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import com.cs446.expensetracker.nav.TransactionNavContainer
import com.cs446.expensetracker.nav.SettingsNavContainer
import com.cs446.expensetracker.session.UserSession
import com.cs446.expensetracker.ui.AddExpenseScreen
import com.cs446.expensetracker.viewmodels.UserSessionViewModel

class MainActivity : ComponentActivity() {
    private val userSessionViewModel: UserSessionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userSessionViewModel.initializeSession()

        setContent {
            ExpenseTrackerTheme {
                val isLoggedIn by userSessionViewModel.isLoggedIn.observeAsState(UserSession.isLoggedIn)

                LaunchedEffect(isLoggedIn) {
                    if (!isLoggedIn) {
                        startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                        finish()
                    }
                }

                if (isLoggedIn) {
                    val rootNavController = rememberNavController()
                    val navBackStackEntry by rootNavController.currentBackStackEntryAsState()
                    Scaffold(
                        bottomBar = {
                            NavigationBar {
                                navItems.forEach { item ->
                                    val isSelected = item.title.lowercase() ==
                                            navBackStackEntry?.destination?.route
                                    NavigationBarItem(
                                        selected = isSelected,
                                        label = {
                                            Text(text = item.title)
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = if(isSelected) {
                                                    item.selectedIcon
                                                } else item.unselectedIcon,
                                                contentDescription = item.title
                                            )
                                        },
                                        onClick = {
                                            rootNavController.navigate(item.title.lowercase()) {
                                                popUpTo(rootNavController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    )
                                }
                            }
                        },
                        floatingActionButton = {
                            FloatingActionButton(
                                onClick = { rootNavController.navigate("addExpense") }
                            ) {
                                Icon(imageVector = Icons.Filled.Add, contentDescription = "Add Expense")
                            }
                        }
                    ) { padding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding)
                        ) {
                            NavHost(rootNavController, startDestination = "home") {
                                composable("home") {
                                    val homeNavContainer = HomeNavContainer()
                                    homeNavContainer.HomeNavHost()
                                }
                                composable("settings") {
                                    val settingsNavContainer = SettingsNavContainer()
                                    settingsNavContainer.SettingsNavHost()
                                }
                                composable("history") {
                                    val transactionNavContainer = TransactionNavContainer()
                                    transactionNavContainer.TransactionNavHost()
                                }
                                composable("addExpense") {
                                    AddExpenseScreen(navController = rootNavController)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

data class BottomNavigationItem(
    val title: String,
    val unselectedIcon: ImageVector,
    val selectedIcon: ImageVector
)

val navItems = listOf(
    BottomNavigationItem(
        title = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
    ),
    BottomNavigationItem(
        title = "Settings",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings,
    ),
    BottomNavigationItem(
        title = "History",
        selectedIcon = Icons.Filled.History,
        unselectedIcon = Icons.Outlined.History,
    ),
)
