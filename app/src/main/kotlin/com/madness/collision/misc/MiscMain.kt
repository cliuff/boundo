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
import android.content.pm.ShortcutManager
import android.os.Environment
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.madness.collision.BuildConfig
import com.madness.collision.instant.Instant
import com.madness.collision.unit.Unit
import com.madness.collision.unit.UnitManager
import com.madness.collision.unit.api_viewing.AccessAV
import com.madness.collision.util.*
import com.madness.collision.util.os.OsUtils
import java.io.File
import java.io.IOException
import java.lang.Exception
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
            AccessAV.initTagSettings(context, prefSettings)
            // disable school timetable unit
            val desc = Unit.getDescription(Unit.UNIT_NAME_SCHOOL_TIMETABLE)
            if (desc != null) UnitManager(context).disableUnit(desc)
            return
        }
        // below: app in update process to the newest
        // below: apply actions
        if (verOri in 0 until 20032901) {
            // remove old format frequency data
            val pref = context.getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE)
            pref.edit { remove(P.UNIT_FREQUENCIES) }
            // init pinned units
            initPinnedUnits(prefSettings)
            // init tags
            AccessAV.initTagSettings(context, prefSettings)
        }
        if (verOri in 0 until 20042923) {
            // delete legacy location pics in cache
            val valCachePubExterior = F.createPath(F.cachePublicPath(context), Environment.DIRECTORY_PICTURES, "Exterior")
            arrayOf(
                F.createPath(valCachePubExterior, "back.webp"),
                F.createPath(valCachePubExterior, "backDark.webp"),
                F.createPath(valCachePubExterior, "twBack.webp"),
                F.createPath(valCachePubExterior, "twBackDark.webp"),
            ).forEach {
                val oriFile = File(it)
                if (oriFile.exists().not()) return@forEach  // continue
                try { oriFile.delete() } catch (e: IOException) { e.printStackTrace() }
            }
        }
        if ((verOri in 0 until 20092514) && X.aboveOn(X.N_MR1)) {
            val sm = context.getSystemService(ShortcutManager::class.java)
            if (sm != null) Instant(context, sm).refreshDynamicShortcuts(P.SC_ID_AUDIO_TIMER)
        }
        if (verOri in 0 until 21010113) {
            deleteDirs(F.valCachePubAvSeal(context))
        }
        if (verOri in 0 until 21081315) {
            // remove preference of Cool App unit
            prefSettings.edit {
                remove("toolsAppPackageName")
            }
            // remove unit config of removed units
            listOf("cool_app", "no_media", "we_chat_evo", "qq_contacts").forEach {
                Unit.unpinUnit(context, it, prefSettings)
                Unit.enableUnit(context, it, prefSettings)
                Unit.removeFrequency(context, it, prefSettings)
            }
            // clear qq contacts data
            if (OsUtils.satisfy(OsUtils.N_MR1)) {
                context.getSystemService(ShortcutManager::class.java)?.let { sm ->
                    val instant = Instant(context, sm)
                    val dataDir = F.createPath(F.cachePublicPath(context),
                        Environment.DIRECTORY_PICTURES, "qqInstantManager")
                    instant.dynamicShortcuts.filter {
                        it.id.startsWith("qq")
                    }.map { it.id }.let {
                        instant.removeDynamicShortcuts(*it.toTypedArray())
                    }
                    X.deleteFolder(File(dataDir))
                }
            }
        }
        if (verOri in 0 until 21090720) {
            AccessAV.updateTagSettings(context)
        }
        if (verOri in 0 until 22030517) {
            // disable school timetable unit
            val desc = Unit.getDescription(Unit.UNIT_NAME_SCHOOL_TIMETABLE)
            if (desc != null) UnitManager(context).disableUnit(desc)
            // remove unit config of removed unit
            Unit.UNIT_NAME_SCHOOL_TIMETABLE.let {
                Unit.unpinUnit(context, it, prefSettings)
                Unit.removeFrequency(context, it, prefSettings)
            }
            // delete exterior app background
            arrayOf(
                F.createPath(F.valFilePubExterior(context), "back.webp"),
                F.createPath(F.valFilePubExterior(context), "backDark.webp"),
            ).forEach {
                val file = File(it)
                if (file.exists().not()) return@forEach
                try { file.delete() } catch (e: Exception) { e.printStackTrace() }
            }
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
