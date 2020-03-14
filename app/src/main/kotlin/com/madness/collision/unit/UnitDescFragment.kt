package com.madness.collision.unit

import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
import com.madness.collision.util.ThemeUtil
import com.madness.collision.util.X
import com.madness.collision.util.alterPadding
import com.madness.collision.util.setMarginText
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

internal class UnitDescFragment : Fragment(), Democratic {

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

    private lateinit var description: Description
//        set(value) {
//            field = value
//            if (isAdded) mViewModel.description.value = value
//        }

    override fun createOptions(context: Context, toolbar: Toolbar, iconColor: Int): Boolean {
        toolbar.title = description.getName(context)
        return true
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
                            X.toast(context, R.string.unit_desc_uninstall_notice, Toast.LENGTH_SHORT)
                            GlobalScope.launch { unitManager.uninstallUnit(description) }
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
                            X.toast(context, R.string.unit_desc_install_notice, Toast.LENGTH_SHORT)
                            GlobalScope.launch { unitManager.installUnit(description) }
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
