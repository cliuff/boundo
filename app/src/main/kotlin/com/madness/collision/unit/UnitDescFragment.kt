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

package com.madness.collision.unit

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.core.view.size
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import com.madness.collision.Democratic
import com.madness.collision.R
import com.madness.collision.databinding.UnitDescBinding
import com.madness.collision.databinding.UnitDescCheckerBinding
import com.madness.collision.main.MainViewModel
import com.madness.collision.util.*
import com.madness.collision.util.AppUtils.asBottomMargin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal class UnitDescFragment() : TaggedFragment(), Democratic {

    override val category: String = "UnitDesc"
    override val id: String = "UnitDesc"

    companion object {

        @JvmStatic
        fun newInstance(description: Description): UnitDescFragment {
            return UnitDescFragment(description)
        }
    }

    private var _viewBinding: UnitDescBinding? = null
    private val viewBinding: UnitDescBinding
        get() = _viewBinding!!
    // share data with unit manager fragment
    private val mViewModel: UnitDescViewModel by activityViewModels()
    private var icStar: Drawable? = null
    private var icStarred: Drawable? = null
    private var toolbar: Toolbar? = null

    private var description: Description? = null

    constructor(description: Description) : this() {
        this.description = description
    }

    override fun createOptions(context: Context, toolbar: Toolbar, iconColor: Int): Boolean {
        toolbar.title = description?.getName(context)
        inflateAndTint(R.menu.toolbar_unit_desc, toolbar, iconColor)
        this.toolbar = toolbar
        return true
    }

    override fun selectOption(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.unitDescToolbarPin -> {
                val context = context ?: return false
                val description = mViewModel.description.value ?: return false
                description.isPinned = !description.isPinned
                item.icon = getStarIcon(context, description.isPinned)
                mViewModel.notifyState(description)
                lifecycleScope.launch(Dispatchers.Default) {
                    Unit.togglePinned(context, description.unitName)
                }
                return true
            }
        }
        return false
    }

    private fun getStarIcon(context: Context, isStarred: Boolean): Drawable? {
        if (isStarred) {
            if (icStarred == null) {
                icStarred = ContextCompat.getDrawable(context, R.drawable.ic_star_24)
                if (icStarred == null) return null
                icStarred!!.setTint(Color.parseColor("#A0FFC030"))
            }
            return icStarred
        } else {
            if (icStar == null) icStar = ContextCompat.getDrawable(context, R.drawable.ic_star_border_24)
            icStar!!.setTint(ThemeUtil.getColor(context, R.attr.colorIcon))
            return icStar
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _viewBinding = UnitDescBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onDestroyView() {
        _viewBinding = null
        super.onDestroyView()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val context = context ?: return
        val unit = description?.unitName ?: return
        val states = DescRetriever(context).includePinState().retrieve(unit)
        if (states.isEmpty()) return
        mViewModel.description.value = states[0]
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val context = context ?: return
        val colorPass: Int by lazy { ThemeUtil.getColor(context, R.attr.colorActionPass) }
        val colorPassBack: Int by lazy { ThemeUtil.getBackColor(colorPass, 0.2f) }
        val colorAlert: Int by lazy { ThemeUtil.getColor(context, R.attr.colorActionAlert) }
        val colorAlertBack: Int by lazy { ThemeUtil.getColor(context, R.attr.colorActionAlertBack) }
        val splitInstallManager = SplitInstallManagerFactory.create(context)
        val installedUnits = Unit.getInstalledUnits(splitInstallManager)
        val unitManager = UnitManager(context, splitInstallManager)
        mViewModel.description.observe(viewLifecycleOwner) {
            it ?: return@observe
            // show unit icon
            viewBinding.unitDescIcon.setImageDrawable(it.description.getIcon(context))
            // show install status for dynamic unit
            if (it.isDynamic) {
                val iconTint = viewBinding.unitDescIcon.imageTintList
                val installStatusColor = if (it.isInstalled) ColorStateList.valueOf(colorPass) else iconTint
                viewBinding.unitDescInstallStatus.run {
                    setText(if (it.isInstalled) R.string.unit_desc_installed else R.string.unit_desc_not_installed)
                    setTextColor(installStatusColor)
                    compoundDrawablesRelative[0].setTintList(installStatusColor)
                    visibility = View.VISIBLE
                }
            } else {
                viewBinding.unitDescInstallStatus.visibility = View.GONE
            }
            // show availability
            viewBinding.unitDescAvailability.run {
                setText(if (it.isAvailable) R.string.unit_desc_available else R.string.unit_desc_unavailable)
                compoundDrawablesRelative[0].setTint(if (it.isAvailable) colorPass else colorAlert)
            }
            // show enable toggle button when present on device
            viewBinding.unitDescEnableToggle.run {
                when {
                    it.isEnabled -> {
                        setText(R.string.unit_desc_disable)
                        setOnClickListener { _ ->
                            it.run {
                                isEnabled = false
                                isPinned = false
                                mViewModel.updateAndNotifyState(this)
                            }
                            lifecycleScope.launch(Dispatchers.Default) {
                                unitManager.disableUnit(it.description)
                            }
                        }
                        visibility = View.VISIBLE
                    }
                    it.isDisabled && it.isInstalled && it.isAvailable -> {
                        setText(R.string.unit_desc_enable)
                        setOnClickListener { _ ->
                            it.run {
                                isEnabled = true
                                isPinned = Unit.getPinnedUnits(context).contains(unitName)
                                mViewModel.updateAndNotifyState(this)
                            }
                            lifecycleScope.launch(Dispatchers.Default) {
                                unitManager.enableUnit(it.description)
                            }
                        }
                        visibility = View.VISIBLE
                    }
                    else -> {
                        visibility = View.GONE
                    }
                }
            }
            // show install action for dynamic unit
            viewBinding.unitDescAction.run {
                when {
                    it.isDynamic && it.isInstalled -> {
                        setText(R.string.unit_desc_uninstall)
                        setTextColor(colorAlert)
                        setBackgroundColor(colorAlertBack)
                        setOnClickListener { _ ->
                            if (!installedUnits.contains(it.unitName)) return@setOnClickListener
                            notifyBriefly(R.string.unit_desc_uninstall_notice)
                            lifecycleScope.launch(Dispatchers.Default) {
                                unitManager.uninstallUnit(it.description, getView())
                            }
                        }
                        visibility = View.VISIBLE
                    }
                    it.isDynamic && it.isAvailable -> {
                        setText(R.string.unit_desc_install)
                        setTextColor(colorPass)
                        setBackgroundColor(colorPassBack)
                        setOnClickListener { _ ->
                            if (installedUnits.contains(it.unitName)) return@setOnClickListener
                            notifyBriefly(R.string.unit_desc_install_notice)
                            lifecycleScope.launch(Dispatchers.Default) {
                                unitManager.installUnit(it.description, getView())
                            }
                        }
                        visibility = View.VISIBLE
                    }
                    else -> {
                        visibility = View.GONE
                    }
                }
            }
            // hide install action when Google Play store is unavailable
            if (!ApiUtils.isGglPlayAvailable(context)) {
                viewBinding.unitDescAction.visibility = View.GONE
            }
            // show requirement checkers
            if (it.description.hasChecker) {
                val inflater = LayoutInflater.from(context)
                val parent = viewBinding.unitDescCheckers
                it.description.checkers.forEachIndexed { index, checker ->
                    // reuse view if possible
                    val checkerBinding = if (index < parent.size) UnitDescCheckerBinding.bind(parent[index])
                    else UnitDescCheckerBinding.inflate(inflater, parent, true)
                    val isCheckPassed = checker.check(context)
                    val icRes = if (isCheckPassed) R.drawable.ic_done_24 else R.drawable.ic_clear_24
                    checkerBinding.unitDescChecker.run {
                        text = checker.getName(context)
                        setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, icRes, 0)
                        compoundDrawablesRelative[2].setTint(if (isCheckPassed) colorPass else colorAlert)
                    }
                }
            }
            // show description
            val descResId = it.description.descRes
            if (descResId != 0) viewBinding.unitDescDesc.setText(descResId)
            // show pin state when enabled
            toolbar?.run {
                val menuItem = menu.findItem(R.id.unitDescToolbarPin)
                if (it.isEnabled) {
                    menuItem.icon = getStarIcon(context, it.isPinned)
                    menuItem.isVisible = true
                } else {
                    menuItem.isVisible = false
                }
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val mainViewModel: MainViewModel by activityViewModels()
        democratize(mainViewModel)

        mainViewModel.contentWidthTop.observe(viewLifecycleOwner) {
            viewBinding.unitDescContainer.alterPadding(top = it)
        }
        mainViewModel.contentWidthBottom.observe(viewLifecycleOwner) {
            viewBinding.unitDescContainer.alterPadding(bottom = asBottomMargin(it))
        }
    }
}
