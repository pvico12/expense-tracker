package com.cs446.expensetracker.session

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

object UserSession {
    var isLoggedIn: Boolean = false
    var userId: Int = -1
    var access_token: String = ""
    var refresh_token: String = ""
    var fcmToken: String = ""
}
