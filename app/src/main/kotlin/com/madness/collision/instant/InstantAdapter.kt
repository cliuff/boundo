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

import android.content.ComponentName
import android.content.Context
import android.content.pm.ComponentInfo
import android.content.pm.PackageManager
import android.content.pm.ShortcutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.madness.collision.R
import com.madness.collision.databinding.InstantItemComplexBinding
import com.madness.collision.databinding.InstantItemShortcutBinding
import com.madness.collision.databinding.InstantItemSimpleBinding
import com.madness.collision.diy.SpanAdapter
import com.madness.collision.instant.shortcut.InstantShortcut
import com.madness.collision.main.MainViewModel
import com.madness.collision.misc.MiscApplication
import com.madness.collision.util.X
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

internal class InstantAdapter<T: InstantItem>(
        override val context: Context, private val mainViewModel: MainViewModel,
        private val dataType: Int, data: List<T> = emptyList()
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), SpanAdapter {

    companion object {
        private val ITEM_TYPE_SIMPLE = R.layout.instant_item_simple
        private val ITEM_TYPE_COMPLEX = R.layout.instant_item_complex
        private val ITEM_TYPE_SHORTCUT = R.layout.instant_item_shortcut
        const val TYPE_SHORTCUT = 0
        const val TYPE_TILE = 1
        const val TYPE_OTHER = 2
    }

    class InstantSimpleHolder(binding: InstantItemSimpleBinding): RecyclerView.ViewHolder(binding.root) {
        val card: MaterialCardView = binding.instantItemSimpleCard
        val switch: SwitchMaterial = binding.instantItemSimpleSwitch
        val title: TextView = binding.instantItemSimpleTitle
        val container: View = binding.instantItemSimpleContainer
    }

    class InstantComplexHolder(binding: InstantItemComplexBinding): RecyclerView.ViewHolder(binding.root) {
        val card: MaterialCardView = binding.instantItemComplexCard
        val switch: SwitchMaterial = binding.instantItemComplexSwitch
        val title: TextView = binding.instantItemComplexTitle
        val titleLayout: View = binding.instantItemComplexTitleLayout
    }

    class InstantShortcutHolder(binding: InstantItemShortcutBinding): RecyclerView.ViewHolder(binding.root) {
        val card: MaterialCardView = binding.instantItemShortcutCard
        val switch: SwitchMaterial = binding.instantItemShortcutSwitch
        val title: TextView = binding.instantItemShortcutTitle
        val titleLayout: View = binding.instantItemShortcutTitleLayout
        val pin: ImageView = binding.instantItemShortcutPin
        val divider: View = binding.instantItemShortcutDivider
    }

    override var spanCount: Int = 1
        set(value) {
            if (value > 0) field = value
        }
    private val mContext: Context = context
    private val mInflater = LayoutInflater.from(context)
    private var mData: List<T> = data
    private val instant: Instant?
    init {
        instant = if (isShortcut) {
            if (X.aboveOn(X.N_MR1)) {
                val manager = context.getSystemService(ShortcutManager::class.java)
                if (manager != null) Instant(context, manager) else null
            } else null
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
        return when {
            isShortcut -> ITEM_TYPE_SHORTCUT
            mData[position].hasDescription -> ITEM_TYPE_COMPLEX
            else -> ITEM_TYPE_SIMPLE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when(viewType) {
            ITEM_TYPE_SHORTCUT -> InstantShortcutHolder(InstantItemShortcutBinding.inflate(mInflater, parent, false))
            ITEM_TYPE_COMPLEX -> InstantComplexHolder(InstantItemComplexBinding.inflate(mInflater, parent, false))
            else -> InstantSimpleHolder(InstantItemSimpleBinding.inflate(mInflater, parent, false))
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
                optimizeSideMargin(position, 30f, 7f, holder.card)
                holder.title.text = name
                holder.container.setOnClickListener {
                    holder.switch.toggle()
                }
                switch = holder.switch
            }
            is InstantComplexHolder -> {
                optimizeSideMargin(position, 30f, 7f, holder.card)
                holder.title.text = name
                holder.titleLayout.setOnClickListener {
                    mainViewModel.displayFragment(item.descriptionPage!!)
                }
                switch = holder.switch
            }
            is InstantShortcutHolder -> {
                optimizeSideMargin(position, 30f, 7f, holder.card)
                holder.title.text = name
                holder.titleLayout.setOnClickListener {
                    val shortcutItem = item as InstantShortcut
                    if (X.aboveOn(X.N_MR1)) instant?.pinShortcut(shortcutItem.id)
                    else InstantCompat.pinShortcutLegacy(mContext, shortcutItem.id)
                }
                holder.pin.setOnClickListener {
                    val shortcutItem = item as InstantShortcut
                    if (X.aboveOn(X.N_MR1)) instant?.pinShortcut(shortcutItem.id)
                    else InstantCompat.pinShortcutLegacy(mContext, shortcutItem.id)
                }
                if (X.belowOff(X.N_MR1)) {
                    holder.switch.visibility = View.GONE
                    holder.divider.visibility = View.GONE
                }
                switch = holder.switch
            }
            else -> return
        }
        if (isShortcut && X.aboveOn(X.N_MR1)) {
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
            when {
                isLoadingComponents -> {
                    onLoadedCallbacks.add { checker.invoke() }
                }
                mComponents == null -> {
                    isLoadingComponents = true
                    GlobalScope.launch {
                        mComponents = MiscApplication.getComponents(mContext)
                        isLoadingComponents = false
                        checker.invoke()
                        onLoadedCallbacks.forEach { it.invoke() }
                    }
                }
                else -> {
                    checker.invoke()
                }
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
