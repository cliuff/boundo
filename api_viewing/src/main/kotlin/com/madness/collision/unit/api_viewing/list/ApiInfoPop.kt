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
import android.widget.Toast
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Guideline
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.ChipGroup
import com.madness.collision.R
import com.madness.collision.main.MainViewModel
import com.madness.collision.misc.MiscApp
import com.madness.collision.unit.api_viewing.ApiViewingViewModel
import com.madness.collision.unit.api_viewing.AppTag
import com.madness.collision.unit.api_viewing.Utils
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.data.EasyAccess
import com.madness.collision.unit.api_viewing.data.VerInfo
import com.madness.collision.unit.api_viewing.databinding.AvShareBinding
import com.madness.collision.unit.api_viewing.seal.SealManager
import com.madness.collision.util.*
import com.madness.collision.util.controller.systemUi
import com.madness.collision.util.os.OsUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt
import com.madness.collision.unit.api_viewing.R as MyR

internal class ApiInfoPop: BottomSheetDialogFragment(), View.OnClickListener{

    companion object {
        private const val packageCoolApk = ApiViewingApp.packageCoolApk
        private const val packagePlayStore = ApiViewingApp.packagePlayStore
        private const val packageSettings = "com.android.settings"

        const val TAG = "APIInfoPop"
        const val ARG_APP = "app"

        private var initializedStoreLink = false
        private val storeMap = mutableMapOf<String, Bitmap>()

        fun clearStores() {
            initializedStoreLink = false
            storeMap.clear()
        }

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

    private var popStore: CollisionDialog? = null

    private val isAiAvailable = X.aboveOn(X.O)

    private var itemLength = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AppTheme_Pop)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        mViews = ViewHolder(inflater.inflate(MyR.layout.av_info_pop, container, false))
        return mViews.view
    }

    override fun onStart() {
        super.onStart()
        val context = context ?: return
        val rootView = view ?: return
        BottomSheetBehavior.from(rootView.parent as View).run {
            configure(context)
        }
    }

    override fun dismiss() {
        popStore?.dismiss()
        super.dismiss()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val context = context ?: return

        systemUi {
            val transparentNavBar = mainApplication.insetBottom == 0
            val isDarkNav = if (transparentNavBar) false else mainApplication.isPaleTheme
            val colorSurface = ThemeUtil.getColor(context, R.attr.colorASurface)
            val navBarColor = if (isDarkNav && OsUtils.dissatisfy(OsUtils.O)) {
                ColorUtil.darkenAs(colorSurface, 0.9f)
            } else {
                colorSurface
            }
            fullscreen()
            // keep status bar icon color untouched
            // Actually this works as intended only when app theme is set to follow system,
            // not configuring this icon color makes it follow dialog's style/theme,
            // which is defined in styles.xml and it follows system dark mode setting.
            // To fix this, set it to the window config of the activity before this.
            statusBar {
                transparentBar()
                // fix status bar icon color
                activity?.window?.let { window ->
                    isDarkIcon = SystemUtil.isDarkStatusIcon(window)
                }
            }
            navigationBar {
                isDarkIcon = isDarkNav
                isTransparentBar = transparentNavBar
                color = navBarColor
            }
        }

        val minMargin = X.size(context, 10f, X.DP).roundToInt()
        if (mainApplication.insetBottom < minMargin) {
            val extraMargin = minMargin - mainApplication.insetBottom
            mViews.guidelineBottom.setGuidelineEnd(extraMargin)
        }

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
                mvApp.icon.setImageBitmap(icon)
                if (isAiAvailable) {
                    if (adaptiveIcon) {
                        mViews.app.badge.drawable.setTint(X.getColor(context, R.color.androidRobotGreen))
                    } else {
                        mViews.app.badge.drawable.setTint(Color.LTGRAY)
                    }
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
        viewApiLevel.text = ver.api.toString()
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
            MyR.id.api_info_back -> actionStores(context, app)
            MyR.id.avAppInfoCapture -> actionShare(context)
            MyR.id.avAppInfoOptions -> {
                val parent = parentFragment
                if (parent !is AppList) return
                dismiss()
                parent.showAppOptions(app)
            }
        }
    }

    private fun actionStores(context: Context, app: ApiViewingApp) {
        if (!initializedStoreLink) {
            lifecycleScope.launch(Dispatchers.Default) {
                if (!initStores(context)) return@launch
                launch(Dispatchers.Main) {
                    // dismiss after viewModel access in initStores
                    dismiss()
                    showStores(context, app)
                }
            }
        } else {
            dismiss()
            showStores(context, app)
        }
    }

    private fun initStores(context: Context): Boolean {
        // by activityViewModels() may produce IllegalArgumentException due to getActivity null value
        // by activityViewModels() may produce IllegalStateException[java.lang.IllegalStateException: Can't access ViewModels from detached fragment]
        // due to fragment being detached?
        if (activity == null || isDetached || !isAdded) {
            X.toast(context, R.string.text_error, Toast.LENGTH_SHORT)
            return false
        }
        initializedStoreLink = true
        val searchSize = 3
        val avViewModel: ApiViewingViewModel by activityViewModels()
        // find from loaded apps
        val filtered = avViewModel.findApps(searchSize) {
            val pn = it.packageName
            pn == packageCoolApk || pn == packagePlayStore || pn == packageSettings
        }
        // load them if not loaded
        for (listApp in filtered) {
            if (listApp.preload) {
                listApp.load(context)
            }
            listApp.icon?.let {
                storeMap[listApp.packageName] = it
            }
        }
        // manually initialize those not found in loaded apps
        for (name in arrayOf(packageCoolApk, packagePlayStore, packageSettings)) {
            if (storeMap[name] != null) continue
            val pi = MiscApp.getPackageInfo(context, packageName = name)
            var app: ApiViewingApp? = null
            if (pi != null) {
                app = ApiViewingApp(context, pi, preloadProcess = true, archive = false)
                app.load(context, pi.applicationInfo)
            } else if (name == packageSettings) {
                app = ApiViewingApp.icon()
                app.load(context, { ContextCompat.getDrawable(context, R.mipmap.logo_settings)!! }, { null })
            }
            val icon = app?.icon
            if (icon != null) storeMap[name] = icon
        }
        return true
    }

    private fun showStores(context: Context, app: ApiViewingApp) {
        val vCoolApk : ImageView
        val vPlayStore : ImageView
        val vSettings: ImageView
        popStore = CollisionDialog(context, R.string.text_forgetit).apply {
            setListener { dismiss() }
            setTitleCollision(R.string.avStoreLink, 0, 0)
            setContent(0)
            setCustomContent(MyR.layout.pop_av_store_link)
            vCoolApk = findViewById(MyR.id.vCoolApk)
            vPlayStore = findViewById(MyR.id.vPlayStore)
            vSettings = findViewById(MyR.id.vSettings)
        }
        val iconWidth = X.size(context, 60f, X.DP).roundToInt()
        val storePackages = arrayOf(packageCoolApk to storeMap[packageCoolApk],
                packagePlayStore to storeMap[packagePlayStore])
        for ((name, storeIcon) in storePackages) {
            var listener: View.OnClickListener? = null
            if (storeIcon != null) {
                listener = View.OnClickListener {
                    try {
                        context.startActivity(app.storePage(name, true))
                    } catch (e: Exception) {
                        e.printStackTrace()
                        context.startActivity(app.storePage(name, false))
                    }
                    popStore?.dismiss()
                }
            }
            when(name) {
                packageCoolApk -> vCoolApk
                packagePlayStore -> vPlayStore
                else -> null
            }?.run {
                if (storeIcon == null) {
                    setPadding(0, 0, 0, 0)
                } else {
                    setImageBitmap(X.toTarget(storeIcon, iconWidth, iconWidth))
                    setOnClickListener(listener)
                }
            }
        }

        val settingsIcon = storeMap[packageSettings]
        if (settingsIcon != null && (app.isNotArchive || X.belowOff(X.Q))) {
            vSettings.setImageBitmap(X.toTarget(settingsIcon, iconWidth, iconWidth))
            vSettings.setOnClickListener {
                if (app.isNotArchive) {
                    context.startActivity(app.settingsPage())
                } else {
                    try {
                        context.startActivity(app.apkPage())
                    } catch (e: Exception) {
                        e.printStackTrace()
                        CollisionDialog.infoCopyable(context, app.appPackage.basePath).show()
                    }
                }
                popStore?.dismiss()
            }
        } else {
            vSettings.setPadding(0, 0, 0, 0)
        }
        popStore?.show()
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