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

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Guideline
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePaddingRelative
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import coil.loadAny
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.ChipGroup
import com.madness.collision.R
import com.madness.collision.diy.WindowInsets
import com.madness.collision.main.MainViewModel
import com.madness.collision.unit.api_viewing.AppTag
import com.madness.collision.unit.api_viewing.Utils
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.data.AppPackageInfo
import com.madness.collision.unit.api_viewing.data.EasyAccess
import com.madness.collision.unit.api_viewing.data.VerInfo
import com.madness.collision.unit.api_viewing.databinding.AvShareBinding
import com.madness.collision.unit.api_viewing.seal.SealManager
import com.madness.collision.util.*
import com.madness.collision.util.os.BottomSheetEdgeToEdge
import com.madness.collision.util.os.DialogFragmentSystemBarMaintainer
import com.madness.collision.util.os.SystemBarMaintainer
import com.madness.collision.util.os.SystemBarMaintainerOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt
import com.madness.collision.unit.api_viewing.R as MyR

internal class ApiInfoPop: BottomSheetDialogFragment(), SystemBarMaintainerOwner, View.OnClickListener{

    companion object {
        const val ARG_APP = "app"

        fun newInstance(app: ApiViewingApp) = ApiInfoPop().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_APP, app)
            }
        }
    }

    @SuppressLint("WrongViewCast")
    class ViewHolder(val view: View) {
        val targetApi: ApiViewHolder = ApiViewHolder(view.findViewById(MyR.id.sdk_info_api_target))
        val minApi: ApiViewHolder = ApiViewHolder(view.findViewById(MyR.id.sdk_info_api_min))
        val app: AppViewHolder = AppViewHolder(view.findViewById(MyR.id.avInfoPopApp))
        val ver: AppCompatTextView = view.findViewById(MyR.id.apiInfoVer) as AppCompatTextView
        val guidelineBottom: Guideline = view.findViewById(MyR.id.sdk_info_guideline_bottom)
        val content: ConstraintLayout = view.findViewById(MyR.id.api_content)
        val capture: ImageButton = view.findViewById(MyR.id.avAppInfoCapture)
        val options: ImageButton = view.findViewById(MyR.id.avAppInfoOptions)
        val container: ViewGroup = view.findViewById(MyR.id.avAppInfoContainer)

        init {
            targetApi.apiTitle.setText(MyR.string.sdkcheck_dialog_targetsdktext)
            minApi.apiTitle.setText(MyR.string.sdkcheck_dialog_minsdktext)
        }
    }

    @SuppressLint("WrongViewCast")
    class ApiViewHolder(val view: View) {
        val back: ImageView = view.findViewById(MyR.id.sdk_info_api_target_back)
        val name: AppCompatTextView = view.findViewById(MyR.id.sdk_info_api_target_chip) as AppCompatTextView
        val api: AppCompatTextView = view.findViewById(MyR.id.sdkcheck_dialog_apptargetsdk) as AppCompatTextView
        val apiTitle: AppCompatTextView = view.findViewById(MyR.id.sdkcheck_dialog_apptargetsdkI) as AppCompatTextView
    }

    @SuppressLint("WrongViewCast")
    class AppViewHolder(val view: View) {
        val back: ImageView = view.findViewById(MyR.id.api_info_back)
        val icon: ImageView = view.findViewById(MyR.id.sdkcheck_dialog_logo)
        val name: AppCompatTextView = view.findViewById(MyR.id.sdkcheck_dialog_appname) as AppCompatTextView
        val badge: ImageView = view.findViewById(MyR.id.api_info_ai_icon)
        val tags: ChipGroup = view.findViewById(MyR.id.avAppInfoAppTags)
    }

    private val viewModel: ApiInfoViewModel by viewModels()
    override val systemBarMaintainer: SystemBarMaintainer = DialogFragmentSystemBarMaintainer(this)

    private lateinit var mViews: ViewHolder

    private val mvApp: AppViewHolder
        get() = mViews.app
    private val mvMin: ApiViewHolder
        get() = mViews.minApi
    private val mvTarget: ApiViewHolder
        get() = mViews.targetApi

    private val ViewHolder.min: MaterialCardView
        get() = minApi.view as MaterialCardView
    private val ViewHolder.target: MaterialCardView
        get() = targetApi.view as MaterialCardView

    private val isAiAvailable = X.aboveOn(X.O)

    private var itemLength = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // BottomSheetDialog style, set enableEdgeToEdge to true
        // (and navigationBarColor set to transparent or translucent)
        // to disable automatic insets handling
        setStyle(STYLE_NORMAL, R.style.AppTheme_Pop)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        mViews = ViewHolder(inflater.inflate(MyR.layout.av_info_pop, container, false))
        return mViews.view
    }

    private val edgeToEdge = BottomSheetEdgeToEdge(this, this::consumeInsets) { activity?.window }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val context = context ?: return
        edgeToEdge.applyInsets(view, context)
    }

    override fun onStart() {
        super.onStart()
        val context = context ?: return
        val rootView = view ?: return
        BottomSheetBehavior.from(rootView.parent as View).run {
            configure(context)
        }
    }

    private fun consumeInsets(insets: WindowInsets) {
        val context = context ?: return
        val minMargin = X.size(context, 10f, X.DP).roundToInt()
        val extraMargin = max(insets.bottom, minMargin)
        mViews.guidelineBottom.setGuidelineEnd(extraMargin)

        val margin = context.resources.getDimensionPixelOffset(MyR.dimen.avAppInfoOptionsMarginBottom)
        mViews.options.updateLayoutParams<ConstraintLayout.LayoutParams> {
            bottomMargin = extraMargin + margin
        }

        mViews.container.updatePaddingRelative(start = insets.start, end = insets.end)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val context = context ?: return

        // below: configure views
        mvApp.run {
            name.setOnClickListener { back.performClick() }
            tags.setOnClickListener { back.performClick() }

            name.setOnLongClickListener { back.performLongClick() }
            tags.setOnLongClickListener { back.performLongClick() }

            if (!isAiAvailable) mViews.app.badge.visibility = View.GONE
        }
        // above: configure views

        itemLength = X.size(context, 45f, X.DP).roundToInt()
        arguments?.apply {
            viewModel.app = MutableLiveData(getParcelable(ARG_APP) ?: ApiViewingApp())
        }

        viewModel.app.observe(viewLifecycleOwner) {
            it?.run {
//                applicationInfo = context.packageManager.getApplicationInfo(packageName, PackageManager.PERMISSION_GRANTED) // todo archive case
                mViews.ver.text = verName
                mvApp.name.text = name
                mvApp.icon.loadAny(AppPackageInfo(context, this))
                if (isAiAvailable) {
                    val tint = if (adaptiveIcon) X.getColor(context, R.color.androidRobotGreen) else Color.LTGRAY
                    mViews.app.badge.drawable.setTint(tint)
                }
                lifecycleScope.launch(Dispatchers.Default) {
                    AppTag.inflateAllTagsAsync(context, mvApp.tags, it)
                }

                disposeAPIInfo(VerInfo.targetDisplay(this), mvTarget)
                disposeAPIInfo(VerInfo.minDisplay(this), mvMin)
            }
        }

        arrayOf(mViews.target, mViews.min, mvApp.icon, mvApp.back, mViews.capture, mViews.options).forEach {
            it.setOnClickListener(this)
        }
    }

    private fun disposeAPIInfo(ver: VerInfo, apiViewHolder: ApiViewHolder) {
        val apiCard: MaterialCardView = apiViewHolder.view as MaterialCardView
        val viewAndroidVersion: TextView = apiViewHolder.name
        val viewApiLevel: TextView = apiViewHolder.api
        val viewApi: TextView = apiViewHolder.apiTitle
        val context = context ?: return
        viewApiLevel.text = ver.apiText
        viewAndroidVersion.text = ver.displaySdk
        if (EasyAccess.isSweet){
            val colorText = SealManager.getItemColorText(ver.api)
            val views = arrayOf(viewAndroidVersion, viewApiLevel, viewApi)
            for (view in views) view.setTextColor(colorText)
        }

        val bitmap: Bitmap = SealManager.disposeSealBack(context, ver.letter, itemLength)
        val paramsTarget = apiCard.layoutParams as ConstraintLayout.LayoutParams
        paramsTarget.width = bitmap.width
        paramsTarget.height = bitmap.height
        val apiBack = apiViewHolder.back
        apiBack.setImageBitmap(bitmap)
    }

    private val infoService = AppInfoService()
    override fun onClick(v: View?) {
        v ?: return
        val context = context ?: return
        val app = viewModel.app.value!!
        when(v.id){
            MyR.id.sdk_info_api_target -> {
                val verLetter = Utils.getAndroidLetterByAPI(app.targetAPI)
                ApiDecentFragment.newInstance(app, ApiDecentFragment.TYPE_TARGET, verLetter, itemLength).let {
                    this.dismiss()
                    val mvm: MainViewModel by activityViewModels()
                    mvm.displayFragment(it)
                }
            }
            MyR.id.sdk_info_api_min -> {
                val verLetter = Utils.getAndroidLetterByAPI(app.minAPI)
                ApiDecentFragment.newInstance(app, ApiDecentFragment.TYPE_MINIMUM, verLetter, itemLength).let {
                    this.dismiss()
                    val mvm: MainViewModel by activityViewModels()
                    mvm.displayFragment(it)
                }
            }
            MyR.id.sdkcheck_dialog_logo -> {
                AppIconFragment.newInstance(app.name, app.packageName, app.appPackage.basePath, app.isArchive).let {
                    this.dismiss()
                    val mvm: MainViewModel by activityViewModels()
                    mvm.displayFragment(it)
                }
            }
            MyR.id.api_info_back -> infoService.actionStores(this, app, lifecycleScope)
            MyR.id.avAppInfoCapture -> actionShare(context)
            MyR.id.avAppInfoOptions -> {
                val parent = parentFragment
                if (parent !is AppList) return
                dismiss()
                parent.showAppOptions(app)
            }
        }
    }

    private fun actionShare(context: Context) {
        val viewHolder = mViews
        val name = viewModel.app.value!!.name
        val fm = parentFragmentManager
        val views = AvShareBinding.inflate(layoutInflater)
        val dialog = CollisionDialog(context, R.string.text_cancel).apply {
            setTitleCollision(0, 0, 0)
            setContent(0)
            setCustomContent(views.root)
            setListener { dismiss() }
            this@ApiInfoPop.dismiss()
            show()
        }
        val images = getShareImage(context, viewHolder)
        views.avShareFullImage.setImageBitmap(images[0])
        views.avShareStrokedImage.setImageBitmap(images[1])
        val share: (isFull: Boolean) -> Unit = { isFull ->
            val re = images[if (isFull) 0 else 1]
            val fileTitle = "${name}_share_${if (isFull) "full" else "stroked"}"
            val fileName = "$fileTitle.png"
            val path = F.createPath(F.cachePublicPath(context), "App", "Logo", fileName)
            if (F.prepare4(path)) {
                X.savePNG(re, path)
                val uri = File(path).getProviderUri(context)
                val titleRes = if (isFull) MyR.string.av_share_full else MyR.string.av_share_stroked
                FilePop.by(context, uri, "image/png", titleRes, uri, fileTitle).show(fm, FilePop.TAG)
            }
        }
        views.avShareFullImage.setOnClickListener {
            dialog.dismiss()
            share.invoke(true)
        }
        views.avShareStrokedImage.setOnClickListener {
            dialog.dismiss()
            share.invoke(false)
        }
    }

    private fun getShareImage(context: Context, views: ViewHolder): List<Bitmap> {
        return shareImage(context, views, { canvas, _, _, _ ->
            canvas.drawColor(ThemeUtil.getColor(context, R.attr.colorASurface))
        }, { canvas, width, height, padding ->
            val length = max(width, height)
            val radius = (length / 2).toFloat()
            val paddingFloat = padding.toFloat()
            val path = GraphicsUtil.getSquirclePath(paddingFloat, paddingFloat, radius)
            val paint = Paint().apply {
                isAntiAlias = true
                color = ThemeUtil.getColor(context, R.attr.colorASurface)
            }
            canvas.drawPath(path, paint)
            paint.apply {
                style = Paint.Style.STROKE
                strokeWidth = X.size(context, 1f, X.DP)
                color = ThemeUtil.getColor(context, R.attr.colorStroke)
            }
            canvas.drawPath(path, paint)
        })
    }

    private fun shareImage(context: Context, views: ViewHolder, vararg back: (canvas: Canvas, width: Int, height: Int, padding: Int) -> Unit): List<Bitmap> {
        val padding = X.size(context, 10f, X.DP).toInt()
        val paddingFloat = padding.toFloat()
        val paddingBottom = X.size(context, 30f, X.DP).toInt()
        val oWidth = views.content.width
        val oHeight = views.content.height
        val width = views.content.width + padding * 2
        val height = views.content.height + padding * 2 + paddingBottom
        val length = max(width, height)
        val left = ((length - width) / 2).toFloat() + paddingFloat
        val top = ((length - height) / 2).toFloat() + paddingFloat

        views.targetApi.back.setImageBitmap(X.circularBitmap(X.drawableToBitmap(views.targetApi.back.drawable).collisionBitmap))
        views.minApi.back.setImageBitmap(X.circularBitmap(X.drawableToBitmap(views.minApi.back.drawable).collisionBitmap))

        if (back.isEmpty()) return emptyList()
        val result: MutableList<Bitmap> = ArrayList(back.size)
        back.forEach {
            val re = Bitmap.createBitmap(length, length, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(re)
            it.invoke(canvas, oWidth, oHeight, padding)
            canvas.translate(left, top)
            views.content.draw(canvas)
            result.add(re)
        }
        return result
    }

}