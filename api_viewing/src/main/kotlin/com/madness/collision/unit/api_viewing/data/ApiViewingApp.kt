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
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import android.provider.Settings
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.madness.collision.R
import com.madness.collision.misc.MiscApp
import com.madness.collision.settings.SettingsFunc
import com.madness.collision.unit.api_viewing.Utils
import com.madness.collision.util.*
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

@Entity(tableName = "apps")
internal class ApiViewingApp(
        @PrimaryKey @ColumnInfo(name = "package") val packageName: String
) : Parcelable {

    companion object {
        const val packageCoolApk = "com.coolapk.market"
        const val packagePlayStore = "com.android.vending"
        const val pageSettings = "settings"
        const val pageApk = "apk"

        private const val TYPE_ICON = 2
        private const val TYPE_APP = 1
        //private static int apiCeiling = Build.VERSION_CODES.P;

        @JvmField
        val CREATOR: Parcelable.Creator<ApiViewingApp> = object : Parcelable.Creator<ApiViewingApp> {
            override fun createFromParcel(parIn: Parcel) = ApiViewingApp(parIn)
            override fun newArray(size: Int) = Array<ApiViewingApp?>(size){ null }
        }
    }

    @Ignore
    var icon: Bitmap? = null
    var name: String = ""
    var verName = ""
    var targetAPI: Int = -1
    var minAPI: Int = -1
    var targetSDK: String = ""
    var minSDK: String = ""
    var targetSDKDisplay: String = ""
    var minSDKDisplay: String = ""
    var targetSDKDouble: Double = -1.0
    var minSDKDouble: Double = -1.0
    var targetSDKLetter: Char = '?'
    var minSDKLetter: Char = '?'
    var adaptiveIcon: Boolean = false
    var preload: Boolean = false
    var apiUnit: Int = ApiUnit.NON
    var apkPath: String = ""
    var updateTime = 0L
    @Ignore
    private var type: Int = TYPE_APP
    @Ignore
    private var mOnLoadedListener: (() -> Unit)? = null

    @Ignore
    private var _iconDetails: IconRetrievingDetails? = null
    private val isIconDefined: Boolean
        get() = _iconDetails != null
    private val iconDetails: IconRetrievingDetails
        get() = _iconDetails!!

    @Ignore
    var isLoadingIcon: Boolean = false
        private set
    val hasIcon: Boolean
        get() = icon != null

    constructor(): this(""){
        type = TYPE_ICON
    }

    constructor(context: Context,  info: PackageInfo, preloadProcess: Boolean, archive: Boolean): this(info.packageName ?: ""){
        if (preloadProcess) {
            type = TYPE_APP
            verName = info.versionName ?: ""
            updateTime = info.lastUpdateTime
            preload = true
            if (archive){
                apiUnit = ApiUnit.APK
                name = packageName
            }else {
                apiUnit = if ((info.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0) ApiUnit.USER else ApiUnit.SYS
                apkPath = info.applicationInfo.sourceDir
                loadName(context, info.applicationInfo)
                //name = manager.getApplicationLabel(pi.applicationInfo).toString();
            }
            this.targetAPI = info.applicationInfo.targetSdkVersion
            targetSDK = Utils.getAndroidVersionByAPI(targetAPI, false)
            if (targetSDK.isNotEmpty()){
                targetSDKDouble = targetSDK.toDouble()
                targetSDKDisplay = targetSDK
            }
            targetSDKLetter = Utils.getAndroidLetterByAPI(targetAPI)

            if (X.aboveOn(Build.VERSION_CODES.N)) {
                minAPI = info.applicationInfo.minSdkVersion
            }else {
                val minApiText = ManifestUtil.getMinSdk(apkPath)
                if (minApiText.isNotEmpty()) minAPI = minApiText.toInt()
            }
            minSDK = Utils.getAndroidVersionByAPI(minAPI, false)
            if (minSDK.isNotEmpty()){
                minSDKDouble = minSDK.toDouble()
                minSDKDisplay = minSDK
            }
            minSDKLetter = Utils.getAndroidLetterByAPI(minAPI)
        }else {
            load(context, info.applicationInfo)
        }
    }

    fun initArchive(context: Context, path: String, applicationInfo: ApplicationInfo): ApiViewingApp {
        apkPath = path
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
                name = if (labelRes == 0) "" else { try { localeContext.getString(labelRes) } catch (e: Exception) { "" } }
            }
        } catch ( e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        } finally {
            if (name.isEmpty()) loadName(context, applicationInfo, false)
        }
    }

    fun getApplicationInfo(context: Context): ApplicationInfo? {
        return if (this.isArchive()) MiscApp.getApplicationInfo(context, apkPath = apkPath)
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
                }, { ManifestUtil.getRoundIcon(context, applicationInfo, apkPath) })
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
        dispatchListener()
        return this
    }

    /**
     * Call this before any load method
     */
    fun setOnLoadedListener(block: () -> Unit): ApiViewingApp {
        mOnLoadedListener = block
        return this
    }

    private fun dispatchListener() {
        mOnLoadedListener?.invoke()
        mOnLoadedListener = null
    }

    private fun loadLogo(context: Context, retrieverLogo: () -> Drawable,  retrieverRound: () -> Drawable?) {
        val isDefined = isIconDefined
        if (!isDefined) {
            _iconDetails = IconRetrievingDetails()
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

    private fun retrieveAppIconInfo(iconDrawable: Drawable) {
        adaptiveIcon = X.aboveOn(X.O) && iconDrawable is AdaptiveIconDrawable
    }

    private fun retrieveAppIcon(context: Context, isDefined: Boolean, iconDrawable: Drawable): Bitmap? {
        var logoDrawable= iconDrawable
        if (isDefined) {
            if (iconDetails.isDefault) {
                logoDrawable = context.getDrawable(R.drawable.res_android_robot_head) ?: return null
            }
        } else {
            iconDetails.width = logoDrawable.intrinsicWidth
            iconDetails.height = logoDrawable.intrinsicHeight
            if (iconDetails.width <= 0 || iconDetails.height <= 0){
                iconDetails.isDefault = true
                logoDrawable = context.getDrawable(R.drawable.res_android_robot_head) ?: return null
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
        if (X.aboveOn(X.O) && adaptiveIcon) {
            return GraphicsUtil.drawAIRound(context, logoDrawable) to null
        }
        // below: retrieve round icon from package
        var roundIcon: Drawable? = null
        if (EasyAccess.shouldManifestedRound) {
            roundIcon = retrieverRound()
            if (X.aboveOn(X.O) && roundIcon is AdaptiveIconDrawable) {
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
        val alphaLimit = 0xA8
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
            iconDetails.shouldClip = ratio < 0.1

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
        val file = File(apkPath)
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(Uri.parse(file.parent), "resource/folder")
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        return intent
    }

    fun clearIcons(){
        preload = true
        icon = null
    }

    fun isArchive() = apiUnit == ApiUnit.APK
    
    fun isNotArchive() = !isArchive()

    private constructor(parIn: Parcel): this(parIn.readString() ?: "") {
        icon = parIn.readParcelable(Bitmap::class.java.classLoader)!!
        name = parIn.readString() ?: ""
        targetAPI = parIn.readInt()
        minAPI = parIn.readInt()
        targetSDK = parIn.readString() ?: ""
        minSDK = parIn.readString() ?: ""
        targetSDKDisplay = parIn.readString() ?: ""
        minSDKDisplay = parIn.readString() ?: ""
        targetSDKDouble = parIn.readDouble()
        minSDKDouble = parIn.readDouble()
        targetSDKLetter = parIn.readInt().toChar()
        minSDKLetter = parIn.readInt().toChar()
        apiUnit = parIn.readInt()
        apkPath = parIn.readString() ?: ""
        updateTime = parIn.readLong()
        type = parIn.readInt()
        val booleanArray = BooleanArray(2)
        parIn.readBooleanArray(booleanArray)
        adaptiveIcon = booleanArray[0]
        preload = booleanArray[1]
        isLoadingIcon = booleanArray[2]
        _iconDetails = parIn.readParcelable(IconRetrievingDetails::class.java.classLoader)
    }

    override fun writeToParcel( dest: Parcel, flags: Int) {
        dest.writeString(packageName)
        dest.writeParcelable(icon, flags)
        dest.writeString(name)
        dest.writeInt(targetAPI)
        dest.writeInt(minAPI)
        dest.writeString(targetSDK)
        dest.writeString(minSDK)
        dest.writeString(targetSDKDisplay)
        dest.writeString(minSDKDisplay)
        dest.writeDouble(targetSDKDouble)
        dest.writeDouble(minSDKDouble)
        dest.writeInt(targetSDKLetter.toInt())
        dest.writeInt(minSDKLetter.toInt())
        dest.writeInt(apiUnit)
        dest.writeString(apkPath)
        dest.writeLong(updateTime)
        dest.writeInt(type)
        dest.writeBooleanArray(booleanArrayOf(adaptiveIcon, preload, isLoadingIcon))
        dest.writeParcelable(_iconDetails, flags)
    }

    override fun describeContents(): Int {
        return 0
    }
}
