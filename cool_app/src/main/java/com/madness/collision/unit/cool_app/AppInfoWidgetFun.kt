package com.madness.collision.unit.cool_app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.madness.collision.BuildConfig
import com.madness.collision.R
import com.madness.collision.main.MainActivity
import com.madness.collision.util.P
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import com.madness.collision.unit.cool_app.R as MyR

object AppInfoWidgetFun {

    fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        GlobalScope.launch {
            val remoteViews = RemoteViews(BuildConfig.APPLICATION_ID, R.layout.app_info_widget)
            remoteViews.setViewVisibility(R.id.appInfoWidgetLogo, View.GONE)
            remoteViews.setViewVisibility(R.id.appInfoWidgetInfo, View.GONE)
            remoteViews.setViewVisibility(R.id.appInfoWidgetLoading, View.VISIBLE)
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews)

            val prefSettings = context.getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE)
            val packageName: String = prefSettings.getString(P.APP_INFO_PACKAGE, P.APP_INFO_PACKAGE_DEFAULT)!!
            val coolApp = CoolApp(packageName)
            if (!coolApp.retrieve()) return@launch

            val infoDownloads = context.getString(MyR.string.ca_downloads) + " " + coolApp.countDownloads
            val infoFlowers = context.getString(MyR.string.ca_flowers) + " " + coolApp.countFlowers
            val infoComments = context.getString(MyR.string.ca_comments) + " " + coolApp.countComments

            val widgetText = "$infoDownloads\n$infoFlowers\n$infoComments"
            remoteViews.setViewVisibility(R.id.appInfoWidgetLoading, View.GONE)
            remoteViews.setViewVisibility(R.id.appInfoWidgetLogo, View.VISIBLE)
            remoteViews.setViewVisibility(R.id.appInfoWidgetInfo, View.VISIBLE)
            remoteViews.setTextViewText(R.id.appInfoWidgetInfo, widgetText)
            remoteViews.setImageViewBitmap(R.id.appInfoWidgetLogo, coolApp.logo)

            // below: click action
            remoteViews.setOnClickPendingIntent(
                    R.id.appInfoWidgetRL,
                    Intent(context, MainActivity::class.java).run {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        putExtras(MainActivity.forItem(MyBridge.unitName))
                        PendingIntent.getActivity(context, 0, this, 0)
                    }
            )

            // Instruct the widget manager to update the widget
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
        }
    }
}
