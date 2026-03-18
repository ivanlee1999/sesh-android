package com.ivanlee.sesh

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class SeshApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val notificationManager = getSystemService(NotificationManager::class.java)

        val timerChannel = NotificationChannel(
            TIMER_CHANNEL_ID,
            getString(R.string.timer_notification_channel),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Persistent timer notification"
            setShowBadge(false)
        }

        val alertsChannel = NotificationChannel(
            ALERTS_CHANNEL_ID,
            getString(R.string.alerts_notification_channel),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Timer completion alerts"
            vibrationPattern = longArrayOf(0, 300, 200, 300)
            enableVibration(true)
        }

        notificationManager.createNotificationChannel(timerChannel)
        notificationManager.createNotificationChannel(alertsChannel)
    }

    companion object {
        const val TIMER_CHANNEL_ID = "sesh_timer"
        const val ALERTS_CHANNEL_ID = "sesh_alerts"
    }
}
