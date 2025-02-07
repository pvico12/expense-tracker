package com.cs446.expensetracker.nav.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cs446.expensetracker.api.EnvironmentConstants
import com.cs446.expensetracker.api.TargetBackendEnvironment
import com.cs446.expensetracker.viewmodels.HealthcheckViewModel

class AdminPageContainer {

    @OptIn(ExperimentalMaterial3Api::class)

    @Composable
    fun AdminPageScreen(modifier: Modifier = Modifier, onBackClick: () -> Unit) {
        val viewModel: HealthcheckViewModel = viewModel()
        val status1 = viewModel.status1.observeAsState()
        val status2 = viewModel.status2.observeAsState()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Admin Page") },
                    navigationIcon = {
                        IconButton(onClick = { onBackClick() }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            },
            content = { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    Column(
                        modifier = modifier
                            .padding(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                                .padding(10.dp)
                        ) {
                            Text(text = "Backend Env", modifier = Modifier.weight(1f)) // New column
                            Text(text = "Status Code", modifier = Modifier.weight(1f))
                            Text(text = "Status", modifier = Modifier.weight(1f))
                            Text(text = "Health", modifier = Modifier.weight(1f))
                        }
                        HorizontalDivider(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                                .padding(10.dp)
                        ) {
                            if (status1.value != null) {
                                Text(text = if (EnvironmentConstants.TARGET_BACKEND_ENV == TargetBackendEnvironment.PROD) "PROD" else "DEV", modifier = Modifier.weight(1f)) // New column
                                Text(text = "${status1.value?.statusCode ?: "Unknown"}", modifier = Modifier.weight(1f))
                                Text(text = "${status1.value?.status ?: "Unknown"}", modifier = Modifier.weight(1f))
                                val color1 = if (status1.value?.statusCode == 200) Color.Green else Color.Red
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(color1, shape = CircleShape)
                                        .weight(1f)
                                )
                            } else {
                                Text(text = "Loading...", modifier = Modifier.weight(1f))
                                Text(text = "Loading...", modifier = Modifier.weight(1f))
                                Text(text = "Loading...", modifier = Modifier.weight(1f))
                                Text(text = "Loading...", modifier = Modifier.weight(1f)) // New column
                            }
                        }
                        HorizontalDivider(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                                .padding(10.dp)
                        ) {
                            if (status2.value != null) {
                                Text(text = if (EnvironmentConstants.TARGET_BACKEND_ENV == TargetBackendEnvironment.PROD) "DEV" else "PROD", modifier = Modifier.weight(1f)) // New column
                                Text(text = "${status2.value?.statusCode ?: "Unknown"}", modifier = Modifier.weight(1f))
                                Text(text = "${status2.value?.status ?: "Unknown"}", modifier = Modifier.weight(1f))
                                val color2 = if (status2.value?.statusCode == 200) Color.Green else Color.Red
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(color2, shape = CircleShape)
                                        .weight(1f)
                                )
                            } else {
                                Text(text = "Loading...", modifier = Modifier.weight(1f))
                                Text(text = "Loading...", modifier = Modifier.weight(1f))
                                Text(text = "Loading...", modifier = Modifier.weight(1f))
                                Text(text = "Loading...", modifier = Modifier.weight(1f)) // New column
                            }
                        }
                    }
                }
            }
        )
    }
}