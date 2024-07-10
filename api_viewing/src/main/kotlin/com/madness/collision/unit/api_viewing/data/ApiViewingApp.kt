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

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Parcel
import android.os.Parcelable
import androidx.core.content.pm.PackageInfoCompat
import androidx.room.*
import com.madness.collision.misc.MiscApp
import com.madness.collision.unit.api_viewing.Utils
import com.madness.collision.unit.api_viewing.info.AppType
import com.madness.collision.unit.api_viewing.info.PkgInfo
import com.madness.collision.unit.api_viewing.info.getAppType
import com.madness.collision.unit.api_viewing.info.isOnBackInvokedCallbackEnabled
import com.madness.collision.unit.api_viewing.util.ApkUtil
import com.madness.collision.unit.api_viewing.util.ManifestUtil
import com.madness.collision.util.os.OsUtils
import kotlinx.parcelize.Parcelize

@Parcelize
data class ApiViewingIconInfo(
    @Embedded(prefix = "icS_")  // SystemICon from API (modified by system, may be from an icon pack)
    val system: ApiViewingIconDetails,
    @Embedded(prefix = "icN_")  // NormalICon from APK (unmodified original app icon)
    val normal: ApiViewingIconDetails,
    @Embedded(prefix = "icR_")  // RoundICon from APK (unmodified original app icon)
    val round: ApiViewingIconDetails,
): Parcelable

@Parcelize
class ApiViewingIconDetails(val isDefined: Boolean, val isAdaptive: Boolean): Parcelable

@Entity(tableName = "app")
open class ApiViewingApp(@PrimaryKey @ColumnInfo var packageName: String) : Parcelable, Cloneable {

    companion object {
        const val packagePlayStore = "com.android.vending"
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

    var verName = ""
    var verCode = 0L
    @Ignore
    var compileAPI: Int = -1
    @Ignore
    var compileApiCodeName: String? = null
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
    @Embedded
    var iconInfo: ApiViewingIconInfo? = null

    @Ignore
    var uid: Int = -1
    @Ignore
    var name: String = ""
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
    val adaptiveIcon: Boolean
        get() = iconInfo?.run { listOf(system, normal, round).any { it.isDefined && it.isAdaptive } } == true
    @Ignore
    private var type: Int = TYPE_APP
    @Ignore
    var appType: AppType = AppType.Common
    @Ignore
    var moduleInfo: ModuleInfo? = null
    @Ignore
    var isCoreApp: Boolean? = null
    @Ignore
    var isBackCallbackEnabled: Boolean? = null
    @Ignore
    var category: Int? = null

    val isJetpackComposed: Boolean
        get() = jetpackComposed == 1
    val isThirdPartyPackagesRetrieved: Boolean
        get() = jetpackComposed == 1 || jetpackComposed == 0
    val isArchive: Boolean
        get() = apiUnit == ApiUnit.APK
    val isNotArchive: Boolean
        get() = !isArchive
    val hasIcon: Boolean get() = iconInfo != null

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

    fun initIgnored(context: Context, pkgInfo: PackageInfo? = getPackageInfo(context)) {
        initIgnored()
        pkgInfo ?: return
        initExtraIgnored(context, pkgInfo)
    }

    /**
     * Initialize ignored properties
     */
    fun initIgnored() {
        uid = -1
        compileAPI = -1
        compileApiCodeName = null
        name = ""
        initApiVer()
        type = TYPE_APP
        appType = AppType.Common
        moduleInfo = null
        isCoreApp = null
        isBackCallbackEnabled = null
        category = null
    }

    fun initExtraIgnored(context: Context, pkgInfo: PackageInfo) {
        val info = pkgInfo.applicationInfo
        uid = info.uid
        loadCompileSdk(info)
        loadName(context, info)
        loadFreshProperties(pkgInfo, context)
    }

    fun init(context: Context, info: PackageInfo, preloadProcess: Boolean, archive: Boolean) {
        // preloadProcess is always true
        if (preloadProcess) init(context, info, archive)
    }

    private fun init(context: Context, info: PackageInfo, archive: Boolean) {
        packageName = info.packageName
        type = TYPE_APP
        verName = info.versionName ?: ""
        verCode = PackageInfoCompat.getLongVersionCode(info)
        updateTime = info.lastUpdateTime
        uid = info.applicationInfo.uid
        if (archive) {
            apiUnit = ApiUnit.APK
            name = packageName
        } else {
            val isNotSysApp = (info.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0
            apiUnit = if (isNotSysApp) ApiUnit.USER else ApiUnit.SYS
            appPackage = AppPackage(info.applicationInfo)
            loadCompileSdk(info.applicationInfo)
            loadName(context, info.applicationInfo)
            loadFreshProperties(info, context)
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
    }

    private fun getMinApiLevelFromArchive(): Int {
        val minApiText = ManifestUtil.getMinSdk(appPackage.basePath)
        return if (minApiText.isNotEmpty()) minApiText.toInt() else -1 // fix cloneable
    }

    private fun loadCompileSdk(applicationInfo: ApplicationInfo) {
        if (OsUtils.satisfy(OsUtils.S)) {
            compileAPI = applicationInfo.compileSdkVersion
            compileApiCodeName = applicationInfo.compileSdkVersionCodename
        } else {
            compileAPI = getCompileSdkFromArchive()
        }
    }

    private fun getCompileSdkFromArchive(): Int {
        val apiText = ManifestUtil.getCompileSdk(appPackage.basePath)
        return if (apiText.isNotEmpty()) apiText.toInt() else -1 // fix cloneable
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

    private fun loadFreshProperties(pkgInfo: PackageInfo, context: Context) {
        appType = getAppType(pkgInfo)
        // todo enhance module and isOnBackInvokedCallbackEnabled checking performance
        moduleInfo = PkgInfo.getModuleInfo(packageName, context)
        isCoreApp = PkgInfo.getIsCoreApp(pkgInfo)
        isBackCallbackEnabled = isOnBackInvokedCallbackEnabled(pkgInfo.applicationInfo, context)
        category = if (OsUtils.satisfy(OsUtils.O)) pkgInfo.applicationInfo.category else null
    }

    fun initArchive(context: Context, pkgInfo: PackageInfo): ApiViewingApp {
        val applicationInfo = pkgInfo.applicationInfo
        appPackage = AppPackage(applicationInfo)
        loadCompileSdk(applicationInfo)

        // API<N and is archive
        if (OsUtils.dissatisfy(OsUtils.N)) {
            minAPI = getMinApiLevelFromArchive()
            initApiVer()
        }

        loadName(context, applicationInfo)
        loadFreshProperties(pkgInfo, context)
        //name = manager.getApplicationLabel(pi.applicationInfo).toString();
        return this
        //this.name = file.getName() + "\n" + file.getParent();
    }

    private fun loadName(context: Context, applicationInfo: ApplicationInfo) {
        name = AppInfoProcessor.loadName(context, applicationInfo, false)
    }

    fun getApplicationInfo(context: Context): ApplicationInfo? {
        return if (this.isArchive) MiscApp.getApplicationInfo(context, apkPath = appPackage.basePath)
        else MiscApp.getApplicationInfo(context, packageName = packageName)
    }

    fun getPackageInfo(context: Context): PackageInfo? {
        return if (this.isArchive) MiscApp.getPackageArchiveInfo(context, path = appPackage.basePath)
        else MiscApp.getPackageInfo(context, packageName = packageName)
    }

    fun retrieveAppIconInfo(iconSet: List<Drawable?>) {
        retrieveConsuming(2, iconSet)
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

    open fun retrieveConsuming(target: Int, arg: Any? = null) {
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
            2 -> {
                synchronized(this) {
                    if (iconInfo != null) return
                    val iconSet = (arg as List<*>).filterIsInstance<Drawable?>()
                    val list = (0..2).map { iconSet.getOrNull(it) }.map { ic ->
                        val isDefined = ic != null
                        val isAdaptive = isDefined && OsUtils.satisfy(OsUtils.O) && ic is AdaptiveIconDrawable
                        ApiViewingIconDetails(isDefined, isAdaptive)
                    }
                    iconInfo = ApiViewingIconInfo(system = list[0], normal = list[1], round = list[2])
                }
            }
        }
    }

    private fun checkKotlin() {  // todo improve Kotlin detection
        appPackage.apkPaths.any { ApkUtil.checkPkg(it, "kotlin") }
    }

    private constructor(parIn: Parcel): this(parIn.readString() ?: "") {
        readParcel(parIn)
    }

    fun readParcel(parIn: Parcel) = parIn.run {
        packageName = readString() ?: ""
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
        isNativeLibrariesRetrieved = readInt() == 1
        isLaunchable = readInt() == 1
        nativeLibraries = BooleanArray(ApkUtil.NATIVE_LIB_SUPPORT_SIZE) { false }
        for (i in nativeLibraries.indices) nativeLibraries[i] = readInt() == 1
        jetpackComposed = readInt()
        iconInfo = readParcelable(ApiViewingIconInfo::class.java.classLoader)
    }

    override fun writeToParcel(dest: Parcel, flags: Int) = dest.run {
        writeString(packageName)
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
        writeInt(if (isNativeLibrariesRetrieved) 1 else 0)
        writeInt(if (isLaunchable) 1 else 0)
        nativeLibraries.forEach { writeInt(if (it) 1 else 0) }
        writeInt(jetpackComposed)
        writeParcelable(iconInfo, flags)
    }

    override fun describeContents(): Int {
        return 0
    }
}
