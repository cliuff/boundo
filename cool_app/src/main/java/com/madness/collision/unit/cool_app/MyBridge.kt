package com.madness.collision.unit.cool_app

import android.appwidget.AppWidgetManager
import android.content.Context
import com.madness.collision.unit.Bridge
import com.madness.collision.unit.Unit

object MyBridge: Bridge() {

    override val unitName: String = Unit.UNIT_NAME_COOL_APP

    /**
     * @param args empty
     */
    override fun getUnitInstance(vararg args: Any?): Unit {
        return MyUnit()
    }

    fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        AppInfoWidgetFun.updateAppWidget(context, appWidgetManager, appWidgetId)
    }
}
