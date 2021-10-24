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
import android.graphics.Bitmap
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import android.provider.Settings
import androidx.core.content.pm.PackageInfoCompat
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.madness.collision.misc.MiscApp
import com.madness.collision.settings.LanguageMan
import com.madness.collision.unit.api_viewing.Utils
import com.madness.collision.unit.api_viewing.util.ApkUtil
import com.madness.collision.unit.api_viewing.util.ManifestUtil
import com.madness.collision.util.GraphicsUtil
import com.madness.collision.util.SystemUtil
import com.madness.collision.util.X
import com.madness.collision.util.os.OsUtils
import java.io.File

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

        @Suppress("unused")
        @JvmField
        val CREATOR: Parcelable.Creator<ApiViewingApp> = object : Parcelable.Creator<ApiViewingApp> {
            override fun createFromParcel(parIn: Parcel) = ApiViewingApp(parIn)
            override fun newArray(size: Int) = Array<ApiViewingApp?>(size) { null }
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
    @ColumnInfo(defaultValue = "-1")
    var jetpackComposed: Int = -1

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

    val isJetpackComposed: Boolean
        get() = jetpackComposed == 1
    val isThirdPartyPackagesRetrieved: Boolean
        get() = jetpackComposed == 1 || jetpackComposed == 0
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
            if (OsUtils.satisfy(OsUtils.N)) {
                minAPI = info.applicationInfo.minSdkVersion
                initApiVer()
            } else if (archive.not()) {
                minAPI = getMinApiLevelFromArchive()
                initApiVer()
            } // else (when API<N and is archive), init minAPI in initArchive()

            val pm = context.packageManager
            isLaunchable = pm.getLaunchIntentForPackage(packageName) != null
        } else {
            load(context, info.applicationInfo)
        }
    }

    private fun getMinApiLevelFromArchive(): Int {
        val minApiText = ManifestUtil.getMinSdk(appPackage.basePath)
        return if (minApiText.isNotEmpty()) minApiText.toInt() else -1 // fix cloneable
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

        // API<N and is archive
        if (OsUtils.dissatisfy(OsUtils.N)) {
            minAPI = getMinApiLevelFromArchive()
            initApiVer()
        }

        loadName(context, applicationInfo)
        //name = manager.getApplicationLabel(pi.applicationInfo).toString();
        return this
        //this.name = file.getName() + "\n" + file.getParent();
    }

    private fun loadName(context: Context, applicationInfo: ApplicationInfo) {
        loadName(context, applicationInfo, false)
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
        val langCode = LanguageMan(context).getLanguage()
        if (langCode == LanguageMan.AUTO || langCode == SystemUtil.getLocaleApp().toString()) {
            loadName(context, applicationInfo, false)
        } else {
            val label = AppInfoProcessor.loadLabel(context, packageName, langCode)
            if (label != null) name = label else loadName(context, applicationInfo, false)
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
        val originalIcon: Bitmap? by lazy {
            AppIconProcessor.retrieveAppIcon(context, isDefined, iconDrawable, iconDetails)
        }
        icon = if (EasyAccess.shouldRoundIcon) {
            val (roundIcon, roundIconDrawable) = adaptiveRoundIcon(context, iconDrawable, retrieverRound)
            roundIcon ?: originalIcon?.let {
                AppIconProcessor.roundIcon(context, isDefined, it, roundIconDrawable, iconDetails)
            }
        } else {
            originalIcon
        }
    }

    val isIconOriginal: Boolean
        get() = !EasyAccess.shouldRoundIcon

    fun retrieveAppIconInfo(iconDrawable: Drawable) {
        adaptiveIcon = OsUtils.satisfy(OsUtils.O) && iconDrawable is AdaptiveIconDrawable
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

    fun retrieveThirdPartyPackages() {
        if (isThirdPartyPackagesRetrieved) return
        retrieveConsuming(1)
    }

    fun retrieveNativeLibraries() {
        if (isNativeLibrariesRetrieved) return
        retrieveConsuming(0)
    }

    open fun retrieveConsuming(target: Int) {
        when (target) {
            0 -> {
                synchronized(nativeLibraries) {
                    if (isNativeLibrariesRetrieved) return
                    ApkUtil.getNativeLibSupport(appPackage).copyInto(nativeLibraries)
                    isNativeLibrariesRetrieved = true
                }
            }
            1 -> {
                // ensure thread safety, otherwise encounter exceptions during app list loading
                synchronized(jetpackComposed) {
                    if (isThirdPartyPackagesRetrieved) return
                    val checkResult = appPackage.apkPaths.any {
                        ApkUtil.checkPkg(it, "androidx.compose")
                    }
                    jetpackComposed = if (checkResult) 1 else 0
                }
            }
        }
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
        icon = readParcelable(Bitmap::class.java.classLoader)
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
        appPackage = readParcelable(AppPackage::class.java.classLoader) ?: AppPackage("")
        updateTime = readLong()
        type = readInt()
        adaptiveIcon = readInt() == 1
        preload = readInt() == 1
        isLoadingIcon = readInt() == 1
        isNativeLibrariesRetrieved = readInt() == 1
        isLaunchable = readInt() == 1
        iconRetrievingDetails = readParcelable(IconRetrievingDetails::class.java.classLoader)
        nativeLibraries = BooleanArray(ApkUtil.NATIVE_LIB_SUPPORT_SIZE) { false }
        for (i in nativeLibraries.indices) nativeLibraries[i] = readInt() == 1
        jetpackComposed = readInt()
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
        writeInt(targetSDKLetter.code)
        writeInt(minSDKLetter.code)
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
        writeInt(jetpackComposed)
    }

    override fun describeContents(): Int {
        return 0
    }
}
