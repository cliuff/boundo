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

package com.madness.collision.unit.api_viewing.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.content.pm.PackageInfoCompat
import androidx.room.*
import com.madness.collision.R
import com.madness.collision.misc.MiscApp
import com.madness.collision.settings.SettingsFunc
import com.madness.collision.unit.api_viewing.Utils
import com.madness.collision.unit.api_viewing.util.ApkUtil
import com.madness.collision.unit.api_viewing.util.ManifestUtil
import com.madness.collision.util.*
import com.madness.collision.util.os.OsUtils
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

@Entity(tableName = "app")
open class ApiViewingApp(@PrimaryKey @ColumnInfo var packageName: String) : Parcelable, Cloneable {

    companion object {
        const val packageCoolApk = "com.coolapk.market"
        const val packagePlayStore = "com.android.vending"
        const val pageSettings = "settings"
        const val pageApk = "apk"

        const val packagePackageInstaller = "com.google.android.packageinstaller"

        private const val TYPE_ICON = 2
        private const val TYPE_APP = 1
        //private static int apiCeiling = Build.VERSION_CODES.P;

        @JvmField
        val CREATOR: Parcelable.Creator<ApiViewingApp> = object : Parcelable.Creator<ApiViewingApp> {
            override fun createFromParcel(parIn: Parcel) = ApiViewingApp(parIn)
            override fun newArray(size: Int) = Array<ApiViewingApp?>(size){ null }
        }

        fun icon(): ApiViewingApp {
            return ApiViewingApp().apply { type = TYPE_ICON }
        }
    }

    var name: String = ""
    var verName = ""
    var verCode = 0L
    var targetAPI: Int = -1
    var minAPI: Int = -1
    var apiUnit: Int = ApiUnit.NON
    var updateTime = 0L
    var isNativeLibrariesRetrieved = false
    var nativeLibraries: BooleanArray = BooleanArray(ApkUtil.NATIVE_LIB_SUPPORT_SIZE) { false }
    var isLaunchable = false
    lateinit var appPackage: AppPackage

    @Ignore
    var icon: Bitmap? = null
    @Ignore
    var targetSDK: String = ""
    @Ignore
    var minSDK: String = ""
    @Ignore
    var targetSDKDisplay: String = ""
    @Ignore
    var minSDKDisplay: String = ""
    @Ignore
    var targetSDKDouble: Double = -1.0
    @Ignore
    var minSDKDouble: Double = -1.0
    @Ignore
    var targetSDKLetter: Char = '?'
    @Ignore
    var minSDKLetter: Char = '?'
    @Ignore
    var adaptiveIcon: Boolean = false
    @Ignore
    var preload: Boolean = false
    @Ignore
    private var type: Int = TYPE_APP
    @Ignore
    var isLoadingIcon: Boolean = false
        private set
    @Ignore
    var iconRetrievingDetails: IconRetrievingDetails? = null

    val isArchive: Boolean
        get() = apiUnit == ApiUnit.APK
    val isNotArchive: Boolean
        get() = !isArchive
    private val isIconDefined: Boolean
        get() = iconRetrievingDetails != null
    private val iconDetails: IconRetrievingDetails
        get() = iconRetrievingDetails!!
    val hasIcon: Boolean
        get() = icon != null

    constructor(): this("")

    constructor(context: Context, info: PackageInfo, preloadProcess: Boolean, archive: Boolean)
            : this(info.packageName ?: "") {
        init(context, info, preloadProcess, archive)
    }

    public override fun clone(): Any {
        return (super.clone() as ApiViewingApp).apply {
            nativeLibraries = nativeLibraries.copyOf()
        }
    }

    /**
     * Initialize ignored properties
     */
    fun initIgnored() {
        icon = null
        initApiVer()
        adaptiveIcon = false
        preload = true
        type = TYPE_APP
        isLoadingIcon = false
        iconRetrievingDetails = null
    }

    fun init(context: Context, info: PackageInfo, preloadProcess: Boolean, archive: Boolean) {
        if (preloadProcess) {
            packageName = info.packageName
            type = TYPE_APP
            verName = info.versionName ?: ""
            verCode = PackageInfoCompat.getLongVersionCode(info)
            updateTime = info.lastUpdateTime
            preload = true
            if (archive) {
                apiUnit = ApiUnit.APK
                name = packageName
            } else {
                val isNotSysApp = (info.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0
                apiUnit = if (isNotSysApp) ApiUnit.USER else ApiUnit.SYS
                appPackage = AppPackage(info.applicationInfo)
                loadName(context, info.applicationInfo)
                //name = manager.getApplicationLabel(pi.applicationInfo).toString();
            }

            // API ver
            targetAPI = info.applicationInfo.targetSdkVersion
            minAPI = if (OsUtils.satisfy(OsUtils.N)) {
                info.applicationInfo.minSdkVersion
            } else {
                val minApiText = ManifestUtil.getMinSdk(appPackage.basePath)
                if (minApiText.isNotEmpty()) minApiText.toInt() else -1 // fix cloneable
            }
            initApiVer()

            val pm = context.packageManager
            isLaunchable = pm.getLaunchIntentForPackage(packageName) != null
        } else {
            load(context, info.applicationInfo)
        }
    }

    private fun initApiVer() {
        // target API ver
        targetSDK = Utils.getAndroidVersionByAPI(targetAPI, false)
        if (targetSDK.isNotEmpty()) {
            targetSDKDouble = targetSDK.toDouble()
            targetSDKDisplay = targetSDK
        } else {
            // fix cloneable
            targetSDKDouble = -1.0
            targetSDKDisplay = ""
        }
        targetSDKLetter = Utils.getAndroidLetterByAPI(targetAPI)
        // min API ver
        minSDK = Utils.getAndroidVersionByAPI(minAPI, false)
        if (minSDK.isNotEmpty()) {
            minSDKDouble = minSDK.toDouble()
            minSDKDisplay = minSDK
        } else {
            // fix cloneable
            minSDKDouble = -1.0
            minSDKDisplay = ""
        }
        minSDKLetter = Utils.getAndroidLetterByAPI(minAPI)
    }

    fun initArchive(context: Context, applicationInfo: ApplicationInfo): ApiViewingApp {
        appPackage = AppPackage(applicationInfo)
        loadName(context, applicationInfo)
        //name = manager.getApplicationLabel(pi.applicationInfo).toString();
        return this
        //this.name = file.getName() + "\n" + file.getParent();
    }

    private fun loadName(context: Context, applicationInfo: ApplicationInfo) {
        loadName(context, applicationInfo, mainApplication.debug)
    }

    private fun loadName(context: Context, applicationInfo: ApplicationInfo, overrideSystem: Boolean){
        if (!overrideSystem){
            name = context.packageManager.getApplicationLabel(applicationInfo).toString()
            return
        }
        // below: unable to create context for Android System
        if (packageName == "android") {
            loadName(context, applicationInfo, false)
            return
        }
        try {
            val la = SettingsFunc.getLanguage(context)
            if (SystemUtil.getLocaleUsr(context).toString() == SystemUtil.getLocaleApp().toString()){
                loadName(context, applicationInfo, false)
            }else {
                val nContext = context.createPackageContext(packageName, Context.CONTEXT_RESTRICTED)
                val localeContext = SystemUtil.getLocaleContext(nContext, SettingsFunc.getLocale(la))
                val labelRes = localeContext.applicationInfo.labelRes
                name = if (labelRes == 0) {
                    ""
                } else {
                    try {
                        localeContext.getString(labelRes)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        ""
                    }
                }
            }
        } catch ( e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        } finally {
            if (name.isEmpty()) loadName(context, applicationInfo, false)
        }
    }

    fun getApplicationInfo(context: Context): ApplicationInfo? {
        return if (this.isArchive) MiscApp.getApplicationInfo(context, apkPath = appPackage.basePath)
        else MiscApp.getApplicationInfo(context, packageName = packageName)
    }

    fun getOriginalIcon(context: Context): Bitmap? {
        return if (isIconOriginal && icon != null) icon else getOriginalIconDrawable(context)?.let { X.drawableToBitmap(it) }
    }

    fun getOriginalIconDrawable(context: Context, applicationInfo: ApplicationInfo? = getApplicationInfo(context)): Drawable? {
        applicationInfo ?: return null
        return context.packageManager.getApplicationIcon(applicationInfo)
    }

    /**
     * for quick load of both app and apk
     */
    fun load(context: Context, applicationInfo: ApplicationInfo? = getApplicationInfo(context)): ApiViewingApp {
        return when (type) {
            TYPE_APP -> {
                applicationInfo ?: return this
                load(context, {
                    getOriginalIconDrawable(context, applicationInfo)!!.mutate()
                }, { ManifestUtil.getRoundIcon(context, applicationInfo, appPackage.basePath) })
            }
            TYPE_ICON -> throw IllegalArgumentException("instance of TYPE_ICON must provide icon retrievers")
            else -> this
        }
    }

    fun load(context: Context, retrieverLogo: () -> Drawable, retrieverRound: () -> Drawable?): ApiViewingApp {
        if (!preload || isLoadingIcon || hasIcon) return this
        preload = false
        isLoadingIcon = true
        loadLogo(context, retrieverLogo, retrieverRound)
        isLoadingIcon = false
        return this
    }

    private fun loadLogo(context: Context, retrieverLogo: () -> Drawable,  retrieverRound: () -> Drawable?) {
        val isDefined = isIconDefined
        if (!isDefined) {
            iconRetrievingDetails = IconRetrievingDetails()
        }

        val iconDrawable = retrieverLogo.invoke()
        retrieveAppIconInfo(iconDrawable)
        val originalIcon: Bitmap? by lazy { retrieveAppIcon(context, isDefined, iconDrawable) }
        icon = if (EasyAccess.shouldRoundIcon) {
            val (roundIcon, roundIconDrawable) = adaptiveRoundIcon(context, iconDrawable, retrieverRound)
            roundIcon ?: originalIcon?.let { roundIcon(context, isDefined, it, roundIconDrawable) }
        } else {
            originalIcon
        }
    }

    val isIconOriginal: Boolean
        get() = !EasyAccess.shouldRoundIcon

    fun retrieveAppIconInfo(iconDrawable: Drawable) {
        adaptiveIcon = OsUtils.satisfy(OsUtils.O) && iconDrawable is AdaptiveIconDrawable
    }

    private fun retrieveAppIcon(context: Context, isDefined: Boolean, iconDrawable: Drawable): Bitmap? {
        var logoDrawable = iconDrawable
        if (isDefined) {
            if (iconDetails.isDefault) {
                logoDrawable = ContextCompat.getDrawable(context, R.drawable.ic_android_24) ?: return null
            }
        } else {
            iconDetails.width = logoDrawable.intrinsicWidth
            iconDetails.height = logoDrawable.intrinsicHeight
            if (iconDetails.width <= 0 || iconDetails.height <= 0) {
                iconDetails.isDefault = true
                logoDrawable = ContextCompat.getDrawable(context, R.drawable.ic_android_24) ?: return null
                iconDetails.width = logoDrawable.intrinsicWidth
                iconDetails.height = logoDrawable.intrinsicHeight
            }

            // below: shrink size if it's too large in case of consuming too much memory
            iconDetails.standardWidth = X.size(context, 72f, X.DP).roundToInt()
            val maxLength = max(iconDetails.width, iconDetails.height)
            if (maxLength > iconDetails.standardWidth){
                val fraction: Float = iconDetails.standardWidth.toFloat() / maxLength
                iconDetails.width = (iconDetails.width * fraction).roundToInt()
                iconDetails.height = (iconDetails.height * fraction).roundToInt()
            }
        }

        if (iconDetails.width <= 0 || iconDetails.height <= 0) return null
        var logo = Bitmap.createBitmap(iconDetails.width, iconDetails.height, Bitmap.Config.ARGB_8888)
        logoDrawable.setBounds(0, 0, iconDetails.width, iconDetails.height)
        logoDrawable.draw(Canvas(logo))
        // make it square and properly centered
        logo = GraphicsUtil.properly2Square(logo)
        return X.toTarget(logo, iconDetails.standardWidth, iconDetails.standardWidth)
    }

    /**
     * @return final icon, raw round icon from retriever
     */
    private fun adaptiveRoundIcon(context: Context, logoDrawable: Drawable, retrieverRound: () -> Drawable?): Pair<Bitmap?, Drawable?> {
        // below: adaptive round icon
        if (OsUtils.satisfy(OsUtils.O) && adaptiveIcon) {
            return GraphicsUtil.drawAIRound(context, logoDrawable) to null
        }
        // below: retrieve round icon from package
        var roundIcon: Drawable? = null
        if (EasyAccess.shouldManifestedRound) {
            roundIcon = retrieverRound()
            if (OsUtils.satisfy(OsUtils.O) && roundIcon is AdaptiveIconDrawable) {
                return GraphicsUtil.drawAIRound(context, roundIcon) to roundIcon
            }
        }
        return null to roundIcon
    }

    /**
     * make icon round
     */
    private fun roundIcon(context: Context, isDefined: Boolean, logo: Bitmap, roundIconDrawable: Drawable?): Bitmap {
        val doPackageIcon = roundIconDrawable != null
        // below: convert into round
        // the higher the harder to be recognized as round icon
        val alphaLimit = 0x90
        val icon2Clip by lazy {
            GraphicsUtil.removeOuterTransparentPixels(logo, noTransparency = false, alphaLimit = alphaLimit).let {
                GraphicsUtil.properly2Square(it)
            }
        }
        if (!isDefined && !doPackageIcon && EasyAccess.shouldClip2Round){
            val radius = X.size(context, 10f, X.DP).roundToInt()
            val radiusFloat = radius.toFloat()
            val diameter = 2 * radius

            val sample = X.toTarget(icon2Clip, diameter, diameter)
            // exam whether to trim it
            val exam = Bitmap.createBitmap(sample)
            val canvasExam = Canvas(exam)
            val paintExam = Paint().apply {
                style = Paint.Style.FILL
                isAntiAlias = true
                color = Color.BLACK
                xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC)
            }
            val pathExam = Path().apply { fillType = Path.FillType.INVERSE_EVEN_ODD }
            pathExam.addCircle(radiusFloat, radiusFloat, radiusFloat, Path.Direction.CW)
            canvasExam.drawPath(pathExam, paintExam)
            val pixels = IntArray(exam.width * exam.height)
            exam.getPixels(pixels, 0, exam.width, 0, 0, exam.width, exam.height)
            val transparentPixels = pixels.filter {
                (it ushr 24 and 0xFF) < alphaLimit
            }
            val transparentSize = transparentPixels.size.toFloat()
            val total = pixels.size.toFloat()
            val ratio = transparentSize / total
            iconDetails.shouldClip = ratio < 0.015

            if (iconDetails.shouldClip) {
                paintExam.shader = BitmapShader(sample, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
                pathExam.fillType = Path.FillType.EVEN_ODD
                val roundSample = Bitmap.createBitmap(diameter, diameter, Bitmap.Config.ARGB_8888)
                Canvas(roundSample).drawPath(pathExam, paintExam)
                iconDetails.shouldStroke = GraphicsUtil.shouldStroke(context, roundSample, GraphicsUtil.AI_FLAVOR_ROUND)
            }
        }

        if (!isDefined && !doPackageIcon && !iconDetails.shouldClip) iconDetails.shouldStroke = true
        val strokeWidth = if (iconDetails.shouldStroke) X.size(context, 1f, X.DP).roundToInt() else 0
        val strokeWidthFloat = if (iconDetails.shouldStroke) strokeWidth.toFloat() else 0f

        // below: get tools ready to draw
        val fraction = 0.6f
        // standard icon size: 48dp * 48dp
        val radiusStandard = X.size(context, 36 / fraction, X.DP).roundToInt()
        // the radius visible to be seen
        val displayRadius = if (iconDetails.shouldStroke) radiusStandard + strokeWidth else radiusStandard
        val displayRadiusFloat = displayRadius.toFloat()
        val displayDiameter = 2 * displayRadius
        // the true radius of image
        val targetRadius = displayRadius + strokeWidth
        val targetRadiusFloat = targetRadius.toFloat()
        val targetDiameter = 2 * targetRadius

        val logoCircular = Bitmap.createBitmap(targetDiameter, targetDiameter, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(logoCircular!!)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val colorStroke = if (iconDetails.shouldStroke) ThemeUtil.getColor(context, R.attr.colorStroke) else 0

        // below: process round icon from package
        if (doPackageIcon) {
            var logo2Draw = X.iconDrawable2Bitmap(context, roundIconDrawable!!)
            // below: ensure it is round
            logo2Draw = X.toTarget(logo2Draw, displayDiameter, displayDiameter)
            paint.shader = BitmapShader(logo2Draw, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            canvas.drawCircle(targetRadiusFloat, targetRadiusFloat, displayRadiusFloat, paint)
            if (!iconDetails.shouldStroke) return logoCircular
        }

        if (iconDetails.shouldClip){
            paint.shader = BitmapShader(X.toTarget(icon2Clip, displayDiameter, displayDiameter), Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            canvas.drawCircle(targetRadiusFloat, targetRadiusFloat, displayRadiusFloat, paint)
            if (!iconDetails.shouldStroke) return logoCircular
        }

        // below: draw icon on top of a white background
        if (!doPackageIcon && !iconDetails.shouldClip){
            // below: draw background
            paint.color = ThemeUtil.getColor(context, R.attr.colorApiLegacyBack)
            canvas.drawCircle(targetRadiusFloat, targetRadiusFloat, displayRadiusFloat, paint)

            // below: draw shrunk app icon
            val shrunkDiameter = (displayDiameter * fraction).toInt()
            val shrunkLogo = X.toTarget(logo, shrunkDiameter, shrunkDiameter)
            val offset = targetRadiusFloat * (1 - fraction)
            canvas.drawBitmap(shrunkLogo, offset, offset, Paint(Paint.ANTI_ALIAS_FLAG))
        }

        // stroke
        val paintStroke = Paint(Paint.ANTI_ALIAS_FLAG)
        paintStroke.color = colorStroke
        paintStroke.strokeWidth = strokeWidthFloat
        paintStroke.style = Paint.Style.STROKE
        canvas.drawCircle(targetRadiusFloat, targetRadiusFloat, displayRadiusFloat, paintStroke)
        return logoCircular
    }

    private fun ApkUtil.getNativeLibSupport(appPackage: AppPackage): BooleanArray {
        return if (appPackage.hasSplits) {
            val re = BooleanArray(NATIVE_LIB_SUPPORT_SIZE) { false }
            appPackage.apkPaths.forEach {
                val apkRe = getNativeLibSupport(it)
                for (i in 0 until NATIVE_LIB_SUPPORT_SIZE) {
                    if (re[i]) continue
                    re[i] = apkRe[i]
                }
            }
            re
        } else {
            getNativeLibSupport(appPackage.basePath)
        }
    }

    open fun retrieveNativeLibraries() {
        ApkUtil.getNativeLibSupport(appPackage).copyInto(nativeLibraries)
        isNativeLibrariesRetrieved = true
    }

    fun storePage(name: String, direct: Boolean = true): Intent = when (name) {
        packageCoolApk -> coolApkPage(direct)
        packagePlayStore -> playStorePage(direct)
        pageSettings -> settingsPage()
        pageApk -> apkPage()
        else -> throw IllegalArgumentException("no such page")
    }

    fun playStorePage(direct: Boolean): Intent{
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        if (direct) intent.component = ComponentName(packagePlayStore, "com.google.android.finsky.activities.MainActivity")
        return intent
    }

    fun coolApkPage(direct: Boolean): Intent{
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.coolapk.com/apk/$packageName"))
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        if (direct) intent.component = ComponentName(packageCoolApk, "com.coolapk.market.view.AppLinkActivity")
        return intent
    }

    fun settingsPage(): Intent{
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.data = Uri.fromParts("package", packageName, null)
        return intent
    }

    fun apkPage(): Intent{
        val file = File(appPackage.basePath)
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(Uri.parse(file.parent), "resource/folder")
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        return intent
    }

    fun clearIcons(){
        preload = true
        icon = null
    }

    private constructor(parIn: Parcel): this(parIn.readString() ?: "") {
        readParcel(parIn)
    }

    fun readParcel(parIn: Parcel) = parIn.run {
        packageName = readString() ?: ""
        val cl = this@ApiViewingApp.javaClass.classLoader
        icon = readParcelable(cl)
        name = readString() ?: ""
        verName = readString() ?: ""
        verCode = readLong()
        targetAPI = readInt()
        minAPI = readInt()
        targetSDK = readString() ?: ""
        minSDK = readString() ?: ""
        targetSDKDisplay = readString() ?: ""
        minSDKDisplay = readString() ?: ""
        targetSDKDouble = readDouble()
        minSDKDouble = readDouble()
        targetSDKLetter = readInt().toChar()
        minSDKLetter = readInt().toChar()
        apiUnit = readInt()
        appPackage = readParcelable(cl) ?: AppPackage("")
        updateTime = readLong()
        type = readInt()
        adaptiveIcon = readInt() == 1
        preload = readInt() == 1
        isLoadingIcon = readInt() == 1
        isNativeLibrariesRetrieved = readInt() == 1
        isLaunchable = readInt() == 1
        iconRetrievingDetails = readParcelable(cl)
        nativeLibraries = BooleanArray(ApkUtil.NATIVE_LIB_SUPPORT_SIZE) { false }
        for (i in nativeLibraries.indices) nativeLibraries[i] = readInt() == 1
    }

    override fun writeToParcel(dest: Parcel, flags: Int) = dest.run {
        writeString(packageName)
        writeParcelable(icon, flags)
        writeString(name)
        writeString(verName)
        writeLong(verCode)
        writeInt(targetAPI)
        writeInt(minAPI)
        writeString(targetSDK)
        writeString(minSDK)
        writeString(targetSDKDisplay)
        writeString(minSDKDisplay)
        writeDouble(targetSDKDouble)
        writeDouble(minSDKDouble)
        writeInt(targetSDKLetter.toInt())
        writeInt(minSDKLetter.toInt())
        writeInt(apiUnit)
        writeParcelable(appPackage, flags)
        writeLong(updateTime)
        writeInt(type)
        writeInt(if (adaptiveIcon) 1 else 0)
        writeInt(if (preload) 1 else 0)
        writeInt(if (isLoadingIcon) 1 else 0)
        writeInt(if (isNativeLibrariesRetrieved) 1 else 0)
        writeInt(if (isLaunchable) 1 else 0)
        writeParcelable(iconRetrievingDetails, flags)
        nativeLibraries.forEach { writeInt(if (it) 1 else 0) }
    }

    override fun describeContents(): Int {
        return 0
    }
}
