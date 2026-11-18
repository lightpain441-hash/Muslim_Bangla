package com.example.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.PrayerCalcMethod
import com.example.data.local.UserPreferencesRepository
import com.example.domain.islamic.PrayerTimeCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

object AdhanScheduler {
    const val CHANNEL_ADHAN = "adhan_prayer_alerts"
    const val CHANNEL_DAILY = "daily_ayah_hadith_reminders"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ADHAN, "নামাজ ও আজান সতর্কতা", NotificationManager.IMPORTANCE_HIGH)
            )
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_DAILY, "দৈনিক আয়াত ও হাদিস", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
    }

    fun scheduleDailyPrayers(
        context: Context,
        latitude: Double,
        longitude: Double,
        method: PrayerCalcMethod,
        adhanEnabled: Boolean,
        dailyReminderEnabled: Boolean
    ) {
        ensureChannels(context)
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val schedule = PrayerTimeCalculator.calculateForToday(latitude, longitude, method)

        schedule.slots.forEachIndexed { idx, slot ->
            val intent = Intent(context, AdhanAlarmReceiver::class.java).apply {
                putExtra(AdhanAlarmReceiver.EXTRA_TITLE, "${slot.nameBangla} ওয়াক্তের সময় হয়েছে")
                putExtra(AdhanAlarmReceiver.EXTRA_BODY, "সময়: ${slot.formatted12h} • জামাত ও ফরজ সালাতের প্রস্তুতি নিন।")
                putExtra(AdhanAlarmReceiver.EXTRA_IS_ADHAN, true)
                putExtra(AdhanAlarmReceiver.EXTRA_NOTIF_ID, 2000 + idx)
            }
            val pi = PendingIntent.getBroadcast(
                context,
                2000 + idx,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            if (adhanEnabled) {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, slot.hour24)
                    set(Calendar.MINUTE, slot.minute)
                    set(Calendar.SECOND, 0)
                    if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
                }
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
            } else {
                alarmManager.cancel(pi)
            }
        }

        val dailyIntent = Intent(context, AdhanAlarmReceiver::class.java).apply {
            putExtra(AdhanAlarmReceiver.EXTRA_TITLE, "আজকের কুরআন ও হাদিস")
            putExtra(AdhanAlarmReceiver.EXTRA_BODY, "“সুতরাং তোমরা আমাকে স্মরণ করো, আমিও তোমাদের স্মরণ রাখবো।” — সূরা বাকারা ২:১৫২")
            putExtra(AdhanAlarmReceiver.EXTRA_IS_ADHAN, false)
            putExtra(AdhanAlarmReceiver.EXTRA_NOTIF_ID, 3001)
        }
        val dailyPi = PendingIntent.getBroadcast(
            context,
            3001,
            dailyIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        if (dailyReminderEnabled) {
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 8)
                set(Calendar.MINUTE, 30)
                set(Calendar.SECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
            }
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                cal.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                dailyPi
            )
        } else {
            alarmManager.cancel(dailyPi)
        }
    }

    fun showImmediateTestNotification(context: Context, title: String, body: String) {
        ensureChannels(context)
        val nm = context.getSystemService(NotificationManager::class.java)
        val pi = PendingIntent.getActivity(
            context,
            10,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notif = NotificationCompat.Builder(context, CHANNEL_ADHAN)
            .setSmallIcon(R.drawable.ic_muslim_bangla_emblem)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        nm.notify(9901, notif)
    }
}

class AdhanAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        AdhanScheduler.ensureChannels(context)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "নামাজের সময়"
        val body = intent.getStringExtra(EXTRA_BODY) ?: "হাইয়্যা আলাস সালাহ"
        val isAdhan = intent.getBooleanExtra(EXTRA_IS_ADHAN, true)
        val notifId = intent.getIntExtra(EXTRA_NOTIF_ID, 2001)

        val pi = PendingIntent.getActivity(
            context,
            notifId,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val channel = if (isAdhan) AdhanScheduler.CHANNEL_ADHAN else AdhanScheduler.CHANNEL_DAILY
        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_muslim_bangla_emblem)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pi)
            .setAutoCancel(true)
        if (isAdhan) {
            builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
        }
        context.getSystemService(NotificationManager::class.java).notify(notifId, builder.build())
    }

    companion object {
        const val EXTRA_TITLE = "title"
        const val EXTRA_BODY = "body"
        const val EXTRA_IS_ADHAN = "is_adhan"
        const val EXTRA_NOTIF_ID = "notif_id"
    }
}

class BootRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val prefs = UserPreferencesRepository(context).preferencesFlow.first()
                AdhanScheduler.scheduleDailyPrayers(
                    context = context,
                    latitude = prefs.latitude,
                    longitude = prefs.longitude,
                    method = prefs.prayerCalcMethod,
                    adhanEnabled = prefs.adhanNotificationsEnabled,
                    dailyReminderEnabled = prefs.dailyAyahReminderEnabled
                )
            }
            pendingResult.finish()
        }
    }
}
