package com.madness.collision.pref

import android.os.Bundle
import androidx.fragment.app.activityViewModels
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.madness.collision.main.MainViewModel
import com.madness.collision.settings.SettingsFunc
import com.madness.collision.unit.api_viewing.AccessAV
import com.madness.collision.unit.api_viewing.R
import com.madness.collision.util.F
import com.madness.collision.util.P
import com.madness.collision.util.X

internal class PrefAv: PreferenceFragmentCompat() {
    companion object {
        const val TAG = "PrefAv"

        @JvmStatic
        fun newInstance() = PrefAv()
    }

    private val mainViewModel: MainViewModel by activityViewModels()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = P.PREF_SETTINGS
        val context = context
        if (context != null) SettingsFunc.updateLanguage(context)
        setPreferencesFromResource(R.xml.pref_settings_av, rootKey)
        if (X.aboveOn(X.Q)) {
            preferenceManager.findPreference<SwitchPreference>(P.API_APK_PRELOAD)?.isVisible = false
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        preferenceManager.findPreference<SwitchPreference>(P.API_CIRCULAR_ICON)?.apply {
            updatePrefRound(this.isChecked)
            onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                if(newValue is Boolean) updatePrefRound(newValue)
                true
            }
        }
        preferenceManager.findPreference<SwitchPreference>(P.AV_SWEET)?.apply {
            updatePrefSweet(this.isChecked)
            onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                if(newValue is Boolean) updatePrefSweet(newValue)
                true
            }
        }
    }

    private fun updatePrefRound(newValue: Boolean){
        preferenceManager.findPreference<SwitchPreference>(P.API_PACKAGE_ROUND_ICON)?.apply {
            isEnabled = newValue
        }
        preferenceManager.findPreference<SwitchPreference>(P.AV_CLIP_ROUND)?.apply {
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