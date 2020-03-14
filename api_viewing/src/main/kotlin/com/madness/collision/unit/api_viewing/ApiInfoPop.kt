package com.madness.collision.unit.api_viewing

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
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
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.observe
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.card.MaterialCardView
import com.madness.collision.R
import com.madness.collision.unit.api_viewing.APIAdapter.Companion.sealBack
import com.madness.collision.unit.api_viewing.APIAdapter.Companion.seals
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.data.EasyAccess
import com.madness.collision.unit.api_viewing.data.VerInfo
import com.madness.collision.unit.api_viewing.databinding.AvShareBinding
import com.madness.collision.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.util.stream.Collectors
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

        fun newInstance(app: ApiViewingApp) = ApiInfoPop().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_APP, app)
            }
        }

        fun disposeSealBack(context: Context, letter: Char, itemLength: Int): Bitmap{
            if (sealBack.containsKey(letter)) return sealBack[letter]!!
            //final int INITIAL_colorPlain = -1;//color #00000000 has the value of 0
            val colorPlain: Int
            val dp60: Int
            var bitmap: Bitmap
            val resImageID = APIAdapter.getAndroidCodenameImageRes(letter)
            // draw color
            if (resImageID == 0) {
                val index4SealBack: Char
                if (letter == 'k' && EasyAccess.isSweet){
                    index4SealBack = letter
                    colorPlain = Color.parseColor("#753500")
                }else{
                    index4SealBack = '?'
                    if (sealBack.containsKey(index4SealBack)) return sealBack[index4SealBack]!!
                    colorPlain = X.getColor(context, R.color.androidRobotGreen)
                }
                dp60 = X.size(context, 60f, X.DP).toInt()
                //bitmap = Bitmap.createBitmap((if (X.belowOff(X.N)) X.size(context, 260f, X.DP).toInt() else dp60 * 2), dp60 * 2, Bitmap.Config.ARGB_8888)
                bitmap = Bitmap.createBitmap(dp60 * 2, dp60 * 2, Bitmap.Config.ARGB_8888)
                Canvas(bitmap).drawColor(colorPlain)
                sealBack[index4SealBack] = bitmap
                val path = F.createPath(F.valCachePubAvSeal(context), "back-$index4SealBack.png")
                if (F.prepare4(path)) X.savePNG(bitmap, path)
                return bitmap
            }
            // draw image res
            APIAdapter.populate4Seal(context, letter, itemLength)
            var seal: Bitmap = seals[letter]!!.collisionBitmap
            //boolean minNada = Build.VERSION.SDK_INT < Build.VERSION_CODES.N;
            val minNada = false
            var dp260 = 0
            if (minNada) dp260 = X.size(context, 260f, X.DP).toInt()
            val targetLength = seal.width / (if (minNada) 52 else 18)
            bitmap = Bitmap.createBitmap(
                    targetLength * (if (minNada) 13 else 4),
                    targetLength * (if (minNada) 9 else 4),
                    Bitmap.Config.ARGB_8888
            )
            seal = Bitmap.createBitmap(
                    seal,
                    targetLength * (if (minNada) 20 else 7),
                    targetLength * (if (minNada) 30 else 7),
                    bitmap.width,
                    bitmap.height
            )
            val canvas2Draw = Canvas(bitmap)
            canvas2Draw.drawColor(Color.parseColor("#fff5f5f5"))
            canvas2Draw.drawBitmap(seal, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG))
            dp60 = X.size(context, 60f, X.DP).toInt()
            bitmap = X.blurBitmap(
                    context, bitmap,
                    (if (minNada) dp260 else dp60 * 2),
                    dp60 * 2
            )
            sealBack[letter] = bitmap
            val path = F.createPath(F.valCachePubAvSeal(context), "back-$letter.png")
            if (F.prepare4(path)) X.savePNG(bitmap, path)
            return bitmap
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
        val capture: ImageButton = view.findViewById(MyR.id.apiCapture)

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

    private val aiAvailable = X.supportAdaptiveIconAvailable()
    private var initializedStoreLink = false
    private val storeMap = HashMap<String, ApiViewingApp>().toMutableMap()

//    private lateinit var applicationInfo: ApplicationInfo

    private var itemLength = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AppTheme_Pop)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mViews = ViewHolder(inflater.inflate(MyR.layout.av_info_pop, container, false))
        return mViews.view
    }

    override fun onStart() {
        super.onStart()
        val context = context ?: return
        val rootView = view ?: return
        BottomSheetBehavior.from(rootView.parent as View).configure(context)
    }

    override fun dismiss() {
        popStore?.dismiss()
        super.dismiss()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val context = context ?: return

        dialog?.window?.let {
            val transparentNavBar = mainApplication.insetBottom == 0
            val isDarkNav = if (transparentNavBar) false else mainApplication.isPaleTheme
            SystemUtil.applyEdge2Edge(it)
            SystemUtil.applyStatusBarColor(context, it, false, isTransparentBar = true)
            val colorSurface = ThemeUtil.getColor(context, R.attr.colorASurface)
            val color = if (isDarkNav && X.belowOff(X.O)) ColorUtil.darkenAs(colorSurface, 0.9f) else colorSurface
            SystemUtil.applyNavBarColor(context, it, isDarkNav, transparentNavBar, color = color)
        }

        val marginBottom = X.size(context, 10f, X.DP).roundToInt()
        if (mainApplication.insetBottom < marginBottom) mViews.guidelineBottom.setGuidelineEnd(marginBottom - mainApplication.insetBottom)

        // below: configure views
        mvApp.run {
            name.setOnClickListener { back.performClick() }

            name.setOnLongClickListener { back.performLongClick() }

            if (!aiAvailable) mViews.app.badge.visibility = View.GONE
        }
        // above: configure views

        itemLength = X.size(context, 70f, X.DP).roundToInt()
        arguments?.apply {
            viewModel.app = MutableLiveData(getParcelable(ARG_APP) ?: ApiViewingApp())
        }

        viewModel.app.observe(viewLifecycleOwner) {
            it?.run {
//                applicationInfo = context.packageManager.getApplicationInfo(packageName, PackageManager.PERMISSION_GRANTED) // todo archive case
                mViews.ver.text = verName
                mvApp.name.text = name
                mvApp.icon.setImageBitmap(icon)
                if (aiAvailable) {
                    if (adaptiveIcon) {
                        mViews.app.badge.drawable.setTint(X.getColor(context, R.color.androidRobotGreen))
                    } else {
                        mViews.app.badge.drawable.setTint(Color.LTGRAY)
                    }
                }

                disposeAPIInfo(VerInfo.targetDisplay(this), mvTarget)
                disposeAPIInfo(VerInfo.minDisplay(this), mvMin)
            }
        }

        arrayOf(mViews.target, mViews.min, mvApp.icon, mvApp.back, mViews.capture).forEach { it.listenedTimelyBy(this) }
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
            val colorText = APIAdapter.getItemColorText(ver.api)
            val views = arrayOf(viewAndroidVersion, viewApiLevel, viewApi)
            for (view in views) view.setTextColor(colorText)
        }

        val bitmap: Bitmap = disposeSealBack(context, ver.letter, itemLength)
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
                val back = X.drawableToBitmap(mvTarget.back.drawable)
                ApiDecentFragment.newInstance(app, ApiDecentFragment.TYPE_TARGET, back).let {
                    this.dismiss()
                    it.show(parentFragmentManager, ApiDecentFragment.TAG)
                }
            }
            MyR.id.sdk_info_api_min -> {
                val back = X.drawableToBitmap(mvMin.back.drawable)
                ApiDecentFragment.newInstance(app, ApiDecentFragment.TYPE_MINIMUM, back).let {
                    this.dismiss()
                    it.show(parentFragmentManager, ApiDecentFragment.TAG)
                }
            }
            MyR.id.sdkcheck_dialog_logo -> {
                AppIconFragment.newInstance(app.name, app.packageName, app.apkPath, app.isArchive()).let {
                    this.dismiss()
                    it.show(parentFragmentManager, AppIconFragment.TAG)
                }
            }
            MyR.id.api_info_back -> {
                GlobalScope.launch {
                    if (!initializedStoreLink) {
                        // by activityViewModels() may produce IllegalArgumentException due to getActivity null value
                        // by activityViewModels() may produce IllegalStateException[java.lang.IllegalStateException: Can't access ViewModels from detached fragment]
                        // due to fragment being detached?
                        if (activity == null || isDetached || !isAdded) {
                            X.toast(context, R.string.text_error, Toast.LENGTH_SHORT)
                            return@launch
                        }
                        initializedStoreLink = true
                        val searchSize = 3
                        var filtered: MutableList<ApiViewingApp> = mutableListOf()
                        val avViewModel: ApiViewingViewModel by activityViewModels()
                        avViewModel.apps4Cache.let {
                            if (X.aboveOn(Build.VERSION_CODES.N)) {
                                filtered = it.parallelStream().filter { apiApp ->
                                    apiApp.packageName == packageCoolApk ||
                                            apiApp.packageName == packagePlayStore ||
                                            apiApp.packageName == packageSettings
                                }.collect(Collectors.toList())
                            } else {
                                for (listApp in it) {
                                    if (listApp.packageName == packageCoolApk ||
                                            listApp.packageName == packagePlayStore
                                            || listApp.packageName == packageSettings)
                                        filtered.add(listApp)
                                    if (filtered.size == searchSize) break
                                }
                            }
                        }
                        launch(Dispatchers.Main) {
                            // dismiss after viewModel access
                            dismiss()
                        }
                        for (listApp in filtered) {
                            if (listApp.preload) listApp.load(context)
                            storeMap[listApp.packageName] = listApp
                        }
                        for (name in arrayOf(packageCoolApk, packagePlayStore, packageSettings)){
                            if (storeMap[name] != null) continue
                            try {
                                val pi = context.packageManager.getPackageInfo(name, 0)
                                storeMap[name] = ApiViewingApp(context, pi, preloadProcess = true, archive = false)
                                        .load(context, pi.applicationInfo)
                            } catch (e: PackageManager.NameNotFoundException) {
                                e.printStackTrace()
                                if (name == packageSettings){
                                    storeMap[name] = ApiViewingApp()
                                            .load(context, { context.getDrawable(R.mipmap.logo_settings)!! }, { null })
                                }
                            }
                        }
                    }

                    launch(Dispatchers.Main) {
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
                        for ((name, storeApp) in arrayOf(packageCoolApk to storeMap[packageCoolApk], packagePlayStore to storeMap[packagePlayStore])){
                            var icon: Bitmap? = null
                            var listener: View.OnClickListener? = null
                            storeApp?.let { store ->
                                icon = store.icon
                                listener = View.OnClickListener {
                                    try {
                                        context.startActivity(app.storePage(name, true))
                                    } catch (e: Exception) {
                                        context.startActivity(app.storePage(name, false))
                                    }
                                    popStore?.dismiss()
                                }
                            }
                            when(name){
                                packageCoolApk -> vCoolApk
                                packagePlayStore -> vPlayStore
                                else -> null
                            }?.run {
                                if (storeApp == null) {
                                    setPadding(0, 0, 0, 0)
                                }else{
                                    icon?.let { setImageBitmap(X.toTarget(it, iconWidth, iconWidth)) }
                                    setOnClickListener(listener)
                                }
                            }
                        }

                        val finalSettings = storeMap[packageSettings]
                        if (finalSettings != null && (app.isNotArchive() || X.belowOff(X.Q))) {
                            run {
                                finalSettings.icon
                            }?.let { vSettings.setImageBitmap(X.toTarget(it, iconWidth, iconWidth)) }
                            vSettings.setOnClickListener {
                                if (app.isNotArchive()) {
                                    context.startActivity(app.settingsPage())
                                } else {
                                    try {
                                        context.startActivity(app.apkPage())
                                    } catch (e: Exception) {
                                        CollisionDialog.infoCopyable(context, app.apkPath).show()
                                    }
                                }
                                popStore?.dismiss()
                            }
                        } else {
                            vSettings.setPadding(0, 0, 0, 0)
                        }
                        popStore?.show()
                    }
                }
            }
            MyR.id.apiCapture -> actionShare(context)
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
            val fileName = "${name}_share_${if (isFull) "full" else "stroked"}.png"
            val path = F.createPath(F.cachePublicPath(context), "App", "Logo", fileName)
            if (F.prepare4(path)) {
                X.savePNG(re, path)
                val uri = File(path).getProviderUri(context)
                val titleRes = if (isFull) MyR.string.av_share_full else MyR.string.av_share_stroked
                FilePop.by(context, uri, "image/png", titleRes, uri).show(fm, FilePop.TAG)
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