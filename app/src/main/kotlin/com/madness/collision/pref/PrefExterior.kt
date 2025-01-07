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

package com.madness.collision.pref

import android.app.TimePickerDialog
import android.content.SharedPreferences
import android.content.res.TypedArray
import android.os.Bundle
import androidx.core.content.edit
import androidx.core.content.res.use
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.madness.collision.R
import com.madness.collision.main.MainActivity
import com.madness.collision.util.*

internal class PrefExterior: PreferenceFragmentCompat() {
    companion object {
        const val TAG = "PrefExterior"
        private const val TIME_SCHEDULE_START = 0
        private const val TIME_SCHEDULE_END = 1

        @JvmStatic
        fun newInstance() = PrefExterior()
    }

    private lateinit var prefLightTheme: Preference
    private lateinit var prefDarkTheme: Preference
    private lateinit var prefApplyDark: Preference
    private lateinit var prefScheduleStart: Preference
    private lateinit var prefScheduleEnd: Preference
    private var keyLightTheme: String = ""
    private var keyDarkTheme: String = ""
    private var keyApplyDarkPlan: String = ""
    private var keyScheduleStart = ""
    private var keyScheduleEnd = ""
    private var indexLightTheme = 0
    private var indexDarkTheme = 0
    private var indexApplyDark = 0
    private val entriesLightTheme: TypedArray
        get() = resources.obtainTypedArray(R.array.prefExteriorLightThemeEntries)
    private val entriesDarkTheme: TypedArray
        get() = resources.obtainTypedArray(R.array.prefExteriorDarkThemeEntries)
    private val entriesApplyDark: TypedArray
        get() = resources.obtainTypedArray(R.array.prefExteriorDarkPlanEntries)
    private val valuesLightTheme: TypedArray
        get() = resources.obtainTypedArray(R.array.prefExteriorLightThemeValues)
    private val valuesDarkTheme: TypedArray
        get() = resources.obtainTypedArray(R.array.prefExteriorDarkThemeValues)
    private val valuesApplyDark: TypedArray
        get() = resources.obtainTypedArray(R.array.prefExteriorDarkPlanValues)
    private val pref: SharedPreferences?
        get() = preferenceManager.sharedPreferences
    private var scheduleStart: String
        get() = pref?.getString(keyScheduleStart, getString(R.string.prefExteriorDefaultDarkPlanScheduleStart)) ?: ""
        set(value) {
            pref?.edit { putString(keyScheduleStart, value) }
        }
    private var scheduleEnd: String
        get() = pref?.getString(keyScheduleEnd, getString(R.string.prefExteriorDefaultDarkPlanScheduleEnd)) ?: ""
        set(value) {
            pref?.edit { putString(keyScheduleEnd, value) }
        }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = P.PREF_SETTINGS
        setPreferencesFromResource(R.xml.pref_exterior, rootKey)
        if (X.belowOff(X.Q)) {
            findPref<Preference>(R.string.prefExteriorKeyForceDarkDesc)?.isVisible = false
        }

        keyApplyDarkPlan = getString(R.string.prefExteriorKeyDarkPlan)
        keyScheduleStart = getString(R.string.prefExteriorKeyDarkPlanScheduleStart)
        keyScheduleEnd = getString(R.string.prefExteriorKeyDarkPlanScheduleEnd)

        prefScheduleStart = findPref(keyScheduleStart) ?: return
        prefScheduleEnd = findPref(keyScheduleEnd) ?: return

        val planValue = pref?.getString(keyApplyDarkPlan, getString(R.string.prefExteriorDefaultDarkPlan)) ?: ""
        updateScheduleEnabling(planValue)
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        val context = context ?: return super.onPreferenceTreeClick(preference)
        return when(preference.key){
            keyLightTheme -> {
                PopupUtil.selectSingle(context, R.string.prefExteriorLightTheme, entriesLightTheme, indexLightTheme) {
                    pop, _, index ->
                    pop.dismiss()
                    preferenceManager.sharedPreferences?.edit {
                        putString(keyLightTheme, valuesLightTheme.use { it.getString(index) })
                    }
                    prefLightTheme.summaryProvider = summaryProvider
                    updateTheme()
                }.show()
                true
            }
            keyDarkTheme -> {
                PopupUtil.selectSingle(context, R.string.prefExteriorDarkTheme, entriesDarkTheme, indexDarkTheme) {
                    pop, _, index ->
                    pop.dismiss()
                    preferenceManager.sharedPreferences?.edit {
                        putString(keyDarkTheme, valuesDarkTheme.use { it.getString(index) })
                    }
                    prefDarkTheme.summaryProvider = summaryProvider
                    updateTheme()
                }.show()
                true
            }
            keyApplyDarkPlan -> {
                PopupUtil.selectSingle(context, R.string.prefExteriorDarkPlan, entriesApplyDark, indexApplyDark) {
                    pop, _, index ->
                    pop.dismiss()
                    val value = valuesApplyDark.use { it.getString(index) }
                    preferenceManager.sharedPreferences?.edit {
                        putString(keyApplyDarkPlan, value)
                    }
                    prefApplyDark.summaryProvider = summaryProvider
                    updateScheduleEnabling(value ?: "")
                    updateTheme()
                }.show()
                true
            }
            keyScheduleStart -> {
                pickTime(TIME_SCHEDULE_START)
                true
            }
            keyScheduleEnd -> {
                pickTime(TIME_SCHEDULE_END)
                true
            }
            getString(R.string.prefExteriorKeyForceDarkDesc) -> {
                CollisionDialog.alert(context, R.string.prefExteriorForceDarkDescDetail).show()
                true
            }
            else -> super.onPreferenceTreeClick(preference)
        }
    }

    private fun pickTime(timePhase: Int){
        val time = when(timePhase){
            TIME_SCHEDULE_END -> scheduleEnd
            else -> scheduleStart
        }
        val (h, m) = getTimeDestructured(time)
        TimePickerDialog(context, { _, hourOfDay, minute ->
            val newTime = "${if (hourOfDay < 10) "0" else ""}$hourOfDay${if (minute < 10) "0" else ""}$minute"
            when(timePhase){
                TIME_SCHEDULE_END -> {
                    scheduleEnd = newTime
                    prefScheduleEnd.summaryProvider = summaryProvider
                }
                else -> {
                    scheduleStart = newTime
                    prefScheduleStart.summaryProvider = summaryProvider
                }
            }
            updateTheme()
        }, h.toInt(), m.toInt(),true).show()
    }

    private fun getTimeDestructured(time: String) = "(\\d{2})(\\d{2})".toRegex().find(time)!!.destructured

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        keyLightTheme = getString(R.string.prefExteriorKeyLightTheme)
        keyDarkTheme = getString(R.string.prefExteriorKeyDarkTheme)

        prefLightTheme = findPref(keyLightTheme) ?: return
        prefDarkTheme = findPref(keyDarkTheme) ?: return
        prefApplyDark = findPref(keyApplyDarkPlan) ?: return

        prefLightTheme.summaryProvider = summaryProvider
        prefDarkTheme.summaryProvider = summaryProvider
        prefApplyDark.summaryProvider = summaryProvider
        prefScheduleStart.summaryProvider = summaryProvider
        prefScheduleEnd.summaryProvider = summaryProvider

        val keyBS = getString(R.string.prefExteriorKeyDarkByBatterySaver)
        findPreference<SwitchPreference>(keyBS)?.setOnPreferenceChangeListener { _, newVal ->
            pref?.edit { putBoolean(keyBS, newVal as Boolean) }
            updateTheme()
            true
        }
    }

    private fun updateScheduleEnabling(planValue: String) {
        val scheduleEnabled = planValue == getString(R.string.prefExteriorDarkPlanValueSchedule)
        prefScheduleStart.isVisible = scheduleEnabled
        prefScheduleEnd.isVisible = scheduleEnabled
    }

    private fun updateTheme(){
        mainApplication.setAction(MainActivity.ACTION_EXTERIOR_THEME to null)
    }

    private val summaryProvider = Preference.SummaryProvider<Preference> { preference ->
        val pref: SharedPreferences = preferenceManager.sharedPreferences ?: return@SummaryProvider ""
        when(preference){
            prefLightTheme -> {
                val lightValue = pref.getString(keyLightTheme, getString(R.string.prefExteriorDefaultLightTheme)) ?: ""
                val lightEntry = P.getPrefIndexedEntry(lightValue, entriesLightTheme, valuesLightTheme)
                indexLightTheme = lightEntry.index
                lightEntry.value
            }
            prefDarkTheme -> {
                val darkValue = pref.getString(keyDarkTheme, getString(R.string.prefExteriorDefaultDarkTheme)) ?: ""
                val darkEntry = P.getPrefIndexedEntry(darkValue, entriesDarkTheme, valuesDarkTheme)
                indexDarkTheme = darkEntry.index
                darkEntry.value
            }
            prefApplyDark -> {
                val planValue = pref.getString(keyApplyDarkPlan, getString(R.string.prefExteriorDefaultDarkPlan)) ?: ""
                val planEntry = P.getPrefIndexedEntry(planValue, entriesApplyDark, valuesApplyDark)
                indexApplyDark = planEntry.index
                planEntry.value
            }
            prefScheduleStart -> {
                val (h, m) = getTimeDestructured(scheduleStart)
                "$h:$m"
            }
            prefScheduleEnd -> {
                val (h, m) = getTimeDestructured(scheduleEnd)
                "$h:$m"
            }
            else -> ""
        }
    }
}
