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

package com.madness.collision.unit.api_viewing.tag.app

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.util.Log
import com.madness.collision.unit.api_viewing.R
import com.madness.collision.unit.api_viewing.data.ApiUnit
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.tag.inflater.AppTagInflater
import com.madness.collision.util.ElapsingTime
import com.madness.collision.util.os.OsUtils
import kotlinx.coroutines.delay

internal fun builtInTags(): Map<String, AppTagInfo> = listOf(
    AppTagInfo(
        id = AppTagInfo.ID_APP_INSTALLER_PLAY, category = 0.cat, icon = ApiViewingApp.packagePlayStore.appIcon,
        label = AppTagInfo.Labels(full = R.string.apiDetailsInstallGP.label, isDynamic = true), rank = 0,
        requisites = pkgInstallerRequisite().list,
        expressing = expressing { res -> res.pkgInstaller == ApiViewingApp.packagePlayStore }
    ).apply { iconKey = ApiViewingApp.packagePlayStore },
    AppTagInfo(
        id = AppTagInfo.ID_APP_INSTALLER, category = 0.cat, icon = AppTagInfo.Icon(isDynamic = true),
        label = AppTagInfo.Labels(full = R.string.apiDetailsInstallPI.label, isDynamic = true), rank = 1,
        requisites = pkgInstallerRequisite().list,
        expressing = expressing { res ->
            val installer = res.pkgInstaller ?: return@expressing false
            installer != ApiViewingApp.packagePlayStore
        }
    ),

    AppTagInfo(
        id = AppTagInfo.ID_TECH_KOTLIN, category = 0.cat, icon = R.drawable.ic_kotlin_72.icon,
        label = "Kotlin".labels, rank = 2,
        requisites = nativeLibrariesRequisite().list,
        expressing = commonExpressing { it.nativeLibraries[7] }
    ).apply { iconKey = "kot" },
    AppTagInfo(
        id = AppTagInfo.ID_TECH_X_COMPOSE, category = 0.cat, icon = R.drawable.ic_cmp_72.icon,
        label = "Jetpack Compose".labels, rank = 3,
        requisites = thirdPartyPkgRequisite().list,
        expressing = commonExpressing { it.isJetpackComposed }
    ).apply { iconKey = "xcm" },
    AppTagInfo(
        id = AppTagInfo.ID_TECH_FLUTTER, category = 0.cat, icon = R.drawable.ic_flutter_72.icon,
        label = "Flutter".labels, rank = 4,
        requisites = nativeLibrariesRequisite().list,
        expressing = commonExpressing { it.nativeLibraries[4] }
    ).apply { iconKey = "flu" },
    AppTagInfo(
        id = AppTagInfo.ID_TECH_REACT_NATIVE, category = 0.cat, icon = R.drawable.ic_react_72.icon,
        label = "React Native".labels, rank = 5,
        requisites = nativeLibrariesRequisite().list,
        expressing = commonExpressing { it.nativeLibraries[5] }
    ).apply { iconKey = "rn" },
    AppTagInfo(
        id = AppTagInfo.ID_TECH_XAMARIN, category = 0.cat, icon = R.drawable.ic_xamarin_72.icon,
        label = "Xamarin".labels, rank = 6,
        requisites = nativeLibrariesRequisite().list,
        expressing = commonExpressing { it.nativeLibraries[6] }
    ).apply { iconKey = "xam" },

    AppTagInfo(
        id = AppTagInfo.ID_APP_ADAPTIVE_ICON, category = 0.cat, icon = R.drawable.ic_ai_72.icon,
        label = R.string.av_ai.labels, rank = 7,
        requisites = appIconRequisite().list,
        expressing = commonExpressing { it.adaptiveIcon }
    ).apply { iconKey = "ai" },
    AppTagInfo(
        id = AppTagInfo.ID_PKG_AAB, category = 0.cat, icon = R.drawable.ic_aab_72.icon,
        label = R.string.av_tag_has_splits.labels, rank = 8,
        expressing = commonExpressing { it.appPackage.hasSplits }
    ).apply { iconKey = "aab" },
    AppTagInfo(
        id = AppTagInfo.ID_APP_SYSTEM, category = 0.cat, icon = R.drawable.ic_system_72.icon,
        label = (R.string.av_adapter_tag_system to R.string.av_settings_tags_system).resLabels, rank = 9,
        expressing = commonExpressing { it.apiUnit == ApiUnit.SYS }
    ).apply { iconKey = "sys" },
    AppTagInfo(
        id = AppTagInfo.ID_APP_HIDDEN, category = 0.cat, icon = R.drawable.ic_hidden_72.icon,
        label = (R.string.av_adapter_tag_hidden to R.string.av_settings_tag_hidden).resLabels, rank = 10,
        expressing = commonExpressing { !it.isLaunchable }
    ).apply { iconKey = "hid" },

    AppTagInfo(
        id = AppTagInfo.ID_PKG_64BIT, category = 0.cat, icon = R.string.av_tag_full_64bit.label.icon,
        label = (R.string.av_tag_full_64bit_normal to R.string.av_tag_full_64bit_full).resLabels, rank = 11,
        requisites = nativeLibrariesRequisite().list,
        expressing = commonExpressing { it.nativeLibraries.let { n -> (!n[0] || n[1]) && (!n[2] || n[3]) } }
    ).apply { iconKey = "64b" },

    AppTagInfo(
        id = AppTagInfo.ID_PKG_ARM32, category = 0.cat, icon = "ARM 32".icon,
        label = R.string.av_tag_arm_32bit.labels, rank = 12,
        requisites = nativeLibrariesRequisite().list,
        expressing = commonExpressing { it.nativeLibraries[0] }
    ),
    AppTagInfo(
        id = AppTagInfo.ID_PKG_ARM64, category = 0.cat, icon = "ARM 64".icon,
        label = R.string.av_tag_arm_64bit.labels, rank = 13,
        requisites = nativeLibrariesRequisite().list,
        expressing = commonExpressing { it.nativeLibraries[1] }
    ),
    AppTagInfo(
        id = AppTagInfo.ID_PKG_X86, category = 0.cat, icon = "x86".icon,
        label = "x86".labels, rank = 14,
        requisites = nativeLibrariesRequisite().list,
        expressing = commonExpressing { it.nativeLibraries[2] }
    ),
    AppTagInfo(
        id = AppTagInfo.ID_PKG_X64, category = 0.cat, icon = "x64".icon,
        label = ("x64" to "x86-64 (x64)").strLabels, rank = 15,
        requisites = nativeLibrariesRequisite().list,
        expressing = commonExpressing { it.nativeLibraries[3] }
    ),

    AppTagInfo(
        id = AppTagInfo.ID_MSG_FCM, category = 0.cat, icon = R.drawable.ic_firebase_72.icon,
        label = ("FCM" to "Firebase Cloud Messaging (FCM)").strLabels, rank = 16,
        requisites = pkgServicesRequisite().list,
        expressing = serviceExpressing("com.google.firebase.messaging.FirebaseMessagingService")
    ).apply { iconKey = "fcm" },
    AppTagInfo(
        id = AppTagInfo.ID_MSG_HUAWEI, category = 0.cat, icon = R.drawable.ic_huawei_72.icon,
        label = R.string.av_settings_tag_huawei_push.labels, rank = 17,
        requisites = pkgServicesRequisite().list,
        expressing = serviceExpressing("com.huawei.hms.support.api.push.service.HmsMsgService")
    ).apply { iconKey = "hwp" },
    AppTagInfo(
        id = AppTagInfo.ID_MSG_XIAOMI, category = 0.cat, icon = R.drawable.ic_xiaomi_72.icon,
        label = R.string.av_settings_tag_xiaomi_push.labels, rank = 18,
        requisites = pkgServicesRequisite().list,
        expressing = xiaomiMsgExpressing()
    ).apply { iconKey = "mip" },
    AppTagInfo(
        id = AppTagInfo.ID_MSG_MEIZU, category = 0.cat, icon = R.drawable.ic_meizu_72.icon,
        label = R.string.av_settings_tag_meizu_push.labels, rank = 19,
        requisites = pkgServicesRequisite().list,
        expressing = serviceExpressing("com.meizu.cloud.pushsdk.NotificationService")
    ).apply { iconKey = "mzp" },
    AppTagInfo(
        id = AppTagInfo.ID_MSG_OPPO, category = 0.cat, icon = R.drawable.ic_oppo_72.icon,
        label = R.string.av_settings_tag_oppo_push.labels, rank = 20,
        requisites = pkgServicesRequisite().list,
        expressing = serviceExpressing("com.heytap.mcssdk.AppPushService")
    ).apply { iconKey = "oop" },
    AppTagInfo(
        id = AppTagInfo.ID_MSG_VIVO, category = 0.cat, icon = R.drawable.ic_vivo_72.icon,
        label = R.string.av_settings_tag_vivo_push.labels, rank = 21,
        requisites = pkgServicesRequisite().list,
        expressing = serviceExpressing("com.vivo.push.sdk.service.CommandClientService")
    ).apply { iconKey = "vvp" },
    AppTagInfo(
        id = AppTagInfo.ID_MSG_JPUSH, category = 0.cat, icon = R.drawable.ic_aurora_72.icon,
        label = (R.string.av_tag_jpush_normal to R.string.av_settings_tag_jpush).resLabels, rank = 22,
        requisites = pkgServicesRequisite().list,
        expressing = serviceExpressing("cn.jpush.android.service.PushService")
    ).apply { iconKey = "j-p" },
    AppTagInfo(
        id = AppTagInfo.ID_MSG_UPUSH, category = 0.cat, icon = R.drawable.ic_umeng_72.icon,
        label = (R.string.av_tag_upush_normal to R.string.av_settings_tag_upush).resLabels, rank = 23,
        requisites = pkgServicesRequisite().list,
        expressing = serviceExpressing("com.umeng.message.UmengIntentService")
    ).apply { iconKey = "u-p" },
    AppTagInfo(
        id = AppTagInfo.ID_MSG_TPNS, category = 0.cat, icon = R.drawable.ic_tpns_72.icon,
        label = (R.string.av_tag_tpns_normal to R.string.av_settings_tag_tpns).resLabels, rank = 24,
        requisites = pkgServicesRequisite().list,
        expressing = serviceExpressing("com.tencent.android.tpush.service.XGVipPushService")
    ).apply { iconKey = "tpn" },
    AppTagInfo(
        id = AppTagInfo.ID_MSG_ALI, category = 0.cat, icon = R.drawable.ic_emas_72.icon,
        label = (R.string.av_tag_ali_push_normal to R.string.av_settings_tag_ali_push).resLabels, rank = 25,
        requisites = pkgServicesRequisite().list,
        expressing = serviceExpressing("org.android.agoo.accs.AgooService")
    ).apply { iconKey = "alp" },
    AppTagInfo(
        id = AppTagInfo.ID_MSG_BAIDU, category = 0.cat, icon = R.drawable.ic_baidu_push_72.icon,
        label = R.string.av_settings_tag_baidu_push.labels, rank = 26,
        requisites = pkgServicesRequisite().list,
        expressing = serviceExpressing("com.baidu.android.pushservice.PushService")
    ).apply { iconKey = "dup" },
    AppTagInfo(
        id = AppTagInfo.ID_MSG_GETUI, category = 0.cat, icon = R.drawable.ic_getui_72.icon,
        label = R.string.av_settings_tag_getui.labels, rank = 27,
        requisites = pkgServicesRequisite().list,
        expressing = serviceExpressing("com.igexin.sdk.PushService")
    ).apply { iconKey = "get" },
).associateBy(AppTagInfo::id)


// Helper functions

private val Int.icon: AppTagInfo.Icon
    get() = AppTagInfo.Icon(this)

private val CharSequence.icon: AppTagInfo.Icon
    get() = AppTagInfo.Icon(text = this.label)

private val AppTagInfo.Label.icon: AppTagInfo.Icon
    get() = AppTagInfo.Icon(text = this)

private val String.appIcon: AppTagInfo.Icon
    get() = AppTagInfo.Icon(pkgName = this)

private val Int.label: AppTagInfo.Label
    get() = AppTagInfo.Label(this)

private val CharSequence.label: AppTagInfo.Label
    get() = AppTagInfo.Label(string = this)

private val Int.labels: AppTagInfo.Labels
    get() = AppTagInfo.Labels(this.label)

private val CharSequence.labels: AppTagInfo.Labels
    get() = AppTagInfo.Labels(this.label)

private val Pair<CharSequence, CharSequence>.strLabels: AppTagInfo.Labels
    get() = AppTagInfo.Labels(normal = first.label, full = second.label)

private val Pair<CharSequence, Int>.strWithResLabels: AppTagInfo.Labels
    get() = AppTagInfo.Labels(normal = first.label, full = second.label)

private val Pair<Int, Int>.resLabels: AppTagInfo.Labels
    get() = AppTagInfo.Labels(normal = first.label, full = second.label)

private val Int.desc: AppTagInfo.Description
    get() = AppTagInfo.Description(this.label)

private val Any.cat: AppTagInfo.Category
    get() = AppTagInfo.Category()

private fun expressing(block: ExpressibleTag.(AppTagInfo.Resources) -> Boolean): AppTagInfo.Expressing {
    return AppTagInfo.Expressing(block)
}

private fun commonExpressing(checker: (ApiViewingApp) -> Boolean)
        : ExpressibleTag.(AppTagInfo.Resources) -> Boolean = expressing { res ->
    checker.invoke(res.app)
}

private fun serviceExpressing(comp: String)
        : ExpressibleTag.(AppTagInfo.Resources) -> Boolean = expressing { res ->
    res.pkgInfo?.services?.any { it.name == comp } == true
}

private val AppTagInfo.Requisite.list: List<AppTagInfo.Requisite>
    get() = listOf(this)


// Private functions

private fun xiaomiMsgExpressing() = expressing { res ->
    val context = res.context
    val app = res.app
    // "com.xiaomi.mipush.sdk.ManifestChecker" to "checkServices"
    val method = "com.xiaomi.mipush.sdk.u" to "d"
    val args = Context.CONTEXT_INCLUDE_CODE or Context.CONTEXT_IGNORE_SECURITY
    val loaderContext = context.createPackageContext(context.packageName, args)
    val checkerMethod = try {
        loaderContext.classLoader.loadClass(method.first).getDeclaredMethod(
            method.second, Context::class.java, PackageInfo::class.java).apply {
            isAccessible = true
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
    if (checkerMethod != null) {
        try {
            checkerMethod.invoke(null, context, res.pkgInfo)
            true
        } catch (e: Throwable) {
            val message = e.cause?.message
            val appName = app.name
            val appPackage = app.packageName
            val appVer = app.verName
            Log.w("av.main.tag", "$message ($appPackage, $appName $appVer)")
            false
        }
    } else {
        serviceExpressing("com.xiaomi.mipush.sdk.MessageHandleService").invoke(this, res)
    }
}

private fun nativeLibrariesRequisite(): AppTagInfo.Requisite = AppTagInfo.Requisite(
    id = "ReqNativeLibs",
    checker = { res -> res.app.isNativeLibrariesRetrieved },
    loader = { res -> res.app.retrieveNativeLibraries() }
)

private fun appIconRequisite(): AppTagInfo.Requisite = AppTagInfo.Requisite(
    id = "ReqAppIcon",
    checker = { res -> res.app.hasIcon },
    loader = { res ->
        val context = res.context
        val app = res.app
        if (app.isLoadingIcon) {
            val elapsingTime = ElapsingTime()
            while (true) {
                if (app.hasIcon || app.isLoadingIcon.not()) break
                if (elapsingTime.elapsed() > 8000) break
                delay(400)
            }
        }
        if (app.hasIcon.not()) app.retrieveAppIconInfo(app.getOriginalIconDrawable(context)!!.mutate())
    }
)

private fun pkgServicesRequisite(): AppTagInfo.Requisite = AppTagInfo.Requisite(
    id = "ReqPkgServices",
    checker = { res -> res.pkgInfo?.services != null }, // todo store res somewhere
    loader = { res ->
        val app = res.app
        val pm = res.context.packageManager
        val flagGetDisabled = if (OsUtils.satisfy(OsUtils.N)) PackageManager.MATCH_DISABLED_COMPONENTS
        else flagGetDisabledLegacy()
        val extraFlags = PackageManager.GET_SERVICES or PackageManager.GET_RECEIVERS or flagGetDisabled
        res.pkgInfo = try {
            if (app.isArchive) pm.getPackageArchiveInfo(app.appPackage.basePath, extraFlags)
            else pm.getPackageInfo(app.packageName, extraFlags)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }?.apply {
            if (services == null) services = emptyArray()
        }
    }
)

@Suppress("DEPRECATION")
private fun flagGetDisabledLegacy() = PackageManager.GET_DISABLED_COMPONENTS

private fun thirdPartyPkgRequisite(): AppTagInfo.Requisite = AppTagInfo.Requisite(
    id = "ReqThirdPartyPkg",
    checker = { res -> res.app.isThirdPartyPackagesRetrieved },
    loader = { res -> res.app.retrieveThirdPartyPackages() }
)

private fun pkgInstallerRequisite(): AppTagInfo.Requisite = AppTagInfo.Requisite(
    id = "ReqPkgInstaller",
    checker = { res -> res.app.isArchive || res.isPkgInstallerRetrieved }, // todo store result
    loader = { res ->
        val context = res.context
        val app = res.app
        // must enable package installer to know unknown installer, which in turn invalidate package installer
        val installer = if (OsUtils.satisfy(OsUtils.R)) {
            try {
                context.packageManager.getInstallSourceInfo(app.packageName)
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
                null
            }?.installingPackageName
        } else {
            getInstallerLegacy(context, app)
        }
        if (installer == null) {
            res.pkgInstaller = null
            res.isPkgInstallerRetrieved = true
            return@Requisite
        }
        // real name should be used when showing tag name
        // use package name when showing tag icon only
//        val ai = MiscApp.getApplicationInfo(context, packageName = installer)
//        val installerName = ai?.loadLabel(context.packageManager)?.toString() ?: ""
//        val name = if (installerName.isNotEmpty()) {
//            installerName
//        } else {
//            val installerAndroid = ApiViewingApp.packagePackageInstaller
//            when (installer) {
//                installerGPlay -> context.getString(MyR.string.apiDetailsInstallGP)
//                installerAndroid -> context.getString(MyR.string.apiDetailsInstallPI)
//                "null" -> null
//                else -> null
//            }
//        }
        // initialize installer that is neither Google Play Store nor Android Package Installer
        AppTagInflater.ensureTagIcon(context, installer)
        res.pkgInstaller = installer
        res.isPkgInstallerRetrieved = true
        res.dynamicLabel = installer
        res.dynamicIconKey = installer
    }
)

@Suppress("DEPRECATION")
private fun getInstallerLegacy(context: Context, app: ApiViewingApp): String? {
    return try {
        context.packageManager.getInstallerPackageName(app.packageName)
    } catch (e: IllegalArgumentException) {
        e.printStackTrace()
        null
    }
}
