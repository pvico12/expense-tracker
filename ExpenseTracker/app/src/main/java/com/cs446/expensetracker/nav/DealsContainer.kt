package com.cs446.expensetracker.nav

import com.cs446.expensetracker.ui.AddExpenseScreen
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.icu.text.DecimalFormat
import android.location.Address
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat.startActivity
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cs446.expensetracker.api.RetrofitInstance
import com.cs446.expensetracker.api.models.CategoryBreakdown
import com.cs446.expensetracker.api.models.DealRetrievalResponse
import com.cs446.expensetracker.api.models.SpendingSummaryResponse
import com.cs446.expensetracker.dashboard.Deals
import com.cs446.expensetracker.mockData.Deal
import com.cs446.expensetracker.mockData.*
import com.cs446.expensetracker.mockData.mockTransactions
import com.cs446.expensetracker.session.UserSession
import com.cs446.expensetracker.ui.AddDealScreen
import com.cs446.expensetracker.ui.ui.theme.*
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import retrofit2.Response
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
//
//fun Context.getActivity(): AppCompatActivity? {
//    var currentContext = this
//    while (currentContext is ContextWrapper) {
//        if (currentContext is AppCompatActivity) {
//            return currentContext
//        }
//        currentContext = currentContext.baseContext
//    }
//    return null
//}

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
                AddDealScreen(navController = dealsNavController, editVersion=backStackEntry.arguments?.getString("editVersion")?.toInt() ?: -1)
            }
        }
    }

}
