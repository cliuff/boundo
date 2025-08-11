package com.madness.collision.wearable.av.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Parcel
import android.os.Parcelable
import com.madness.collision.wearable.R
import com.madness.collision.wearable.misc.MiscApp
import com.madness.collision.wearable.util.GraphicsUtil
import com.madness.collision.wearable.util.X
import kotlin.math.max
import kotlin.math.roundToInt

internal class ApiViewingApp(val packageName: String) : Parcelable {

    companion object {
        private const val TYPE_ICON = 2
        private const val TYPE_APP = 1
        //private static int apiCeiling = Build.VERSION_CODES.P;

        @JvmField
        val CREATOR: Parcelable.Creator<ApiViewingApp> = object : Parcelable.Creator<ApiViewingApp> {
            override fun createFromParcel(parIn: Parcel) = ApiViewingApp(parIn)
            override fun newArray(size: Int) = Array<ApiViewingApp?>(size){ null }
        }
    }

    var logo: Bitmap? = null
    var name: String = ""
    var verName = ""
    var targetAPI: Int = -1
    var targetSDK: String = ""
    var targetSDKDisplay: String = ""
    var targetSDKDouble: Double = -1.0
    var adaptiveIcon: Boolean = false
    var preload: Boolean = false
    var apiUnit: Int = ApiUnit.NON
    var updateTime = 0L
    private var type: Int = TYPE_APP

    constructor(): this(""){
        type = TYPE_ICON
    }

    constructor(context: Context,  info: PackageInfo, preloadProcess: Boolean): this(info.packageName ?: ""){
        if (preloadProcess) {
            type = TYPE_APP
            verName = info.versionName ?: ""
            updateTime = info.lastUpdateTime
            preload = true
            info.applicationInfo?.let { applicationInfo ->
                apiUnit = if ((applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0) ApiUnit.USER else ApiUnit.SYS
                loadName(context, applicationInfo)
                //name = manager.getApplicationLabel(pi.applicationInfo).toString();
                this.targetAPI = applicationInfo.targetSdkVersion
            }
            targetSDK = X.getAndroidVersionByAPI(targetAPI, false)
            if (targetSDK.isNotEmpty()){
                targetSDKDouble = targetSDK.toDouble()
                targetSDKDisplay = targetSDK
            }
        }else {
            info.applicationInfo?.let { applicationInfo ->
                load(context, applicationInfo)
            }
        }
    }

    private fun loadName(context: Context, applicationInfo: ApplicationInfo){
        name = context.packageManager.getApplicationLabel(applicationInfo).toString()
    }

    /**
     * for quick load of both app and apk
     */
    fun load(context: Context){
        preload = false
        val applicationInfo: ApplicationInfo? = MiscApp.getApplicationInfo(context, packageName = packageName)
        applicationInfo ?: return
        load(context, applicationInfo)
    }

    fun load(context: Context, applicationInfo: ApplicationInfo): ApiViewingApp {
        preload = false
        return when (type) {
            TYPE_APP -> load(context){
                context.packageManager.getApplicationIcon(applicationInfo).mutate()
            }
            TYPE_ICON -> throw IllegalArgumentException("instance of TYPE_ICON must provide icon retrievers")
            else -> this
        }
    }

    fun load(context: Context, retrieverLogo: () -> Drawable): ApiViewingApp {
        preload = false
        loadLogo(context, retrieverLogo)
        return this
    }

    private fun loadLogo(context: Context, retrieverLogo: () -> Drawable){
        var logoDrawable = retrieverLogo()
        var width = logoDrawable.intrinsicWidth
        var height = logoDrawable.intrinsicHeight
        if (width <= 0 || height <= 0){
            logoDrawable = context.getDrawable(R.drawable.res_android_robot_head) ?: return
            width = logoDrawable.intrinsicWidth
            height = logoDrawable.intrinsicHeight
        }
        adaptiveIcon = X.aboveOn(X.O) && logoDrawable is AdaptiveIconDrawable
        // below: shrink size if it's too large in case of consuming too much memory
        val standardLength = X.size(context, 40f, X.DP).roundToInt()
        val maxLength = max(width, height)
        if (maxLength > standardLength){
            val fraction: Float = standardLength.toFloat() / maxLength
            width = (width * fraction).roundToInt()
            height = (height * fraction).roundToInt()
        }
        // above: shrink size if it's too large in case of consuming too much memory
        var logo = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        logoDrawable.setBounds(0, 0, width, height)
        logoDrawable.draw(Canvas(logo))
        // make it square and properly centered
        logo = GraphicsUtil.properly2Square(logo)
        this.logo = X.toTarget(logo, standardLength, standardLength)
    }

    fun clearIcons(){
        preload = true
        logo = null
    }

    private constructor(parIn: Parcel): this(parIn.readString() ?: "") {
        logo = parIn.readParcelable(Bitmap::class.java.classLoader)!!
        name = parIn.readString() ?: ""
        targetAPI = parIn.readInt()
        targetSDK = parIn.readString() ?: ""
        targetSDKDisplay = parIn.readString() ?: ""
        targetSDKDouble = parIn.readDouble()
        apiUnit = parIn.readInt()
        updateTime = parIn.readLong()
        type = parIn.readInt()
        val booleanArray = BooleanArray(2)
        parIn.readBooleanArray(booleanArray)
        adaptiveIcon = booleanArray[0]
        preload = booleanArray[1]
    }

    override fun writeToParcel( dest: Parcel, flags: Int) {
        dest.writeString(packageName)
        dest.writeParcelable(logo, flags)
        dest.writeString(name)
        dest.writeInt(targetAPI)
        dest.writeString(targetSDK)
        dest.writeString(targetSDKDisplay)
        dest.writeDouble(targetSDKDouble)
        dest.writeInt(apiUnit)
        dest.writeLong(updateTime)
        dest.writeInt(type)
        dest.writeBooleanArray(booleanArrayOf(adaptiveIcon, preload))
    }

    override fun describeContents(): Int {
        return 0
    }
}
