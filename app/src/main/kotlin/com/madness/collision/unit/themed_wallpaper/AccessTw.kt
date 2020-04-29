/*
 * Copyright 2020 Clifford Liu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.madness.collision.unit.themed_wallpaper

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import com.madness.collision.unit.Unit
import com.madness.collision.unit.UnitAccess

object AccessTw: UnitAccess(Unit.UNIT_NAME_THEMED_WALLPAPER) {

    fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        getMethod("updateAppWidget", Context::class, AppWidgetManager::class, Int::class)
                .invoke(context, appWidgetManager, appWidgetId)
    }

    @Suppress("UNCHECKED_CAST")
    fun getIsWallpaperChanged(activity: ComponentActivity): MutableLiveData<Boolean> {
        return getMethod("getIsWallpaperChanged", ComponentActivity::class)
                .invoke(activity) as MutableLiveData<Boolean>
    }

    @Suppress("UNCHECKED_CAST")
    fun getIsWallpaperChanged(fragment: Fragment): MutableLiveData<Boolean> {
        return getMethod("getIsWallpaperChanged", Fragment::class)
                .invoke(fragment) as MutableLiveData<Boolean>
    }
}
