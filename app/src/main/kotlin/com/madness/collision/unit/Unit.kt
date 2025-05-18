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

package com.madness.collision.unit

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import androidx.appcompat.widget.Toolbar
import androidx.core.content.edit
import androidx.fragment.app.activityViewModels
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.madness.collision.Democratic
import com.madness.collision.R
import com.madness.collision.main.MainViewModel
import com.madness.collision.util.*
import com.madness.collision.util.os.OsUtils

/**
 * Dynamic feature
 */
abstract class Unit: TaggedFragment(), Democratic {

    override val category: String
        get() = "Unit"

    companion object {
        const val UNIT_NAME_API_VIEWING = "api_viewing"
        const val UNIT_NAME_IMAGE_MODIFYING = "image_modifying"
        const val UNIT_NAME_THEMED_WALLPAPER = "themed_wallpaper"
        const val UNIT_NAME_AUDIO_TIMER = "audio_timer"
        const val UNIT_NAME_DEVICE_MANAGER = "device_manager"
        private val UNIT_CLASSES: MutableMap<String, Class<Unit>> = hashMapOf()
        private val UNIT_BRIDGE_CLASSES: MutableMap<String, Class<Bridge>> = hashMapOf()
        private val UNIT_DESCRIPTIONS: Map<String, Description>
        val UNITS: List<String>
        val STATIC_UNITS: List<String>
        val DYNAMIC_UNITS: List<String>
        init {
            val mUnitDescriptions: List<Description> = listOf(
                    Description(UNIT_NAME_API_VIEWING, R.string.apiViewer, R.drawable.ic_android_24).setDescResId(R.string.unit_desc_av),
                    StaticDescription(UNIT_NAME_IMAGE_MODIFYING, R.string.developertools_cropimage, R.drawable.ic_landscape_24).setDescResId(R.string.unit_desc_im),
                    StaticDescription(UNIT_NAME_THEMED_WALLPAPER, R.string.twService, R.drawable.ic_image_24).setDescResId(R.string.unit_desc_tw)
                            .setRequirement(Description.Checker(R.string.unit_desc_requirement_tw) {
                                it.packageManager.hasSystemFeature(PackageManager.FEATURE_LIVE_WALLPAPER)
                            }),
                    StaticDescription(UNIT_NAME_AUDIO_TIMER, R.string.unit_audio_timer, R.drawable.ic_timer_24).setDescResId(R.string.unit_desc_at),
                    StaticDescription(UNIT_NAME_DEVICE_MANAGER, R.string.unit_device_manager, R.drawable.ic_devices_other_24)
                            .setRequirement(Description.Checker(R.string.unit_desc_requirement_dm) {
                                it.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)
                            }),
            )
            UNIT_DESCRIPTIONS = mUnitDescriptions.associateBy { it.unitName }
            UNITS = UNIT_DESCRIPTIONS.keys.toList().sorted()
            STATIC_UNITS = mUnitDescriptions.filterIsInstance<StaticDescription>().map { it.unitName }
            DYNAMIC_UNITS = mUnitDescriptions.filter {
                it !is StaticDescription
            }.map { it.unitName }
        }

        fun getInstalledUnits(context: Context): List<String> {
            return UNITS.mapNotNull { if (getUnitClass(it) != null) it else null }
        }

        fun loadUnitClasses(
                context: Context,
                installedUnits: List<String> = getInstalledUnits(context),
                frequencies: Map<String, Int> = getFrequencies(context)
        ) {
            getSortedUnitNamesByFrequency(null, installedUnits, frequencies).forEach { unitName ->
                loadUnitClass(unitName)?.let { UNIT_CLASSES[unitName] = it }
                loadBridgeClass(unitName)?.let { UNIT_BRIDGE_CLASSES[unitName] = it }
            }
        }

        /**
         * From installed units
         */
        fun getSortedUnitNamesByFrequency(
                context: Context? = null,
                installedUnits: List<String> = getInstalledUnits(context!!),
                frequencies: Map<String, Int> = getFrequencies(context!!)
        ): List<String> {
            return installedUnits // small
                .map { it to (frequencies[it] ?: 0) }
                .sortedByDescending { it.second } // stateful
                .map { it.first } // few
        }

        fun getFrequencies(context: Context,
                           pref: SharedPreferences = context.getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE)
        ): Map<String, Int> {
            return PrefsUtil.getCompoundItem<String, String>(pref, P.UNIT_FREQUENCIES).mapValues {
                it.value.toInt()
            }
        }

        fun increaseFrequency(context: Context, unitName: String,
                              pref: SharedPreferences = context.getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE)
        ) {
            val frequencies = getFrequencies(context, pref).toMutableMap()
            val f = if (OsUtils.satisfy(OsUtils.N)) frequencies.getOrDefault(unitName, 0)
            else frequencies[unitName] ?: 0
            frequencies[unitName] = f + 1
            PrefsUtil.putCompoundItem(pref, P.UNIT_FREQUENCIES, frequencies.mapValues { it.value.toString() })
        }

        fun removeFrequency(context: Context, unitName: String,
                              pref: SharedPreferences = context.getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE)
        ) {
            val frequencies = getFrequencies(context, pref).toMutableMap()
            frequencies.remove(unitName)
            PrefsUtil.putCompoundItem(pref, P.UNIT_FREQUENCIES, frequencies.mapValues { it.value.toString() })
        }

        fun getPinnedUnits(context: Context,
                           pref: SharedPreferences = context.getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE)
        ): Set<String> {
            val json = pref.getString(P.UNIT_PINNED, "")
            if (json.isNullOrEmpty()) return emptySet()
            val type = TypeToken.getParameterized(Set::class.java, String::class.java).type
            return Gson().fromJson(json, type) ?: emptySet()
        }

        fun getIsPinned(context: Context, unitName: String,
                           pref: SharedPreferences = context.getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE)
        ): Boolean {
            val pinnedUnits = getPinnedUnits(context, pref)
            return pinnedUnits.contains(unitName)
        }

        fun togglePinned(context: Context, unitName: String,
                        pref: SharedPreferences = context.getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE),
                         isPinned: Boolean = getIsPinned(context, unitName, pref)
        ) {
            if (isPinned) unpinUnit(context, unitName, pref)
            else pinUnit(context, unitName, pref)
        }

        fun pinUnit(context: Context, unitName: String,
                              pref: SharedPreferences = context.getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE)
        ) {
            val pinnedUnits = getPinnedUnits(context, pref).toMutableSet()
            if (pinnedUnits.contains(unitName)) return
            pinnedUnits.add(unitName)
            val type = TypeToken.getParameterized(Set::class.java, String::class.java).type
            pref.edit { putString(P.UNIT_PINNED, Gson().toJson(pinnedUnits, type)) }
        }

        fun unpinUnit(context: Context, unitName: String,
                    pref: SharedPreferences = context.getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE)
        ) {
            val pinnedUnits = getPinnedUnits(context, pref).toMutableSet()
            if (!pinnedUnits.contains(unitName)) return
            pinnedUnits.remove(unitName)
            val type = TypeToken.getParameterized(Set::class.java, String::class.java).type
            pref.edit { putString(P.UNIT_PINNED, Gson().toJson(pinnedUnits, type)) }
        }

        fun getDisabledUnits(context: Context,
                           pref: SharedPreferences = context.getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE)
        ): Set<String> {
            val json = pref.getString(P.UNIT_DISABLED, "")
            if (json.isNullOrEmpty()) return emptySet()
            val type = TypeToken.getParameterized(Set::class.java, String::class.java).type
            return Gson().fromJson(json, type) ?: emptySet()
        }

        fun getIsDisabled(context: Context, unitName: String,
                        pref: SharedPreferences = context.getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE)
        ): Boolean {
            val disabledUnits = getDisabledUnits(context, pref)
            return disabledUnits.contains(unitName)
        }

        fun toggleDisabled(context: Context, unitName: String,
                         pref: SharedPreferences = context.getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE),
                         isDisabled: Boolean = getIsDisabled(context, unitName, pref)
        ) {
            if (isDisabled) enableUnit(context, unitName, pref)
            else disableUnit(context, unitName, pref)
        }

        fun disableUnit(context: Context, unitName: String,
                    pref: SharedPreferences = context.getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE)
        ) {
            val disabledUnits = getDisabledUnits(context, pref).toMutableSet()
            if (disabledUnits.contains(unitName)) return
            disabledUnits.add(unitName)
            val type = TypeToken.getParameterized(Set::class.java, String::class.java).type
            pref.edit { putString(P.UNIT_DISABLED, Gson().toJson(disabledUnits, type)) }
        }

        fun enableUnit(context: Context, unitName: String,
                      pref: SharedPreferences = context.getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE)
        ) {
            val disabledUnits = getDisabledUnits(context, pref).toMutableSet()
            if (!disabledUnits.contains(unitName)) return
            disabledUnits.remove(unitName)
            val type = TypeToken.getParameterized(Set::class.java, String::class.java).type
            pref.edit { putString(P.UNIT_DISABLED, Gson().toJson(disabledUnits, type)) }
        }

        @Suppress("UNCHECKED_CAST")
        fun loadUnitClass(unitName: String): Class<Unit>? {
            val packagedName = UNIT_DESCRIPTIONS[unitName]?.packagedName ?: ""
            return try {
                Class.forName("$packagedName.MyUnit") as Class<Unit>
            } catch (e: ClassNotFoundException) {
                e.printStackTrace()
                null
            }
        }

        @Suppress("UNCHECKED_CAST")
        fun loadBridgeClass(unitName: String): Class<Bridge>? {
            val packagedName = UNIT_DESCRIPTIONS[unitName]?.packagedName ?: ""
            return try {
                Class.forName("$packagedName.MyBridge") as Class<Bridge>
            } catch (e: ClassNotFoundException) {
                e.printStackTrace()
                null
            }
        }

        fun getUnitClass(unitName: String): Class<Unit>? {
            return UNIT_CLASSES[unitName] ?: loadUnitClass(unitName)
        }

        fun getBridgeClass(unitName: String): Class<Bridge>? {
            return UNIT_BRIDGE_CLASSES[unitName] ?: loadBridgeClass(unitName)
        }

        fun getDescription(unitName: String) : Description? {
            return UNIT_DESCRIPTIONS[unitName]
        }

        fun getUnit(unitName: String, vararg args: Any?): Unit? {
            val bridge = getBridge(unitName) ?: return null
            return bridge.getUnitInstance(*args)
        }

        fun getBridge(unitName: String): Bridge? {
            val clazz = getBridgeClass(unitName) ?: return null
            return clazz.objectInstance
        }

        fun getUpdates(unitName: String): UpdatesProvider? {
            return getBridge(unitName)?.getUpdates()
        }
    }

    protected val mainViewModel : MainViewModel by activityViewModels()

    protected fun democratize() {
        democratize(mainViewModel)
    }

    fun configNavigation(toolbar: Toolbar, iconColor: Int) {
        mainViewModel.configNavigation(toolbar, iconColor)
    }

}
