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

package com.madness.collision.unit.api_viewing.list

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.madness.collision.Democratic
import com.madness.collision.R
import com.madness.collision.main.MainViewModel
import com.madness.collision.unit.api_viewing.Utils
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.data.EasyAccess
import com.madness.collision.unit.api_viewing.data.VerInfo
import com.madness.collision.unit.api_viewing.databinding.ApiDecentFragmentBinding
import com.madness.collision.unit.api_viewing.seal.SealManager
import com.madness.collision.util.TaggedFragment
import com.madness.collision.util.X
import com.madness.collision.util.adapted
import com.madness.collision.util.mainApplication
import com.madness.collision.util.os.SystemBarMaintainerOwner
import com.madness.collision.util.os.systemBars
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    private lateinit var viewBinding: ApiDecentFragmentBinding

    override fun createOptions(context: Context, toolbar: Toolbar, iconColor: Int): Boolean {
        toolbar.visibility = View.INVISIBLE
        (toolbar.tag as View?)?.isVisible = false  // hide toolbar divider
        // delay to avoid overriding by activity's configuration
        lifecycleScope.launch(Dispatchers.Main) {
            delay(30)
            updateBars()
        }
        return true
    }

    private fun updateBars() {
        val activity = activity
        if (activity !is SystemBarMaintainerOwner) return
        val systemBarMaintainer = activity.systemBarMaintainer
        with(activity) {
            val insets = systemBarMaintainer.activeInsets ?: return
            systemBars(insets, true) {
                top {
                    isDarkIcon = isDarkBar
                    transparentBar()
                }
                bottom {
                    isDarkIcon = isDarkBar
                    transparentBar()
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewBinding = ApiDecentFragmentBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val context = context ?: return

        val mainViewModel: MainViewModel by activityViewModels()
        democratize(mainViewModel)

        arguments?.apply {
            // catch android.os.BadParcelableException: ClassNotFoundException when unmarshalling:
            // com.madness.collision.unit.api_viewing.data.ApiViewingApp$ByteBuddy$... on an Oppo device
            try {
                viewModel.app = MutableLiveData(getParcelable(ARG_APP) ?: ApiViewingApp())
                viewModel.type = MutableLiveData(getInt(ARG_TYPE))
                val verLetter = getChar(ARG_VER_LETTER)
                val itemLength = getInt(ARG_ITEM_LENGTH)
                viewModel.back = MutableLiveData(SealManager.disposeSealBack(context, verLetter, itemLength))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        viewModel.app.observe(viewLifecycleOwner) {
            it?.run {
                viewBinding.apiDecentLogo.setImageBitmap(getOriginalIcon(context))
                viewBinding.apiDecentLabel.text = name
                viewBinding.apiDecentVer.text = verName
                when(viewModel.type.value) {
                    TYPE_TARGET -> VerInfo(targetAPI, isExact = true, isCompact = true)
                    TYPE_MINIMUM -> VerInfo(minAPI, isExact = true, isCompact = true)
                    else -> VerInfo(-1, "", ' ')
                }.run {
                    viewBinding.apiDecentChipAPI.text = apiText
                    viewBinding.apiDecentChipVer.run {
                        if (sdk.isNotEmpty()) text = sdk else visibility = View.GONE
                    }
                    if (EasyAccess.isSweet) {
                        val codeName = Utils.getAndroidCodenameByAPI(context, api)
                        val codeNameText = if (codeName.matches("""\d+""".toRegex())) codeName.toInt().adapted else codeName
                        viewBinding.apiDecentChipCodeName.text = codeNameText
                        if (viewBinding.apiDecentChipCodeName.text.isBlank()) viewBinding.apiDecentChipCodeName.visibility = View.GONE
                        val resId = SealManager.getAndroidCodenameImageRes(letter)
                        viewBinding.apiDecentChipCodeName.chipIcon = if (resId == 0) null else ContextCompat.getDrawable(context, resId)
                        val colorText = SealManager.getItemColorText(api)
                        arrayOf(viewBinding.apiDecentLabel, viewBinding.apiDecentChipAPI,
                                viewBinding.apiDecentChipVer, viewBinding.apiDecentChipCodeName,
                                viewBinding.apiDecentAPILabel, viewBinding.apiDecentVer).forEach { view ->
                            view.setTextColor(colorText)
                        }
                        viewBinding.apiDecentHeart.drawable.mutate().setTint(colorText)

                        isDarkBar = colorText == Color.BLACK
                        updateBars()
                    } else {
                        viewBinding.apiDecentChipCodeName.visibility = View.GONE
                    }
                    viewBinding.apiDecentGB.setGuidelineEnd(mainApplication.insetBottom)
                }
            }
        }
        viewModel.type.observe(viewLifecycleOwner) {
            when(it){
                TYPE_TARGET -> viewBinding.apiDecentAPILabel.text = getString(R.string.apiSdkTarget)
                TYPE_MINIMUM -> viewBinding.apiDecentAPILabel.text = getString(R.string.apiSdkMin)
            }
        }
        viewModel.back.observe(viewLifecycleOwner) {
            viewBinding.apiDecentShade.visibility = if (mainApplication.isDarkTheme) View.VISIBLE else View.GONE
            val reso = X.getCurrentAppResolution(context)
            viewBinding.apiDecentBack.background = BitmapDrawable(resources, X.toTarget(it, reso.x, reso.y))
        }
    }

}
