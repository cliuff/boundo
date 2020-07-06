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

package com.madness.collision.instant

import android.annotation.TargetApi
import android.content.ComponentName
import android.content.Context
import android.content.pm.ComponentInfo
import android.content.pm.PackageManager
import android.content.pm.ShortcutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.madness.collision.R
import com.madness.collision.databinding.InstantItemComplexBinding
import com.madness.collision.databinding.InstantItemSimpleBinding
import com.madness.collision.instant.shortcut.InstantShortcut
import com.madness.collision.main.MainViewModel
import com.madness.collision.misc.MiscApplication
import com.madness.collision.util.X
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@TargetApi(X.N_MR1)
internal class InstantAdapter<T: InstantItem>(
        context: Context, private val mainViewModel: MainViewModel,
        private val dataType: Int, data: List<T> = emptyList()
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_SIMPLE = R.layout.instant_item_simple
        private const val TYPE_COMPLEX = R.layout.instant_item_complex
        const val TYPE_SHORTCUT = 0
        const val TYPE_TILE = 1
        const val TYPE_OTHER = 2
    }

    class InstantSimpleHolder(binding: InstantItemSimpleBinding): RecyclerView.ViewHolder(binding.root) {
        val switch: SwitchMaterial = binding.instantItemSimpleSwitch
        val title: TextView = binding.instantItemSimpleTitle
        val container: View = binding.instantItemSimpleContainer
    }

    class InstantComplexHolder(binding: InstantItemComplexBinding): RecyclerView.ViewHolder(binding.root) {
        val switch: SwitchMaterial = binding.instantItemComplexTitleSwitch
        val title: TextView = binding.instantItemComplexTitle
        val titleLayout: View = binding.instantItemComplexTitleLayout
    }

    private val mContext: Context = context
    private val mInflater = LayoutInflater.from(context)
    private var mData: List<T> = data
    private val instant: Instant?
    init {
        instant = if (isShortcut) {
            val manager = context.getSystemService(ShortcutManager::class.java)
            if (manager != null) Instant(context, manager) else null
        } else {
            null
        }
    }
    private var mComponents: List<ComponentInfo>? = null
    private var isLoadingComponents: Boolean = false
    private val onLoadedCallbacks: MutableList<() -> Unit> = mutableListOf()

    private val isShortcut: Boolean
        get() = dataType == TYPE_SHORTCUT

    private val isTile: Boolean
        get() = dataType == TYPE_TILE

    private val isOther: Boolean
        get() = dataType == TYPE_OTHER

    override fun getItemViewType(position: Int): Int {
        return if (mData[position].hasDescription) TYPE_COMPLEX else TYPE_SIMPLE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when(viewType) {
            TYPE_SIMPLE -> InstantSimpleHolder(InstantItemSimpleBinding.inflate(mInflater, parent, false))
            else -> InstantComplexHolder(InstantItemComplexBinding.inflate(mInflater, parent, false))
        }
    }

    override fun getItemCount(): Int {
        return mData.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = mData[position]
        val name = item.getName(mContext)
        val switch: SwitchMaterial
        when (holder) {
            is InstantSimpleHolder -> {
                holder.title.text = name
                holder.container.setOnClickListener {
                    holder.switch.toggle()
                }
                switch = holder.switch
            }
            is InstantComplexHolder -> {
                holder.title.text = name
                holder.titleLayout.setOnClickListener {
                    mainViewModel.displayFragment(item.descriptionPage!!)
                }
                switch = holder.switch
            }
            else -> return
        }
        if (isShortcut) {
            val shortcutItem = item as InstantShortcut
            instant?.dynamicShortcuts?.find { it.id == shortcutItem.id }?.let {
                switch.isChecked = true
            }
            switch.setOnCheckedChangeListener { _, isChecked ->
                instant ?: return@setOnCheckedChangeListener
                if (isChecked) instant.addDynamicShortcuts(shortcutItem.id)
                else instant.removeDynamicShortcuts(shortcutItem.id)
            }
        } else if (isTile || isOther) {
            val tileItem = item as InstantComponent<*>
            val claName = tileItem.klass.qualifiedName ?: ""
            val checker = {
                if (claName.isNotEmpty()) GlobalScope.launch {
                    val components = mComponents ?: emptyList()
                    val isEnabled = MiscApplication.isComponentEnabled(mContext, claName, components)
                    launch(Dispatchers.Main) {
                        switch.isChecked = isEnabled
                    }
                }
            }
            if (isLoadingComponents) {
                onLoadedCallbacks.add { checker.invoke() }
            } else if (mComponents == null) {
                isLoadingComponents = true
                GlobalScope.launch {
                    mComponents = MiscApplication.getComponents(mContext)
                    isLoadingComponents = false
                    checker.invoke()
                    onLoadedCallbacks.forEach { it.invoke() }
                }
            } else {
                checker.invoke()
            }
            val comp = ComponentName(mContext.packageName, claName)
            switch.setOnCheckedChangeListener { _, isChecked ->
                val state = if (isChecked)
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                else
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                mContext.packageManager.setComponentEnabledSetting(comp, state, PackageManager.DONT_KILL_APP)
            }
        }
    }

}
