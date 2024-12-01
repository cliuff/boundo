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
import androidx.core.content.pm.PackageInfoCompat
import androidx.room.Ignore
import com.madness.collision.misc.MiscApp
import com.madness.collision.unit.api_viewing.database.ApiViewingIconDetails
import com.madness.collision.unit.api_viewing.database.ApiViewingIconInfo
import com.madness.collision.unit.api_viewing.info.AppType
import com.madness.collision.unit.api_viewing.info.PkgInfo
import com.madness.collision.unit.api_viewing.info.getAppType
import com.madness.collision.unit.api_viewing.info.isOnBackInvokedCallbackEnabled
import com.madness.collision.unit.api_viewing.util.ApkUtil
import com.madness.collision.unit.api_viewing.util.ManifestUtil
import com.madness.collision.util.os.OsUtils
import java.lang.ref.WeakReference

open class ApiViewingApp(var packageName: String) : Cloneable {

    companion object {
        const val packagePlayStore = "com.android.vending"
        const val packagePackageInstaller = "com.google.android.packageinstaller"
    }

    var verName: String = ""
    var verCode: Long = 0L
    @Ignore var compileAPI: Int = -1
    @Ignore var compileApiCodeName: String? = null
    var targetAPI: Int = -1
    var minAPI: Int = -1
    var apiUnit: Int = ApiUnit.NON
    var updateTime: Long = 0L
    var isNativeLibrariesRetrieved: Boolean = false
    var nativeLibraries: BooleanArray = BooleanArray(ApkUtil.NATIVE_LIB_SUPPORT_SIZE) { false }
    var isLaunchable: Boolean = false
    var appPackage: AppPackage = AppPackage("")
    var jetpackComposed: Int = -1
    var iconInfo: ApiViewingIconInfo? = null

    @Ignore var uid: Int = -1
    @Ignore var name: String = ""

    val adaptiveIcon: Boolean
        get() = iconInfo?.run { listOf(system, normal, round).any { it.isDefined && it.isAdaptive } } == true
    @Ignore var appType: AppType = AppType.Common
    @Ignore var moduleInfo: ModuleInfo? = null
    @Ignore var isCoreApp: Boolean? = null
    @Ignore var isBackCallbackEnabled: Boolean? = null
    @Ignore var category: Int? = null

    @Ignore private var pkgInstallerRef: String? = null
    // store a weak reference to enable temporary data caching
    @Ignore private var pkgInfoRef: WeakReference<PackageInfo> = WeakReference(null)
    // services of 500 apps only consume a couple MB of memory, store them to avoid expensive re-computation
    @Ignore private var serviceFamilyClassesRef: Set<String>? = null
    @Ignore private var miPushSdkCheckRef: Int = -1

    val isPkgInstallerLoaded: Boolean
        get() = pkgInstallerRef != null
    val isPkgInstallerNull: Boolean
        get() = pkgInstallerRef == "@null"
    var pkgInstaller: String?
        get() = pkgInstallerRef?.takeUnless { it == "@null" }
        set(value) { pkgInstallerRef = value ?: "@null" }
    var pkgInfo: PackageInfo?
        get() = pkgInfoRef.get()
        set(value) { pkgInfoRef = WeakReference(value) }
    var serviceFamilyClasses: Set<String>?
        by ::serviceFamilyClassesRef
    val isMiPushSdkChecked: Boolean
        get() = miPushSdkCheckRef != -1
    var isMiPushEnabled: Boolean?
        get() = when (miPushSdkCheckRef) { 1 -> true; 0 -> false; else -> null }
        set(value) { miPushSdkCheckRef = value?.compareTo(false) ?: -2 }

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
        uid = -1
        compileAPI = -1
        compileApiCodeName = null
        name = ""
        appType = AppType.Common
        moduleInfo = null
        isCoreApp = null
        isBackCallbackEnabled = null
        category = null

        pkgInfo?.let { loadFreshProperties(it, context) }
    }

    fun init(context: Context, info: PackageInfo, preloadProcess: Boolean, archive: Boolean) {
        // preloadProcess is always true
        if (!preloadProcess) return
        packageName = info.packageName
        verName = info.versionName ?: ""
        verCode = PackageInfoCompat.getLongVersionCode(info)
        updateTime = info.lastUpdateTime
        apiUnit = when {
            archive -> ApiUnit.APK
            (info.applicationInfo?.flags ?: 0) and ApplicationInfo.FLAG_SYSTEM != 0 -> ApiUnit.SYS
            else -> ApiUnit.USER
        }
        info.applicationInfo?.let { appInfo ->
            appPackage = AppPackage(appInfo)
            targetAPI = appInfo.targetSdkVersion
            minAPI = when {
                OsUtils.satisfy(OsUtils.N) -> appInfo.minSdkVersion
                else -> ManifestUtil.getMinSdk(appPackage.basePath).toIntOrNull() ?: -1 // fix cloneable
            }
        }
        isLaunchable = context.packageManager.getLaunchIntentForPackage(packageName) != null

        loadFreshProperties(info, context)
    }

    private fun loadFreshProperties(pkgInfo: PackageInfo, context: Context) {
        val applicationInfo = pkgInfo.applicationInfo
        if (applicationInfo != null) {
            uid = applicationInfo.uid
            name = AppInfoProcessor.loadName(context, applicationInfo, false)
            if (OsUtils.satisfy(OsUtils.S)) {
                compileAPI = applicationInfo.compileSdkVersion
                compileApiCodeName = applicationInfo.compileSdkVersionCodename
            } else {
                compileAPI = ManifestUtil.getCompileSdk(appPackage.basePath).toIntOrNull() ?: -1 // fix cloneable
            }
        }
        appType = getAppType(pkgInfo)
        // todo enhance module and isOnBackInvokedCallbackEnabled checking performance
        moduleInfo = PkgInfo.getModuleInfo(packageName, context)
        isCoreApp = PkgInfo.getIsCoreApp(pkgInfo)
        if (applicationInfo != null) {
            isBackCallbackEnabled = isOnBackInvokedCallbackEnabled(applicationInfo, context)
            category = if (OsUtils.satisfy(OsUtils.O)) applicationInfo.category else null
        }
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
            0 -> loadNativeLibraries()
            1 -> loadThirdPartyPackages()
            2 -> loadAppIconInfo((arg as List<*>).filterIsInstance<Drawable?>())
        }
    }

    private fun checkKotlin() {  // todo improve Kotlin detection
        appPackage.apkPaths.any { ApkUtil.checkPkg(it, "kotlin") }
    }
}


private fun ApiViewingApp.loadNativeLibraries() {
    fun AppPackage.getNativeLibSupport(): BooleanArray {
        if (!hasSplits) return ApkUtil.getNativeLibSupport(basePath)
        var arr = BooleanArray(0)
        for (path in apkPaths) {
            val lib = ApkUtil.getNativeLibSupport(path)
            if (arr.size != lib.size) { arr = lib; continue }
            for (i in arr.indices) if (!arr[i]) arr[i] = lib[i]
        }
        return arr
    }
    synchronized(nativeLibraries) {
        if (isNativeLibrariesRetrieved) return
        appPackage.getNativeLibSupport().copyInto(nativeLibraries)
        isNativeLibrariesRetrieved = true
    }
}

private fun ApiViewingApp.loadThirdPartyPackages() {
    // ensure thread safety, otherwise encounter exceptions during app list loading
    synchronized(jetpackComposed) {
        if (isThirdPartyPackagesRetrieved) return
        val checkResult = appPackage.apkPaths.any {
            ApkUtil.checkPkg(it, "androidx.compose")
        }
        jetpackComposed = if (checkResult) 1 else 0
    }
}

private fun ApiViewingApp.loadAppIconInfo(iconSet: List<Drawable?>) {
    synchronized(this) {
        if (iconInfo != null) return
        val list = (0..2).map { iconSet.getOrNull(it) }.map { ic ->
            val isDefined = ic != null
            val isAdaptive = isDefined && OsUtils.satisfy(OsUtils.O) && ic is AdaptiveIconDrawable
            ApiViewingIconDetails(isDefined, isAdaptive)
        }
        iconInfo = ApiViewingIconInfo(system = list[0], normal = list[1], round = list[2])
    }
}
