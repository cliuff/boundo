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

package com.madness.collision.unit.api_viewing

import android.content.SharedPreferences
import android.os.Bundle
import androidx.core.content.edit
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.madness.collision.settings.SettingsFunc
import com.madness.collision.unit.api_viewing.tag.app.AppTagManager
import com.madness.collision.unit.api_viewing.tag.app.getFullLabel
import com.madness.collision.unit.api_viewing.util.PrefUtil
import com.madness.collision.util.*

internal class PrefAv: PreferenceFragmentCompat() {
    companion object {
        const val TAG = "PrefAv"

        @JvmStatic
        fun newInstance() = PrefAv()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = P.PREF_SETTINGS
        val context = context
        if (context != null) SettingsFunc.updateLanguage(context)
        setPreferencesFromResource(R.xml.pref_settings_av, rootKey)
        if (X.aboveOn(X.Q)) {
            findPref<SwitchPreference>(PrefUtil.API_APK_PRELOAD)?.run {
                isVisible = false
                // category preference
                parent?.isVisible = false
            }
        }

        findPref<SwitchPreference>(PrefUtil.API_CIRCULAR_ICON)?.apply {
            updatePrefRound(this.isChecked)
            onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                if(newValue is Boolean) updatePrefRound(newValue)
                true
            }
        }
        findPref<SwitchPreference>(PrefUtil.AV_SWEET)?.apply {
            updatePrefSweet(this.isChecked)
            onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                if(newValue is Boolean) updatePrefSweet(newValue)
                true
            }
        }
    }

    private fun updatePrefRound(newValue: Boolean) {
        findPref<SwitchPreference>(PrefUtil.API_PACKAGE_ROUND_ICON)?.isVisible = newValue
        findPref<SwitchPreference>(PrefUtil.AV_CLIP_ROUND)?.isVisible = newValue
    }

    private fun updatePrefSweet(newValue: Boolean) {
        if (!newValue) {
            val context = context ?: return
            AccessAV.clearSeals()
            X.deleteFolder(F.createFile(F.valCachePubAvSeal(context)))
        }
    }

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        if (preference == null) return super.onPreferenceTreeClick(preference)
        val context = context ?: return super.onPreferenceTreeClick(preference)
        val prefKeyTags = context.getString(R.string.avTags)
        if (preference.key == prefKeyTags) {
            val pref: SharedPreferences = preferenceManager.sharedPreferences
            val prefValue = pref.getStringSet(prefKeyTags, null) ?: emptySet()
            val tags = AppTagManager.tags
            val rankedTags = tags.values.sortedBy { it.rank }
            val checkedIndexes = prefValue.mapNotNullTo(mutableSetOf()) { id ->
                rankedTags.indexOfFirst { it.id == id }
            }
            val filterTags = rankedTags.map { it.getFullLabel(context)?.toString() ?: "" }
            val tagIcons = rankedTags.map { it.icon.drawableResId }
            PopupUtil.selectMulti(context, R.string.av_settings_tags, filterTags, tagIcons, checkedIndexes) { pop, _, indexes ->
                pop.dismiss()
                val resultSet = indexes.mapTo(mutableSetOf()) { rankedTags[it].id }
                pref.edit { putStringSet(prefKeyTags, resultSet) }
            }.show()
            return true
        }
        return super.onPreferenceTreeClick(preference)
    }

}