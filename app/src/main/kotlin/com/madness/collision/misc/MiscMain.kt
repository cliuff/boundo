/*
 * Copyright 2021 Clifford Liu
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

package com.madness.collision.misc

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.madness.collision.BuildConfig
import com.madness.collision.settings.LanguageMan
import com.madness.collision.unit.Unit
import com.madness.collision.unit.api_viewing.AccessAV
import com.madness.collision.util.*
import com.madness.collision.util.config.LocaleUtils
import com.madness.collision.util.os.OsUtils
import java.io.File
import kotlin.random.Random

internal object MiscMain {

    fun clearCache(context: Context){
        deleteDirs(F.valCachePubAvApk(context), F.valCachePubAvLogo(context))
        if(Random(System.currentTimeMillis()).nextInt(100) == 1) deleteDirs(F.valCachePubAvSeal(context))
    }

    /**
     * Check app version and do certain updates when app updates
     */
    fun ensureUpdate(context: Context, prefSettings: SharedPreferences){
        val verDefault = -1
        val verOri = prefSettings.getInt(P.APPLICATION_V, verDefault)
        val ver = BuildConfig.VERSION_CODE
        // below: update registered version
        prefSettings.edit { putInt(P.APPLICATION_V, ver) }
        // below: app gone through update process
        if (verOri == ver) return
        // below: app first launch or launch after erased data
        // newly add operations must be add in the corresponding update section as well
        // because this one will not be invoked during app update
        // causing those devices unaffected
        if (verOri == verDefault) {
            // init pinned units
            initPinnedUnits(prefSettings)
            // init tags
            AccessAV.initTagSettings(prefSettings)
            return
        }
        // below: app in update process to the newest
        // below: apply actions
        if (verOri in 0 until 22082119 && OsUtils.satisfy(OsUtils.T)) {
            val locale = LanguageMan(context).getLocaleOrNull()
            if (locale != null) LocaleUtils.set(locale)
        }
        if (verOri in 0 until 22103122) {
            // remove unit config of school timetable
            "school_timetable".let {
                Unit.unpinUnit(context, it, prefSettings)
                Unit.removeFrequency(context, it, prefSettings)
            }
            // delete files of school timetable unit
            deleteDirs(
                F.createPath(F.filePublicPath(context), "TT"),
                F.createPath(F.cachePublicPath(context), "TT"),
            )
            // remove preferences
            if (OsUtils.satisfy(OsUtils.N)) {
                context.deleteSharedPreferences("iCalendarPreferences")
            } else {
                context.getSharedPreferences("iCalendarPreferences", Context.MODE_PRIVATE)
                    .edit { clear() }
            }
            val keys = listOf("icsInstructor", "googleCalendarDefault", "iCalendarAppMode", "icsFilePath")
            prefSettings.edit { keys.forEach { remove(it) } }
        }
        listOf(SelfUpdater23()).forEach { updater ->
            if (verOri in 0..<updater.maxVerCode) updater.apply(verOri, prefSettings)
        }
    }

    private fun initPinnedUnits(pref: SharedPreferences) {
        val pinnedUnits = mutableSetOf<String>()
        listOf(
                Unit.UNIT_NAME_API_VIEWING,
                Unit.UNIT_NAME_AUDIO_TIMER,
        ).forEach {
            pinnedUnits.add(it)
        }
        pref.edit { putString(P.UNIT_PINNED, Gson().toJson(pinnedUnits, TypeToken.getParameterized(Set::class.java, String::class.java).type)) }
    }

    private fun deleteDirs(vararg paths: String){
        paths.forEach {
            val f = File(it)
            if (f.exists()) f.deleteRecursively()
        }
    }

    fun registerNotificationChannels(context: Context, prefSettings: SharedPreferences){
        val ver = prefSettings.getString(P.NOTIFICATION_CHANNELS_NO, "") ?: ""
        prefSettings.edit { putString(P.NOTIFICATION_CHANNELS_NO, NotificationsUtil.NO) }
        if (ver == NotificationsUtil.NO) return
        NotificationsUtil.updateAllGroups(context)
        NotificationsUtil.updateAllChannels(context)
    }

}
