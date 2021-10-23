/*
 * Copyright 2021 Clifford Liu
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

package com.madness.collision.unit.school_timetable

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import com.madness.collision.R
import com.madness.collision.settings.SettingsFunc
import com.madness.collision.unit.Unit
import com.madness.collision.unit.school_timetable.databinding.ActivityTtPart1Binding
import com.madness.collision.util.CollisionDialog
import com.madness.collision.util.X
import com.madness.collision.util.alterPadding
import com.madness.collision.util.measure
import kotlin.math.roundToInt
import com.madness.collision.unit.school_timetable.R as MyR

internal class TTManualFragment: Unit(), View.OnClickListener{

    override val id: String = "ST-Manual"

    companion object {
        @JvmStatic
        fun newInstance() = TTManualFragment()
    }

    private lateinit var viewBinding: ActivityTtPart1Binding

    override fun createOptions(context: Context, toolbar: Toolbar, iconColor: Int): Boolean {
        configNavigation(toolbar, iconColor)
        toolbar.setTitle(R.string.textManual)
        return true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val context = context
        if (context != null) SettingsFunc.updateLanguage(context)
        viewBinding = ActivityTtPart1Binding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val context = context ?: return
        democratize(mainViewModel)
        mainViewModel.contentWidthTop.observe(viewLifecycleOwner) {
            viewBinding.ttManualContainer.alterPadding(top = it)
        }
        mainViewModel.insetBottom.observe(viewLifecycleOwner) {
            val marginBottom = X.size(context, 10f, X.DP).roundToInt()
            viewBinding.ttManualActions.alterPadding(bottom = if (it > marginBottom) it else (marginBottom - it))

            viewBinding.ttManualActions.measure()
            viewBinding.ttManualContainer.alterPadding(bottom = viewBinding.ttManualActions.measuredHeight)
        }

        viewBinding.ttManualBrowsers.setOnClickListener(this)
        viewBinding.ttManualOK.setOnClickListener(this)
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