package com.madness.collision.unit.api_viewing

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.observe
import com.madness.collision.R
import com.madness.collision.main.MainViewModel
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.data.EasyAccess
import com.madness.collision.unit.api_viewing.data.VerInfo
import com.madness.collision.util.SystemUtil
import com.madness.collision.util.X
import com.madness.collision.util.alterPadding
import com.madness.collision.util.mainApplication
import kotlinx.android.synthetic.main.api_decent_fragment.*
import com.madness.collision.unit.api_viewing.R as MyR

internal class ApiDecentFragment : DialogFragment() {

    companion object {
        const val TYPE_TARGET = 1
        const val TYPE_MINIMUM = 2
        const val ARG_APP = "app"
        const val ARG_TYPE = "type"
        const val ARG_BACK = "back"
        const val TAG = "APIDecentFragment"

        @JvmStatic
        fun newInstance(app: ApiViewingApp, type: Int, back: Bitmap) = ApiDecentFragment().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_APP, app)
                putInt(ARG_TYPE, type)
                putParcelable(ARG_BACK, back)
            }
        }
    }

    private val viewModel: ApiDecentViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AppTheme_FullScreenDialog)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(MyR.layout.api_decent_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val context = context ?: return

        dialog?.window?.let {
            it.decorView.systemUiVisibility = it.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LOW_PROFILE
            SystemUtil.applyEdge2Edge(it)
        }

        val mainViewModel: MainViewModel by activityViewModels()
        mainViewModel.insetLeft.observe(viewLifecycleOwner){
            apiDecentBack.alterPadding(start = it)
        }
        mainViewModel.insetRight.observe(viewLifecycleOwner){
            apiDecentBack.alterPadding(end = it)
        }

        arguments?.apply {
            viewModel.app = MutableLiveData(getParcelable(ARG_APP) ?: ApiViewingApp())
            viewModel.type = MutableLiveData(getInt(ARG_TYPE))
            viewModel.back = MutableLiveData(getParcelable(ARG_BACK)!!)
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
                        apiDecentChipCodeName.chipIcon = if (resId == 0) null else context.getDrawable(resId)
                        val colorText = APIAdapter.getItemColorText(api)
                        arrayOf(apiDecentLabel, apiDecentChipAPI, apiDecentChipVer, apiDecentChipCodeName, apiDecentAPILabel, apiDecentVer).forEach { view ->
                            view.setTextColor(colorText)
                        }
                        apiDecentHeart.drawable.mutate().setTint(colorText)

                        val darkBar = colorText == Color.BLACK
                        dialog?.window?.let { window ->
                            SystemUtil.applyStatusBarColor(context, window, darkBar, isTransparentBar = true)
                            SystemUtil.applyNavBarColor(context, window, darkBar, isTransparentBar = true)
                        }
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

    override fun onStart() {
        super.onStart()
        if (dialog == null) return
        dialog!!.window?.run {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setWindowAnimations(R.style.AppTheme_PopUp)
        }
    }

}
