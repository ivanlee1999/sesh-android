package com.ivanlee.sesh.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TimerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val serviceIntent = Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_ALARM_FIRED
        }
        context.startService(serviceIntent)
    }
}
