package com.madness.collision.instant

import android.annotation.TargetApi
import android.content.ComponentName
import android.content.Context
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
import com.madness.collision.instant.tile.InstantTile
import com.madness.collision.main.MainViewModel
import com.madness.collision.util.X

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

    private val isShortcut: Boolean
        get() = dataType == TYPE_SHORTCUT

    private val isTile: Boolean
        get() = dataType == TYPE_TILE

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
        } else if (isTile) {
            val tileItem = item as InstantTile<*>
            val comp = ComponentName(mContext.packageName, tileItem.tileClass.qualifiedName ?: "")
            switch.isChecked = mContext.packageManager.getComponentEnabledSetting(comp) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
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
