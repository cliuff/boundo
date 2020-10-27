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

package com.madness.collision.settings

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.edit
import androidx.core.content.res.use
import androidx.fragment.app.activityViewModels
import com.madness.collision.Democratic
import com.madness.collision.R
import com.madness.collision.databinding.FragmentSettingsBinding
import com.madness.collision.databinding.SettingsUnitItemBinding
import com.madness.collision.main.MainActivity
import com.madness.collision.main.MainViewModel
import com.madness.collision.pref.PrefExterior
import com.madness.collision.unit.DescRetriever
import com.madness.collision.unit.Unit
import com.madness.collision.util.*

internal class SettingsFragment : TaggedFragment(), Democratic, NavNode {

    override val category: String = "Settings"
    override val id: String = "Settings"

    companion object {
        private const val TAG = "Settings"
    }

    override val nodeDestinationId: Int = R.id.settingsFragment
    private val mainViewModel: MainViewModel by activityViewModels()
    private var _viewBinding: FragmentSettingsBinding? = null
    private val viewBinding: FragmentSettingsBinding
        get() = _viewBinding!!

    private lateinit var prHandler: PermissionRequestHandler

    override fun createOptions(context: Context, toolbar: Toolbar, iconColor: Int): Boolean {
        toolbar.setTitle(R.string.Main_ToolBar_title_Settings)
        return true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _viewBinding = FragmentSettingsBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onDestroyView() {
        _viewBinding = null
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val context = context ?: return

        if (mainApplication.debug) {
            viewBinding.settingsUnits.findViewById<ViewStub>(R.id.settingsItemLanguagesStub)?.inflate()
            viewBinding.settingsUnits.findViewById<View>(R.id.settingsItemLanguagesContainer)?.run {
                findViewById<TextView>(R.id.settingsUnitItem)?.run {
                    setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_language_24, 0, 0, 0)
                    setText(R.string.Settings_Button_SwitchLanguage)
                }
                setOnClickListener {
                    showLanguages(context)
                }
            }
        }

        viewBinding.settingsItemStyle.setOnClickListener {
//            navigate(SettingsFragmentDirections.actionSettingsFragmentToUtilPage(TypedNavArg<PrefExterior>(), R.string.settings_exterior))
            navigate(SettingsFragmentDirections.actionSettingsFragmentToUtilPage(TypedNavArg<PrefExterior>()).apply {
                titleId = R.string.settings_exterior
            })
        }

        viewBinding.settingsItemAbout.setOnClickListener {
            navigate(SettingsFragmentDirections.actionSettingsFragmentToAdviceFragment())
        }

        val installedUnits = DescRetriever(context).retrieveInstalled()
        val inflater = LayoutInflater.from(context)
        val parent = viewBinding.settingsUnits
        for (state in installedUnits) {
            val unit = state.unitName
            val unitBridge = Unit.getBridge(unit) ?: continue
            val settingsPage = unitBridge.getSettings() ?: continue
            val unitDesc = state.description
            val checkerBinding = SettingsUnitItemBinding.inflate(inflater, parent, true)
            checkerBinding.settingsUnitItemContainer.setOnClickListener {
                mainViewModel.displayFragment(settingsPage)
            }
            checkerBinding.settingsUnitItem.run {
                text = unitDesc.getName(context)
                setCompoundDrawablesRelativeWithIntrinsicBounds(unitDesc.getIcon(context), null, null, null)
            }
        }
    }

    private fun showLanguages(context: Context) {
        val langEntries = context.resources.obtainTypedArray(R.array.prefSettingsLangEntries)
        val langValues = context.resources.obtainTypedArray(R.array.prefSettingsLangValues)
        val lang = SettingsFunc.getLanguage(context)
        Log.i(TAG, "Currently set: $lang")
        val langIndex = P.getPrefIndex(lang, langValues)
        PopupUtil.selectSingle(context, R.string.Settings_Language_Dialog_Title, langEntries, langIndex) {
            pop, _, index ->
            pop.dismiss()
            val values = context.resources.obtainTypedArray(R.array.prefSettingsLangValues)
            val newLang = values.use { it.getString(index) } ?: SettingsFunc.AUTO
            val systemAppLang = SystemUtil.getLocaleApp().toString()
            Log.i(TAG, "New lang: $newLang, system app lang: $systemAppLang")
            val oldLocaleString = SystemUtil.getLocaleUsr(context).toString()
            SettingsFunc.settingsPreferencesLanguage(context, newLang)
            val newLocaleString = SystemUtil.getLocaleUsr(context).toString()
            val shouldSwitch = oldLocaleString != newLocaleString
            if (shouldSwitch) {
                if (newLang == SettingsFunc.AUTO && oldLocaleString != systemAppLang){
                    SettingsFunc.switchLanguage(context, systemAppLang)
                }
                mainViewModel.action.value = MainActivity.ACTION_RECREATE to null
            }
        }.show()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        democratize(mainViewModel)
        mainViewModel.contentWidthTop.observe(viewLifecycleOwner) {
            viewBinding.settingsUnits.alterPadding(top = it)
        }
        mainViewModel.contentWidthBottom.observe(viewLifecycleOwner) {
            viewBinding.settingsUnits.alterPadding(bottom = it)
        }
    }

    private fun checkUpdate(context: Context){
        val activity = activity ?: return
        val bar = ProgressBar(context)
        val checking = CollisionDialog.loading(context, bar)
        bar.post {
            val settingsPreferences = context.getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE)
            settingsPreferences.edit {
                putBoolean(P.SETTINGS_UPDATE_NOTIFY, true)
                putBoolean(P.SETTINGS_UPDATE_VIA_SETTINGS, true)
            }
            prHandler = PermissionRequestHandler(activity)
            SettingsFunc.check4Update(context, checking, prHandler)
        }
        checking.show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode){
            SettingsFunc.requestWriteStorage -> {
                if (grantResults.isEmpty()) return
                when (grantResults[0]){
                    PackageManager.PERMISSION_GRANTED -> prHandler.resumeJob.run()
                }
            }
        }
    }

}
