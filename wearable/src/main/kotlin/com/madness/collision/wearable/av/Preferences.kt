package com.madness.collision.wearable.av

import android.os.Bundle
import androidx.fragment.app.activityViewModels
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import androidx.wear.ambient.AmbientModeSupport
import com.madness.collision.wearable.R
import com.madness.collision.wearable.av.data.ApiUnit
import com.madness.collision.wearable.av.data.EasyAccess
import com.madness.collision.wearable.main.MainViewModel
import com.madness.collision.wearable.util.P

internal class Preferences: PreferenceFragmentCompat() {
    companion object {
        @JvmStatic
        fun newInstance() = Preferences()
    }

    private lateinit var mAmbient: AmbientModeSupport.AmbientCallback
    private val mainViewModel: MainViewModel by activityViewModels()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = P.PREF_SETTINGS
        setPreferencesFromResource(R.xml.pref_av, rootKey)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val prefSweet = preferenceManager.findPreference<SwitchPreference>(getString(R.string.avSweet))?.apply {
            onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                if(newValue is Boolean) updatePrefSweet(newValue)
                true
            }
        }
        val prefIncludeDisabled = preferenceManager.findPreference<SwitchPreference>(getString(R.string.avIncludeDisabled))?.apply {
            onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                if(newValue is Boolean) updatePrefIncludeDisabled(newValue)
                true
            }
        }

//        val prefCategory: PreferenceCategory? by lazy { preferenceManager.findPreference<PreferenceCategory>(getString(R.string.avCategoryAPI)) }
        mAmbient = object : AmbientModeSupport.AmbientCallback(){
            override fun onEnterAmbient(ambientDetails: Bundle?) {
                super.onEnterAmbient(ambientDetails)
                prefSweet?.isEnabled = false
                prefIncludeDisabled?.isEnabled = false
            }

            override fun onExitAmbient() {
                super.onExitAmbient()
                prefSweet?.isEnabled = true
                prefIncludeDisabled?.isEnabled = true
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mainViewModel.ambient = mAmbient
    }

    private fun updatePrefSweet(newValue: Boolean){
        EasyAccess.isSweet = newValue
    }

    private fun updatePrefIncludeDisabled(newValue: Boolean){
        EasyAccess.shouldIncludeDisabled = newValue
        val viewModel: ApiViewingViewModel by activityViewModels()
        viewModel.screenOut(ApiUnit.ALL_APPS)
        viewModel.updateApps4Display()
        viewModel.loadedItems.unLoad(ApiUnit.ALL_APPS)
    }

}
