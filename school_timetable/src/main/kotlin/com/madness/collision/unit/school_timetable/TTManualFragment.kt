package com.madness.collision.unit.school_timetable

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.observe
import com.madness.collision.R
import com.madness.collision.settings.SettingsFunc
import com.madness.collision.unit.Unit
import com.madness.collision.util.CollisionDialog
import com.madness.collision.util.X
import com.madness.collision.util.alterPadding
import com.madness.collision.util.measure
import kotlinx.android.synthetic.main.activity_tt_part1.*
import kotlin.math.roundToInt
import com.madness.collision.unit.school_timetable.R as MyR

internal class TTManualFragment: Unit(), View.OnClickListener{
    companion object {
        @JvmStatic
        fun newInstance() = TTManualFragment()
    }

    override fun createOptions(context: Context, toolbar: Toolbar, iconColor: Int): Boolean {
        toolbar.setTitle(R.string.textManual)
        return true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val context = context
        if (context != null) SettingsFunc.updateLanguage(context)
        return inflater.inflate(MyR.layout.activity_tt_part1, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val context = context ?: return
        democratize(mainViewModel)
        mainViewModel.contentWidthTop.observe(viewLifecycleOwner) {
            ttManualContainer.alterPadding(top = it)
        }
        mainViewModel.insetBottom.observe(viewLifecycleOwner) {
            val marginBottom = X.size(context, 10f, X.DP).roundToInt()
            ttManualActions.alterPadding(bottom = if (it > marginBottom) it else (marginBottom - it))

            ttManualActions.measure()
            ttManualContainer.alterPadding(bottom = ttManualActions.measuredHeight)
        }

        ttManualBrowsers.setOnClickListener(this)
        ttManualOK.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        v ?: return
        val context = context ?: return
        when (v.id) {
            MyR.id.ttManualBrowsers -> CollisionDialog.alert(context, MyR.string.timetable_manual_browsers).show()
            MyR.id.ttManualOK -> mainViewModel.popUpBackStack()
        }
    }

}