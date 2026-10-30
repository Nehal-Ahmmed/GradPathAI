package org.gradpath.ai

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class GradPathFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        println("[FCM] Device Token Generated: $token")
        // Automatically subscribe to the global academic feed updates topic
        FirebaseMessaging.getInstance().subscribeToTopic("academic_feed_updates")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    println("[FCM] Subscribed to topic 'academic_feed_updates'")
                }
            }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "🎓 GradPath AI Alert"

        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: "New scholarship or PhD research circular discovered!"

        println("[FCM] Push Notification Received: $title - $body")
        NotificationHelper.showFeedNotification(applicationContext, title, body)
    }
}
