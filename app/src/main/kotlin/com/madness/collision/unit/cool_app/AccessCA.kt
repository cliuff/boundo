package com.madness.collision.unit.cool_app

import android.appwidget.AppWidgetManager
import android.content.Context
import com.madness.collision.unit.Unit
import com.madness.collision.unit.UnitAccess

object AccessCA: UnitAccess(Unit.UNIT_NAME_COOL_APP) {

    fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        getMethod("updateAppWidget", Context::class, AppWidgetManager::class, Int::class)
                .invoke(context, appWidgetManager, appWidgetId)
    }
}
