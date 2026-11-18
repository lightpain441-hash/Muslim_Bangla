package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.local.PrayerCalcMethod
import com.example.data.local.UserPreferencesRepository
import com.example.domain.islamic.HijriCalendarHelper
import com.example.domain.islamic.PrayerTimeCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MuslimBanglaWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val prefs = UserPreferencesRepository(context).preferencesFlow.first()
                updateWidgets(
                    context = context,
                    appWidgetManager = appWidgetManager,
                    appWidgetIds = appWidgetIds,
                    cityName = prefs.cityName,
                    lat = prefs.latitude,
                    lng = prefs.longitude,
                    method = prefs.prayerCalcMethod
                )
            }
            pendingResult.finish()
        }
    }

    companion object {
        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, MuslimBanglaWidgetProvider::class.java))
            if (ids.isNotEmpty()) {
                CoroutineScope(Dispatchers.IO).launch {
                    runCatching {
                        val prefs = UserPreferencesRepository(context).preferencesFlow.first()
                        updateWidgets(
                            context, manager, ids,
                            prefs.cityName, prefs.latitude, prefs.longitude,
                            prefs.prayerCalcMethod
                        )
                    }
                }
            }
        }

        private fun updateWidgets(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray,
            cityName: String,
            lat: Double,
            lng: Double,
            method: PrayerCalcMethod
        ) {
            val schedule = PrayerTimeCalculator.calculateForToday(lat, lng, method)
            val hijri = HijriCalendarHelper.gregorianToHijri()
            val launchPi = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            appWidgetIds.forEach { appWidgetId ->
                val views = RemoteViews(context.packageName, R.layout.widget_muslim_bangla).apply {
                    setTextViewText(R.id.widget_title, "মুসলিম বাংলা • ${cityName.substringBefore(",")}")
                    setTextViewText(R.id.widget_hijri_date, hijri.formattedBangla)
                    setTextViewText(
                        R.id.widget_next_prayer,
                        "পরবর্তী: ${schedule.nextPrayer.nameBangla} — ${schedule.nextPrayer.formatted12h}"
                    )
                    setOnClickPendingIntent(R.id.widget_root, launchPi)
                }
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }
}
