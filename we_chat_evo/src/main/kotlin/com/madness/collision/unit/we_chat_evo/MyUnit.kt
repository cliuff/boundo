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

package com.madness.collision.unit.we_chat_evo

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.appcompat.widget.Toolbar
import com.madness.collision.R
import com.madness.collision.settings.SettingsFunc
import com.madness.collision.unit.Unit
import com.madness.collision.unit.we_chat_evo.databinding.UnitWeChatEvoBinding
import com.madness.collision.util.alterPadding
import com.madness.collision.util.notify
import com.madness.collision.unit.we_chat_evo.R as MyR

class MyUnit : Unit(), CompoundButton.OnCheckedChangeListener {

    override val id: String = "WE"

    private lateinit var viewBinding: UnitWeChatEvoBinding

    override fun createOptions(context: Context, toolbar: Toolbar, iconColor: Int): Boolean {
        toolbar.setTitle(R.string.unit_we_chat_evo)
        return true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        activity?.let { SettingsFunc.updateLanguage(it) }
        viewBinding = UnitWeChatEvoBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val context = context ?: return
        viewBinding.weNote.setText(MyR.string.we_note)
        viewBinding.weSwitch.isChecked = componentEnabled<InstantWeChatActivity>(context)
        viewBinding.weSwitch.setOnCheckedChangeListener(this)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        democratize()
        mainViewModel.contentWidthTop.observe(viewLifecycleOwner) {
            viewBinding.weContainer.alterPadding(top = it)
        }
    }

    private inline fun <reified K> componentEnabled(context: Context): Boolean {
        val comp = ComponentName(context.packageName, K::class.qualifiedName ?: "")
        return context.packageManager.getComponentEnabledSetting(comp) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
    }

    private inline fun <reified K> setComponentState(context: Context, state: Int) {
        val comp = ComponentName(context.packageName, K::class.qualifiedName ?: "")
        context.packageManager.setComponentEnabledSetting(comp, state, PackageManager.DONT_KILL_APP)
    }

    private fun getCompState(isEnabled: Boolean): Int {
        return if (isEnabled)
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        else
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
    }

    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        if (buttonView?.id != MyR.id.weSwitch) return
        val context = context ?: return
        if (componentEnabled<InstantWeChatActivity>(context) != isChecked) {
            setComponentState<InstantWeChatActivity>(context, getCompState(isChecked))
        }
        val toastRes = if (isChecked)
            MyR.string.Main_WeChatLauncher_Toast_State_ShiftEnabled
        else
            MyR.string.Main_WeChatLauncher_Toast_State_ShiftDisabled
        notify(toastRes)
    }

}
