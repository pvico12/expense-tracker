package com.cs446.expensetracker

import android.app.Application
import com.cs446.expensetracker.api.RetrofitInstance
import com.cs446.expensetracker.session.UserSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ExpenseTrackerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initializeUserSession()
    }

    private fun initializeUserSession() {
        CoroutineScope(Dispatchers.IO).launch {
            try {

            } catch (e: Exception) {
                UserSession.isLoggedIn = false
            }
        }
    }
}