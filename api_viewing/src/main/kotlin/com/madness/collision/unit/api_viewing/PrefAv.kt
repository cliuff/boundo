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

package com.madness.collision.unit.api_viewing

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.madness.collision.settings.SettingsFunc
import com.madness.collision.unit.api_viewing.util.PrefUtil
import com.madness.collision.util.F
import com.madness.collision.util.P
import com.madness.collision.util.X

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
            preferenceManager.findPreference<SwitchPreference>(PrefUtil.API_APK_PRELOAD)?.run {
                isVisible = false
                // category preference
                parent?.isVisible = false
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        preferenceManager.findPreference<SwitchPreference>(PrefUtil.API_CIRCULAR_ICON)?.apply {
            updatePrefRound(this.isChecked)
            onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                if(newValue is Boolean) updatePrefRound(newValue)
                true
            }
        }
        preferenceManager.findPreference<SwitchPreference>(PrefUtil.AV_SWEET)?.apply {
            updatePrefSweet(this.isChecked)
            onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                if(newValue is Boolean) updatePrefSweet(newValue)
                true
            }
        }
    }

    private fun updatePrefRound(newValue: Boolean){
        preferenceManager.findPreference<SwitchPreference>(PrefUtil.API_PACKAGE_ROUND_ICON)?.apply {
            isEnabled = newValue
        }
        preferenceManager.findPreference<SwitchPreference>(PrefUtil.AV_CLIP_ROUND)?.apply {
            isEnabled = newValue
        }
    }

    private fun updatePrefSweet(newValue: Boolean){
        if (!newValue) {
            val context = context ?: return
            AccessAV.clearSeals()
            X.deleteFolder(F.createFile(F.valCachePubAvSeal(context)))
        }
    }

}