package com.madness.collision.versatile

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.util.Log
import com.madness.collision.main.MainActivity
import com.madness.collision.unit.Unit
import com.madness.collision.unit.cool_app.AccessCA

class AppInfoWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            AccessCA.updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
        if (context == null || intent == null) return
        val action = intent.action
        when(action){
            ACTION_CLICK_PARENT -> {
                Log.i("Widget", "click")
                Intent(context, MainActivity::class.java).apply {
                    putExtras(MainActivity.forItem(Unit.UNIT_NAME_COOL_APP))
                    context.startActivity(this)
                }
            }
        }
    }

    companion object {

        const val ACTION_CLICK_PARENT = "widget app action parent"
    }
}
