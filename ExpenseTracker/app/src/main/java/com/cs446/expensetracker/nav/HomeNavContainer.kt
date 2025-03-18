package com.cs446.expensetracker.nav

import android.os.Build
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.DrawerDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.cs446.expensetracker.api.RetrofitInstance
import com.cs446.expensetracker.api.models.UserProfileResponse
import com.cs446.expensetracker.dashboard.Dashboard
import com.cs446.expensetracker.session.UserSession
import com.cs446.expensetracker.ui.ui.theme.Pink40
import com.cs446.expensetracker.ui.ui.theme.Pink80
import com.cs446.expensetracker.ui.ui.theme.Typography
import com.cs446.expensetracker.ui.ui.theme.tileColor
import kotlinx.coroutines.launch
import retrofit2.Response
import LogOut
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import com.cs446.expensetracker.ui.LoginActivity


class HomeNavContainer {

    @RequiresApi(Build.VERSION_CODES.O)
    @Composable
    fun HomeScreen(homeNavController: NavHostController) {
//        val homeNavController = rememberNavController()
        val coroutineScope = rememberCoroutineScope()
        val drawerState = rememberDrawerState(DrawerValue.Closed)
        val dashboard = Dashboard()
        val context = LocalContext.current

        var firstname by remember { mutableStateOf("") }
        var lastname by remember { mutableStateOf("") }

        LaunchedEffect(Unit){
            try {
                val token = UserSession.access_token ?: ""
                val userId = UserSession.userId
                val profileResponse: Response<UserProfileResponse> = RetrofitInstance.apiService.getUserProfile(userId)
                if (profileResponse.isSuccessful) {
                    val responseBody = profileResponse.body()
                    firstname = responseBody?.firstname?.takeIf { it.isNotEmpty() } ?: ""
                    lastname = responseBody?.lastname?.takeIf { it.isNotEmpty() } ?: ""
                }
            } catch (e: Exception) {
                Log.d("HOME NAV", "Error: ${e.message}")
            }
        }

        val closeDrawer: () -> Unit = {
            coroutineScope.launch { drawerState.close() }
        }

        ModalNavigationDrawer(
            drawerState = drawerState,
            scrimColor = DrawerDefaults.scrimColor,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = Pink40,
                    drawerTonalElevation = DrawerDefaults.ModalDrawerElevation
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = "$firstname $lastname",
                            color = tileColor,
                            style = Typography.headlineMedium,
                            modifier = Modifier
                                .padding(start = 16.dp, top = 16.dp)
                        )
                        Spacer(Modifier.height(24.dp))
                        NavigationDrawerItem(
                            icon = {
                                Icon(
                                    Icons.Filled.AccountCircle,
                                    contentDescription = null
                                )
                            },
                            label = { Text("Profile", color = tileColor) },
                            selected = false,
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                homeNavController.navigate("settings/profile")
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                unselectedIconColor = tileColor,
                                unselectedContainerColor = Color.Transparent,
                                selectedContainerColor = Pink80
                            ),
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )

                        // only for admin user
                        if (UserSession.role == "admin") {
                            NavigationDrawerItem(
                                icon = {
                                    Icon(
                                        Icons.Filled.BarChart,
                                        contentDescription = null
                                    )
                                },
                                label = { Text("Admin Testing", color = tileColor) },
                                selected = false,
                                onClick = {
                                    coroutineScope.launch { drawerState.close() }
                                    homeNavController.navigate("settings/admin-page")
                                },
                                colors = NavigationDrawerItemDefaults.colors(
                                    unselectedIconColor = tileColor,
                                    unselectedContainerColor = Color.Transparent
                                ),
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )
                        }

                        NavigationDrawerItem(
                            icon = { Icon(LogOut, contentDescription = null) },
                            label = { Text("Logout", color = tileColor) },
                            selected = false,
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                UserSession.isLoggedIn = false
                                UserSession.userId = -1
                                UserSession.access_token = ""
                                UserSession.refresh_token = ""
                                val intent = Intent(context, LoginActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                context.startActivity(intent)
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                unselectedIconColor = tileColor,
                                unselectedContainerColor = Color.Transparent,
                                selectedContainerColor = Pink80
                            ),
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                }
            }
        ) {
            BackHandler(enabled = drawerState.isOpen, onBack = closeDrawer)
            dashboard.DashboardScreen(drawerState, homeNavController)
        }


    }

}