package org.gradpath.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Initialize Notification Channel for background feed alerts
        NotificationHelper.createNotificationChannel(this)

        // Subscribe to global academic feed updates topic
        try {
            com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic("academic_feed_updates")
        } catch (_: Exception) {}

        setContent {
            App()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}