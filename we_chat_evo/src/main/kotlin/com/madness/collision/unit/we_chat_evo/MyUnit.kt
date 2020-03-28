package com.madness.collision.unit.we_chat_evo

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.observe
import com.madness.collision.R
import com.madness.collision.settings.SettingsFunc
import com.madness.collision.unit.Unit
import com.madness.collision.util.X
import com.madness.collision.util.alterPadding
import com.madness.collision.util.setMarginText
import kotlinx.android.synthetic.main.unit_we_chat_evo.*
import com.madness.collision.unit.we_chat_evo.R as MyR

class MyUnit : Unit(), CompoundButton.OnCheckedChangeListener {

    override fun createOptions(context: Context, toolbar: Toolbar, iconColor: Int): Boolean {
        toolbar.setTitle(R.string.unit_we_chat_evo)
        return true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity?.let { SettingsFunc.updateLanguage(it) }
        return inflater.inflate(MyR.layout.unit_we_chat_evo, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val context = context ?: return
        weNote.setMarginText(context, MyR.string.we_note)
        weSwitch.isChecked = componentEnabled<InstantWeChatActivity>(context)
        weSwitch.setOnCheckedChangeListener(this)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        democratize()
        mainViewModel.contentWidthTop.observe(viewLifecycleOwner) {
            weContainer.alterPadding(top = it)
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
        X.toast(context, toastRes, Toast.LENGTH_LONG)
    }

}
