package com.example.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.R
import com.example.data.model.AlarmFrequency
import com.example.data.model.StoredAlarm
import java.util.Calendar
import java.util.concurrent.TimeUnit

object NotificationHelper {
    const val CHANNEL_ID = "tibb_medicine_alarms"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.notification_channel_desc)
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun showReminderNotification(
        context: Context,
        alarmId: String,
        title: String,
        body: String,
    ) {
        ensureChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            alarmId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(alarmId.hashCode(), notification)
    }

    fun scheduleAlarmWork(context: Context, alarm: StoredAlarm) {
        ensureChannel(context)
        val wm = WorkManager.getInstance(context)
        scheduleSlot(wm, "${alarm.id}_1", alarm, alarm.time24h)
        if (alarm.frequency == AlarmFrequency.TWICE_DAILY && !alarm.secondTime24h.isNullOrBlank()) {
            scheduleSlot(wm, "${alarm.id}_2", alarm, alarm.secondTime24h)
        }
    }

    private fun scheduleSlot(wm: WorkManager, uniqueWorkName: String, alarm: StoredAlarm, time24h: String) {
        val parts = time24h.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 8
        val minute = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
        }
        val initialDelayMs = (target.timeInMillis - now.timeInMillis).coerceAtLeast(10_000L)
        val inputData = Data.Builder()
            .putString(ReminderWorker.KEY_ALARM_ID, uniqueWorkName)
            .putString(ReminderWorker.KEY_TITLE, "Tibb: ${alarm.patientName} — ${alarm.medicineLabel}")
            .putString(
                ReminderWorker.KEY_BODY,
                alarm.notes?.takeIf { it.isNotBlank() }
                    ?: "دوا یا معائنے کا وقت ہو گیا ہے۔ براہِ کرم مقررہ وقت پر لیں۔",
            )
            .build()

        when (alarm.frequency) {
            AlarmFrequency.ONE_TIME -> {
                val req = OneTimeWorkRequestBuilder<ReminderWorker>()
                    .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
                    .setInputData(inputData)
                    .build()
                wm.enqueueUniqueWork(uniqueWorkName, ExistingWorkPolicy.REPLACE, req)
            }
            AlarmFrequency.WEEKLY -> {
                val req = PeriodicWorkRequestBuilder<ReminderWorker>(7, TimeUnit.DAYS)
                    .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
                    .setInputData(inputData)
                    .build()
                wm.enqueueUniquePeriodicWork(uniqueWorkName, ExistingPeriodicWorkPolicy.UPDATE, req)
            }
            AlarmFrequency.DAILY, AlarmFrequency.TWICE_DAILY -> {
                val req = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
                    .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
                    .setInputData(inputData)
                    .build()
                wm.enqueueUniquePeriodicWork(uniqueWorkName, ExistingPeriodicWorkPolicy.UPDATE, req)
            }
        }
    }
}

class ReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val alarmId = inputData.getString(KEY_ALARM_ID) ?: "tibb_alarm"
        val title = inputData.getString(KEY_TITLE) ?: "Tibb Medicine Reminder"
        val body = inputData.getString(KEY_BODY) ?: "Time for your scheduled dose or health checkup."
        NotificationHelper.showReminderNotification(applicationContext, alarmId, title, body)
        return Result.success()
    }

    companion object {
        const val KEY_ALARM_ID = "alarm_id"
        const val KEY_TITLE = "title"
        const val KEY_BODY = "body"
    }
}
