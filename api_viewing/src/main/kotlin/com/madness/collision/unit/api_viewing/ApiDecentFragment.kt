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

package com.madness.collision.unit.api_viewing

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.observe
import com.madness.collision.Democratic
import com.madness.collision.R
import com.madness.collision.main.MainViewModel
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.data.EasyAccess
import com.madness.collision.unit.api_viewing.data.VerInfo
import com.madness.collision.util.SystemUtil
import com.madness.collision.util.TaggedFragment
import com.madness.collision.util.X
import com.madness.collision.util.mainApplication
import kotlinx.android.synthetic.main.api_decent_fragment.*
import com.madness.collision.unit.api_viewing.R as MyR

internal class ApiDecentFragment : TaggedFragment(), Democratic {

    override val category: String = "AV"
    override val id: String = "ApiDecent"

    companion object {
        const val TYPE_TARGET = 1
        const val TYPE_MINIMUM = 2
        const val ARG_APP = "app"
        const val ARG_TYPE = "type"
        const val ARG_VER_LETTER = "verLetter"
        const val ARG_ITEM_LENGTH = "itemLength"

        @JvmStatic
        fun newInstance(app: ApiViewingApp, type: Int, verLetter: Char, itemLength: Int) = ApiDecentFragment().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_APP, app)
                putInt(ARG_TYPE, type)
                putChar(ARG_VER_LETTER, verLetter)
                putInt(ARG_ITEM_LENGTH, itemLength)
            }
        }
    }

    private val viewModel: ApiDecentViewModel by viewModels()
    private var isDarkBar = false

    override fun createOptions(context: Context, toolbar: Toolbar, iconColor: Int): Boolean {
        toolbar.visibility = View.GONE
        updateBars()
        return true
    }

    private fun updateBars() {
        val context = context ?: return
        activity?.window?.let { window ->
            // Low profile mode is deprecated since Android 11.
            if (X.belowOff(X.R)) applyLowProfileModeLegacy(window)
            SystemUtil.applyStatusBarColor(context, window, isDarkBar, isTransparentBar = true)
            SystemUtil.applyNavBarColor(context, window, isDarkBar, isTransparentBar = true)
        }
    }

    @Suppress("deprecation")
    private fun applyLowProfileModeLegacy(window: Window) {
        val decorView = window.decorView
        decorView.systemUiVisibility = decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LOW_PROFILE
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(MyR.layout.api_decent_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val context = context ?: return

        val mainViewModel: MainViewModel by activityViewModels()
        democratize(mainViewModel)

        arguments?.apply {
            viewModel.app = MutableLiveData(getParcelable(ARG_APP) ?: ApiViewingApp())
            viewModel.type = MutableLiveData(getInt(ARG_TYPE))
            val verLetter = getChar(ARG_VER_LETTER)
            val itemLength = getInt(ARG_ITEM_LENGTH)
            viewModel.back = MutableLiveData(ApiInfoPop.disposeSealBack(context, verLetter, itemLength))
        }
        viewModel.app.observe(viewLifecycleOwner) {
            it?.run {
                apiDecentLogo.setImageBitmap(getOriginalIcon(context))
                apiDecentLabel.text = name
                apiDecentVer.text = verName
                when(viewModel.type.value) {
                     TYPE_TARGET -> VerInfo(targetAPI, Utils.getAndroidVersionByAPI(targetAPI, true), targetSDKLetter)
                    TYPE_MINIMUM -> VerInfo(minAPI, Utils.getAndroidVersionByAPI(minAPI, true), minSDKLetter)
                    else -> VerInfo(-1, "", ' ')
                }.run {
                    apiDecentChipAPI.text = api.toString()
                    apiDecentChipVer.text = sdk
                    if (EasyAccess.isSweet){
                        apiDecentChipCodeName.text = Utils.getAndroidCodenameByAPI(context, api)
                        if (apiDecentChipCodeName.text.isBlank()) apiDecentChipCodeName.visibility = View.GONE
                        val resId = APIAdapter.getAndroidCodenameImageRes(letter)
                        apiDecentChipCodeName.chipIcon = if (resId == 0) null else ContextCompat.getDrawable(context, resId)
                        val colorText = APIAdapter.getItemColorText(api)
                        arrayOf(apiDecentLabel, apiDecentChipAPI, apiDecentChipVer, apiDecentChipCodeName, apiDecentAPILabel, apiDecentVer).forEach { view ->
                            view.setTextColor(colorText)
                        }
                        apiDecentHeart.drawable.mutate().setTint(colorText)

                        isDarkBar = colorText == Color.BLACK
                        updateBars()
                    } else {
                        apiDecentChipCodeName.visibility = View.GONE
                    }
                    apiDecentGB.setGuidelineEnd(mainApplication.insetBottom)
                }
            }
        }
        viewModel.type.observe(viewLifecycleOwner) {
            when(it){
                TYPE_TARGET -> apiDecentAPILabel.text = getString(R.string.apiSdkTarget)
                TYPE_MINIMUM -> apiDecentAPILabel.text = getString(R.string.apiSdkMin)
            }
        }
        viewModel.back.observe(viewLifecycleOwner) {
            apiDecentShade.visibility = if (mainApplication.isDarkTheme) View.VISIBLE else View.GONE
            val reso = X.getCurrentAppResolution(context)
            apiDecentBack.background = BitmapDrawable(resources, X.toTarget(it, reso.x, reso.y))
        }
    }

}
