package com.cs446.expensetracker

import com.cs446.expensetracker.session.UserSession
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class PushNotificationService: FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Send the token to the server
        UserSession.fcmToken = token
    }

//    override fun onMessageReceived(message: RemoteMessage) {
//        super.onMessageReceived(message)
//
//        // here you can edit push notification message
//    }
}