package com.cs446.expensetracker.nav

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

import com.cs446.expensetracker.nav.settings.AdminPageContainer

class SettingsNavContainer {

    @Composable
    fun SettingsNavHost() {
        val settingsNavController = rememberNavController()
        NavHost(settingsNavController, startDestination = "settings") {
            composable("settings") {
                SettingsScreen(
                    onAdminPageClick = { settingsNavController.navigate("settings/admin-page") }
                )
            }

            composable("settings/admin-page") {
                val adminPageContainer = AdminPageContainer()
                adminPageContainer.AdminPageScreen(onBackClick = { settingsNavController.popBackStack() })
            }
        }
    }

    @Composable
    fun SettingsScreen(onAdminPageClick: () -> Unit) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SettingsListItem(
                header = "Settings Option 1",
                description = "Description of Settings Option 1",
                onClick = { }
            )
            Spacer(modifier = Modifier.height(3.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(3.dp))
            SettingsListItem(
                header = "Admin Page",
                description = "Administrative page for testing backend connection and overall application health.",
                onClick = onAdminPageClick
            )
        }
    }

    @Composable
    fun SettingsListItem(header: String, description: String, onClick: () -> Unit) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
                .clickable { onClick() },
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = header,
                modifier = Modifier.padding(4.dp),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = description,
                modifier = Modifier.padding(4.dp),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }


}