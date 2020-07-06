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
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import com.madness.collision.Democratic
import com.madness.collision.R
import com.madness.collision.databinding.UnitDescBinding
import com.madness.collision.databinding.UnitDescCheckerBinding
import com.madness.collision.main.MainViewModel
import com.madness.collision.util.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

internal class UnitDescFragment : TaggedFragment(), Democratic {

    override val category: String = "UnitDesc"
    override val id: String = "UnitDesc"

    companion object {

        @JvmStatic
        fun newInstance(description: Description): UnitDescFragment {
            return UnitDescFragment().apply {
                this.description = description
            }
        }
    }

    private var _viewBinding: UnitDescBinding? = null
    private val viewBinding: UnitDescBinding
        get() = _viewBinding!!
    private val mViewModel: UnitDescViewModel by viewModels()
    private var icStar: Drawable? = null
    private var icStarred: Drawable? = null
    private var toolbar: Toolbar? = null

    private lateinit var description: Description
//        set(value) {
//            field = value
//            if (isAdded) mViewModel.description.value = value
//        }

    override fun createOptions(context: Context, toolbar: Toolbar, iconColor: Int): Boolean {
        toolbar.title = description.getName(context)
        inflateAndTint(R.menu.toolbar_unit_desc, toolbar, iconColor)
        this.toolbar = toolbar
        return true
    }

    override fun selectOption(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.unitDescToolbarPin -> {
                val unitName = mViewModel.description.value?.unitName ?: return false
                val context = context ?: return false
                val isPinned = item.icon == icStarred
                item.icon = getStarIcon(context, !isPinned)
                GlobalScope.launch {
                    Unit.togglePinned(context, unitName)
                }
                return true
            }
        }
        return false
    }

    private fun getStarIcon(context: Context, isStarred: Boolean): Drawable? {
        if (isStarred) {
            if (icStarred == null) {
                icStarred = context.getDrawable(R.drawable.ic_star_24)
                if (icStarred == null) return null
                icStarred!!.setTint(Color.parseColor("#A0FFC030"))
            }
            return icStarred
        } else {
            if (icStar == null) icStar = context.getDrawable(R.drawable.ic_star_border_24)
            icStar!!.setTint(ThemeUtil.getColor(context, R.attr.colorIcon))
            return icStar
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _viewBinding = UnitDescBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onDestroyView() {
        _viewBinding = null
        super.onDestroyView()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mViewModel.description.value = description
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val context = context ?: return
        val colorPass: Int by lazy { ThemeUtil.getColor(context, R.attr.colorActionPass) }
        val colorPassBack: Int by lazy { ThemeUtil.getBackColor(colorPass, 0.2f) }
        val colorAlert: Int by lazy { ThemeUtil.getColor(context, R.attr.colorActionAlert) }
        val colorAlertBack: Int by lazy { ThemeUtil.getColor(context, R.attr.colorActionAlertBack) }
        mViewModel.description.observe(viewLifecycleOwner) {
            val splitInstallManager = SplitInstallManagerFactory.create(context)
            val installedUnits = Unit.getInstalledUnits(splitInstallManager)
            val isInstalled = installedUnits.contains(it.unitName)
            val isAvailable = it.isAvailable(context)
            viewBinding.unitDescIcon.setImageDrawable(it.getIcon(context))

            val iconTint = viewBinding.unitDescIcon.imageTintList
            val installStatusColor = if (isInstalled) ColorStateList.valueOf(colorPass) else iconTint
            viewBinding.unitDescInstallStatus.run {
                setText(if (isInstalled) R.string.unit_desc_installed else R.string.unit_desc_not_installed)
                setTextColor(installStatusColor)
                compoundDrawablesRelative[0].setTintList(installStatusColor)
            }

            viewBinding.unitDescAvailability.run {
                setText(if (isAvailable) R.string.unit_desc_available else R.string.unit_desc_unavailable)
                compoundDrawablesRelative[0].setTint(if (isAvailable) colorPass else colorAlert)
            }

            viewBinding.unitDescAction.run {
                when {
                    isInstalled -> {
                        visibility = View.VISIBLE
                        setText(R.string.unit_desc_uninstall)
                        setTextColor(colorAlert)
                        setBackgroundColor(colorAlertBack)
                        setOnClickListener {
                            val unitManager = UnitManager(context, splitInstallManager)
                            if (!installedUnits.contains(description.unitName)) return@setOnClickListener
                            notifyBriefly(R.string.unit_desc_uninstall_notice)
                            GlobalScope.launch { unitManager.uninstallUnit(description, getView()) }
                        }
                    }
                    isAvailable -> {
                        visibility = View.VISIBLE
                        setText(R.string.unit_desc_install)
                        setTextColor(colorPass)
                        setBackgroundColor(colorPassBack)
                        setOnClickListener {
                            val unitManager = UnitManager(context, splitInstallManager)
                            if (installedUnits.contains(description.unitName)) return@setOnClickListener
                            notifyBriefly(R.string.unit_desc_install_notice)
                            GlobalScope.launch { unitManager.installUnit(description, getView()) }
                        }
                    }
                    else -> {
                        visibility = View.GONE
                    }
                }
            }

            if (it.hasChecker) {
                val inflater = LayoutInflater.from(context)
                val parent = viewBinding.unitDescCheckers
                it.checkers.forEach { checker ->
                    val checkerBinding = UnitDescCheckerBinding.inflate(inflater, parent, true)
                    val isCheckPassed = checker.check(context)
                    val icRes = if (isCheckPassed) R.drawable.ic_done_24 else R.drawable.ic_clear_24
                    checkerBinding.unitDescChecker.run {
                        text = checker.getName(context)
                        setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, icRes, 0)
                        compoundDrawablesRelative[2].setTint(if (isCheckPassed) colorPass else colorAlert)
                    }
                }
            }

            viewBinding.unitDescDesc.setMarginText(context, it.descRes)

            toolbar?.run {
                menu.findItem(R.id.unitDescToolbarPin).icon = getStarIcon(context, Unit.getIsPinned(context, it.unitName))
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
            viewBinding.unitDescContainer.alterPadding(bottom = it)
        }
    }
}
