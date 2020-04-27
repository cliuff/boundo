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

package com.madness.collision.misc

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.pm.ShortcutManager
import android.graphics.drawable.Drawable
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.madness.collision.BuildConfig
import com.madness.collision.instant.Instant
import com.madness.collision.main.MainViewModel
import com.madness.collision.unit.Unit
import com.madness.collision.unit.api_viewing.AccessAV
import com.madness.collision.unit.we_chat_evo.InstantWeChatActivity
import com.madness.collision.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
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
            return
        }
        // below: app in update process to the newest
        // below: apply actions
        if ((verOri in 0 until 20021716) && X.aboveOn(X.N_MR1)) {
            val sm = context.getSystemService(ShortcutManager::class.java)
            if (sm != null) Instant(context, sm).refreshDynamicShortcuts(P.SC_ID_API_VIEWER)
        }
        if (verOri in 0 until 20032901) {
            // covert to json
            val pref: SharedPreferences = context.getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE)
            val data = pref.getStringSet(P.UNIT_FREQUENCIES, HashSet())!!
            pref.edit { remove(P.UNIT_FREQUENCIES) }
            val originalData = data.associate { it.split(":").run { this[0] to this[1] } }
            PrefsUtil.putCompoundItem(pref, P.UNIT_FREQUENCIES, originalData)
            // disable WeChat launcher icon
            if (Unit.getDescription(Unit.UNIT_NAME_WE_CHAT_EVO)?.isAvailable(context) == false) {
                val componentName = ComponentName(context.packageName, InstantWeChatActivity::class.qualifiedName ?: "")
                val isEnabled = context.packageManager.getComponentEnabledSetting(componentName) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                if (isEnabled) {
                    context.packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
                }
            }
            // init pinned units
            initPinnedUnits(prefSettings)
            // init tags
            AccessAV.initTagSettings(context, prefSettings)
        }
        if (verOri in 0 until 20042713) {
            // add new Adaptive Icon and Has Splits tags
            AccessAV.updateTagSettingsAiHs(context, prefSettings)
            // move pics from cache to file
            val valCachePubExterior = F.createPath(F.cachePublicPath(context), Environment.DIRECTORY_PICTURES, "Exterior")
            listOf(
                    F.createPath(valCachePubExterior, "back.webp") to F.valFilePubExteriorPortrait(context),
                    F.createPath(valCachePubExterior, "backDark.webp") to F.valFilePubExteriorPortraitDark(context),
                    F.createPath(valCachePubExterior, "twBack.webp") to F.valFilePubTwPortrait(context),
                    F.createPath(valCachePubExterior, "twBackDark.webp") to F.valFilePubTwPortraitDark(context)
            ).forEach {
                val oriFile = File(it.first)
                val newFile = File(it.second)
                if (oriFile.exists() && F.prepare4(newFile)) {
                    try {
                        X.copyFileLessTwoGB(oriFile, newFile)
                        oriFile.delete()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun initPinnedUnits(pref: SharedPreferences) {
        val pinnedUnits = mutableSetOf<String>()
        listOf(
                Unit.UNIT_NAME_API_VIEWING,
                Unit.UNIT_NAME_AUDIO_TIMER,
                Unit.UNIT_NAME_COOL_APP,
                Unit.UNIT_NAME_SCHOOL_TIMETABLE
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

    fun updateExteriorBackgrounds(context: Context){
        GlobalScope.launch {
            loadExteriorBackgrounds(context)
            if (context is ComponentActivity){
                launch(Dispatchers.Main){
                    val mainViewModel: MainViewModel by context.viewModels()
                    mainViewModel.background.value = mainApplication.background
                }
            }
        }
    }

    private fun loadExteriorBackgrounds(context: Context){
        val app = mainApplication
        val backPath = if (app.isDarkTheme) F.valFilePubExteriorPortraitDark(context) else F.valFilePubExteriorPortrait(context)
        (File(backPath).exists()).let {
            if (it) app.background = Drawable.createFromPath(backPath)
            app.exterior = it
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
