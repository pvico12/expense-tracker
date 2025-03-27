package com.cs446.expensetracker

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Grading
import androidx.compose.material.icons.automirrored.outlined.Grading
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import androidx.compose.ui.graphics.Color
import com.cs446.expensetracker.ui.dashboard.AddGoalScreen
import com.cs446.expensetracker.nav.DealsContainer
import com.cs446.expensetracker.nav.TransactionNavContainer
import com.cs446.expensetracker.nav.settings.AdminPageContainer
import com.cs446.expensetracker.nav.settings.ProfilePageContainer
import com.cs446.expensetracker.session.UserSession
import com.cs446.expensetracker.ui.AddExpenseScreen
import com.cs446.expensetracker.ui.WelcomeActivity
import com.cs446.expensetracker.viewmodels.UserSessionViewModel
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
class MainActivity : ComponentActivity() {
    private val userSessionViewModel: UserSessionViewModel by viewModels()

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userSessionViewModel.initializeSession()

        FirebaseApp.initializeApp(this) // Add this line

        // Fetch FCM token
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.d("FCM Token", token)
                UserSession.fcmToken = token
                // Send this token to your Python backend
//                sendTokenToBackend(token)
            } else {
                Log.e("FCM Token", "Fetching token failed", task.exception)
            }
        }

        setContent {
            ExpenseTrackerTheme {
                val isLoggedIn by userSessionViewModel.isLoggedIn.observeAsState(UserSession.isLoggedIn)

                LaunchedEffect(isLoggedIn) {
                    if (!isLoggedIn) {
                        startActivity(Intent(this@MainActivity, WelcomeActivity::class.java))
                        finish()
                    }
                }

                if (isLoggedIn) {
                    val rootNavController = rememberNavController()
                    val navBackStackEntry by rootNavController.currentBackStackEntryAsState()
                    Scaffold(
                        bottomBar = {
                            NavigationBar(containerColor = Color(0xFF4B0C0C), contentColor = Color(0xFFF6F3F3)) {
                                navItems.forEach { item ->
                                    val isSelected = item.title.lowercase() ==
                                            navBackStackEntry?.destination?.route
                                    val colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color(0xFFF6F3F3),
                                        selectedTextColor = Color(0xFFF6F3F3),
                                        indicatorColor = Color(0xFF9A3B3B),
                                        unselectedIconColor = Color(0xFFF6F3F3),
                                        unselectedTextColor = Color(0xFFF6F3F3)
                                    )
                                    NavigationBarItem(
                                        colors = colors,
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
                            if (navBackStackEntry?.destination?.route?.contains("settings") != true) {
                                FloatingActionButton(
                                    onClick = { rootNavController.navigate("addExpense") },
                                    containerColor = Color(0xFF4B0C0C),
                                    contentColor = Color(0xFFF6F3F3)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Add,
                                        contentDescription = "Add Expense"
                                    )
                                }
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
                                    homeNavContainer.HomeScreen(rootNavController)
                                }
                                composable("history") {
                                    val transactionNavContainer = TransactionNavContainer()
                                    transactionNavContainer.TransactionNavHost()
                                }
                                composable("deals") {
                                    val settingsNavContainer = DealsContainer()
                                    settingsNavContainer.DealsNavHost()
                                }
                                composable("addExpense") {
                                    AddExpenseScreen(navController = rootNavController)
                                }
                                composable("settings/admin-page") {
                                    val adminPageContainer = AdminPageContainer()
                                    adminPageContainer.AdminPageScreen(onBackClick = { rootNavController.popBackStack() })
                                }

                                composable("settings/profile") {
                                    val profilePageContainer = ProfilePageContainer()
                                    profilePageContainer.ProfilePageScreen(onBackClick = { rootNavController.popBackStack() })
                                }

                                composable("addGoalScreen/{editVersion}") { backStackEntry ->
                                    AddGoalScreen(navController = rootNavController, editVersion=backStackEntry.arguments?.getString("editVersion")?.toInt() ?: -1)
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
        title = "History",
        selectedIcon = Icons.Filled.History,
        unselectedIcon = Icons.Outlined.History,
    ),
    BottomNavigationItem(
        title = "Deals",
        selectedIcon = Icons.AutoMirrored.Filled.Grading,
        unselectedIcon = Icons.AutoMirrored.Outlined.Grading,
    ),
)
