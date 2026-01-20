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
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import com.madness.collision.misc.PackageCompat
import com.madness.collision.unit.api_viewing.R
import com.madness.collision.unit.api_viewing.data.ApiUnit
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.data.ArchiveEntryFlags
import com.madness.collision.unit.api_viewing.data.DexPackageFlags
import com.madness.collision.unit.api_viewing.info.AppType
import com.madness.collision.unit.api_viewing.list.AppListService
import com.madness.collision.unit.api_viewing.tag.inflater.AppTagInflater
import com.madness.collision.unit.api_viewing.util.ManifestUtil
import com.madness.collision.util.os.OsUtils
import io.cliuff.boundo.art.apk.dex.AsyncDexLibSuperFinder
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout

internal fun builtInTags(): Map<String, AppTagInfo> = listOf(
    AppTagInfo(
        id = AppTagInfo.ID_APP_INSTALLER_PLAY, category = 0.cat, icon = ApiViewingApp.packagePlayStore.appIcon,
        label = R.string.apiDetailsInstallGP.labels, rank = "10",  // todo dynamic label
        desc = installerResultDesc,
        requisites = pkgInstallerRequisite().list,
        expressing = expressing { res -> res.app.pkgInstaller == ApiViewingApp.packagePlayStore }
    ).apply { iconKey = ApiViewingApp.packagePlayStore },
    AppTagInfo(
        id = AppTagInfo.ID_APP_INSTALLER, category = 0.cat, icon = AppTagInfo.Icon(isDynamic = true),
        label = AppTagInfo.Labels(full = R.string.apiDetailsInstallPI.label, isDynamic = true), rank = "11",
        desc = installerResultDesc,
        requisites = pkgInstallerRequisite().list,
        expressing = expressing { res ->
            val installer = res.app.pkgInstaller ?: return@expressing false
            installer != ApiViewingApp.packagePlayStore
        }
    ),

    AppTagInfo(
        id = AppTagInfo.ID_TECH_KOTLIN, category = 0.cat, icon = R.drawable.ic_kotlin_72.icon,
        label = R.string.av_tag_kotlin.labels, rank = "12",
        desc = "kotlin".packageResultDesc,
        requisites = kotlinRequisite().list,
        expressing = commonExpressing {
            ArchiveEntryFlags.BIT_KOTLIN in it.archiveEntryFlags ||
                    DexPackageFlags.BIT_KOTLIN in it.dexPackageFlags ||
                    DexPackageFlags.BIT_JETPACK_COMPOSE in it.dexPackageFlags ||
                    DexPackageFlags.BIT_COMPOSE_MULTIPLATFORM in it.dexPackageFlags
        }
    ).apply { iconKey = "kot" },
    AppTagInfo(
        id = AppTagInfo.ID_TECH_X_COMPOSE, category = 0.cat, icon = R.drawable.ic_cmp_72.icon,
        label = R.string.av_tag_compose.labels, rank = "13",
        desc = "androidx.compose".packageResultDesc,
        requisites = thirdPartyPkgRequisite().list,
        expressing = commonExpressing { DexPackageFlags.BIT_JETPACK_COMPOSE in it.dexPackageFlags }
    ).apply { iconKey = "xcm" },
    AppTagInfo(
        id = AppTagInfo.ID_TECH_COMPOSE_CMP, category = 0.cat, icon = R.drawable.ic_compose_cmp_72.icon,
        label = R.string.av_tag_compose_cmp.labels, rank = "135",
        desc = "org.jetbrains.compose".packageResultDesc,
        requisites = thirdPartyPkgRequisite().list,
        expressing = commonExpressing { DexPackageFlags.BIT_COMPOSE_MULTIPLATFORM in it.dexPackageFlags }
    ).apply { iconKey = "cmp" },
    AppTagInfo(
        id = AppTagInfo.ID_TECH_FLUTTER, category = 0.cat, icon = R.drawable.ic_flutter_72.icon,
        label = R.string.av_tag_flutter.labels, rank = "14",
        desc = "libflutter.so".fileResultDesc,
        requisites = archiveEntriesRequisite().list,
        expressing = commonExpressing { ArchiveEntryFlags.BIT_LIB_FLUTTER in it.archiveEntryFlags }
    ).apply { iconKey = "flu" },
    AppTagInfo(
        id = AppTagInfo.ID_TECH_REACT_NATIVE, category = 0.cat, icon = R.drawable.ic_react_72.icon,
        label = R.string.av_tag_react_native.labels, rank = "15",
        desc = "libreactnative.so".fileResultDesc,
        requisites = archiveEntriesRequisite().list,
        expressing = commonExpressing { ArchiveEntryFlags.BIT_LIB_REACT_NATIVE in it.archiveEntryFlags }
    ).apply { iconKey = "rn" },
    AppTagInfo(
        id = AppTagInfo.ID_TECH_XAMARIN, category = 0.cat, icon = R.drawable.ic_xamarin_72.icon,
        label = R.string.av_tag_xamarin.labels, rank = "16",
        desc = "libxamarin-app.so".fileResultDesc,
        requisites = archiveEntriesRequisite().list,
        expressing = commonExpressing { ArchiveEntryFlags.BIT_LIB_XAMARIN in it.archiveEntryFlags }
    ).apply { iconKey = "xam" },
    AppTagInfo(
        id = AppTagInfo.ID_TECH_MAUI, category = 0.cat, icon = R.drawable.ic_dot_net_72.icon,
        label = R.string.av_tag_maui.labels, rank = "165",
        desc = "libaot-Microsoft.Maui.dll.so".fileResultDesc,
        requisites = mauiRequisite().list,
        expressing = commonExpressing {
            ArchiveEntryFlags.BIT_LIB_MAUI in it.archiveEntryFlags ||
                    DexPackageFlags.BIT_MAUI in it.dexPackageFlags
        }
    ).apply { iconKey = "mau" },
    AppTagInfo(
        id = AppTagInfo.ID_TECH_CORDOVA, category = 0.cat, icon = R.drawable.ic_cordova_72.icon,
        label = R.string.av_tag_cordova.labels, rank = "166",
        desc = "org.apache.cordova".packageResultDesc,
        requisites = thirdPartyPkgRequisite().list,
        expressing = commonExpressing { DexPackageFlags.BIT_CORDOVA in it.dexPackageFlags }
    ).apply { iconKey = "cdv" },

    AppTagInfo(
        id = AppTagInfo.ID_APP_ADAPTIVE_ICON, category = 0.cat, icon = R.drawable.ic_ai_72.icon,
        label = R.string.av_ai.labels, rank = "17",
        desc = R.string.av_tag_result_ai.resultDesc,
        requisites = appIconRequisite().list,
        expressing = commonExpressing { it.adaptiveIcon }
    ).apply { iconKey = "ai" },
    AppTagInfo(
        id = AppTagInfo.ID_PKG_AAB, category = 0.cat, icon = R.drawable.ic_aab_72.icon,
        label = R.string.av_tag_has_splits.labels, rank = "18",
        desc = R.string.av_tag_result_aab.resultDesc,
        expressing = commonExpressing { it.appPackage.hasSplits }
    ).apply { iconKey = "aab" },
    AppTagInfo(
        id = AppTagInfo.ID_APP_SYSTEM, category = 0.cat, icon = R.drawable.ic_system_72.icon,
        label = (R.string.av_adapter_tag_system to R.string.av_settings_tags_system).resLabels, rank = "19",
        desc = R.string.av_tag_result_sys.resultDesc,
        expressing = commonExpressing { it.apiUnit == ApiUnit.SYS }
    ).apply { iconKey = "sys" },
    AppTagInfo(
        id = AppTagInfo.ID_APP_SYSTEM_CORE, category = 0.cat, icon = R.string.av_tag_ic_system_core.label.icon,
        label = R.string.av_tag_system_core.labels, rank = "20",
        desc = R.string.av_tag_result_system_core.resultDesc,
        expressing = commonExpressing { it.isCoreApp == true }
    ),
    AppTagInfo(
        id = AppTagInfo.ID_APP_SYSTEM_MODULE, category = 0.cat, icon = R.string.av_tag_ic_system_module.label.icon,
        label = R.string.av_tag_system_module.labels, rank = "21",
        desc = R.string.av_tag_result_system_module.resultDesc,
        availability = sdkAvailable(OsUtils.Q),
        expressing = commonExpressing { it.moduleInfo != null }
    ),
    AppTagInfo(
        id = AppTagInfo.ID_APP_CATEGORY, category = 0.cat, icon = "CAT".icon,
        label = R.string.av_tag_category.labels, rank = "211",
        desc = R.string.av_tag_result_category.resultDesc,
        valueExpressing = exp@{ _, res ->
            val cat = res.app.category ?: return@exp null
            if (OsUtils.dissatisfy(OsUtils.O)) return@exp null
            ApplicationInfo.getCategoryTitle(res.context, cat)?.toString()
        },
        expressing = commonExpressing {
            it.category != null && it.category != ApplicationInfo.CATEGORY_UNDEFINED
        }
    ),
    AppTagInfo(
        id = AppTagInfo.ID_TYPE_OVERLAY, category = 0.cat, icon = "RRO".icon,
        label = R.string.av_tag_type_overlay.labels, rank = "22",
        desc = R.string.av_tag_result_type_overlay.resultDesc,
        expressing = commonExpressing { it.appType is AppType.Overlay }
    ),
    AppTagInfo(
        id = AppTagInfo.ID_TYPE_INSTANT, category = 0.cat, icon = R.string.av_tag_ic_type_instant.label.icon,
        label = R.string.av_tag_type_instant.labels, rank = "23",
        desc = R.string.av_tag_result_type_instant.resultDesc,
        availability = sdkAvailable(OsUtils.O),
        expressing = commonExpressing { it.appType == AppType.InstantApp }
    ),
    AppTagInfo(
        id = AppTagInfo.ID_TYPE_WEB_APK, category = 0.cat, icon = R.drawable.ic_pwa_72.icon,
        label = R.string.av_tag_type_web_apk.labels, rank = "24",
        desc = R.string.av_tag_result_type_web_apk.resultDesc,
        expressing = commonExpressing { it.appType == AppType.WebApk }
    ).apply { iconKey = "pwa" },
    AppTagInfo(
        id = AppTagInfo.ID_APP_HIDDEN, category = 0.cat, icon = R.drawable.ic_hidden_72.icon,
        label = (R.string.av_adapter_tag_hidden to R.string.av_settings_tag_hidden).resLabels, rank = "25",
        desc = R.string.av_tag_result_hidden.resultDesc,
        expressing = commonExpressing { it.isNotArchive && !it.isLaunchable }
    ).apply { iconKey = "hid" },
    AppTagInfo(
        id = AppTagInfo.ID_APP_PREDICTIVE_BACK, category = 0.cat, icon = R.string.av_tag_ic_predictive_back.label.icon,
        label = R.string.av_tag_predictive_back.labels, rank = "26",
        desc = R.string.av_tag_result_predictive_back.resultDesc,
        availability = sdkAvailable(OsUtils.T),
        expressing = commonExpressing { it.isBackCallbackEnabled == true }
    ),

    AppTagInfo(
        id = AppTagInfo.ID_PKG_64BIT, category = 0.cat, icon = R.string.av_tag_full_64bit.label.icon,
        label = R.string.av_tag_full_64bit.labels, rank = "27",
        desc = R.string.av_tag_result_64bit.resultDesc,
        availability = { Build.SUPPORTED_64_BIT_ABIS.isNotEmpty() },
        requisites = archiveEntriesRequisite().list,
        expressing = commonExpressing { ArchiveEntryFlags.BIT_NATIVE_LIBS_64B in it.archiveEntryFlags }
    ),

    AppTagInfo(
        id = AppTagInfo.ID_MSG_FCM, category = 0.cat, icon = R.drawable.ic_firebase_72.icon,
        label = (R.string.av_tag_fcm_normal to R.string.av_tag_fcm_full).resLabels, rank = "32",
        desc = "com.google.firebase.messaging.FirebaseMessagingService".serviceResultDesc,
        requisites = pkgServicesRequisite().list,
        expressing = serviceExpressing("com.google.firebase.messaging.FirebaseMessagingService")
    ).apply { iconKey = "fcm" },
    AppTagInfo(
        id = AppTagInfo.ID_MSG_HUAWEI, category = 0.cat, icon = R.drawable.ic_huawei_72.icon,
        label = R.string.av_settings_tag_huawei_push.labels, rank = "33",
        desc = "com.huawei.hms.support.api.push.service.HmsMsgService".serviceResultDesc,
        requisites = pkgServicesRequisite().list,
        expressing = serviceExpressing("com.huawei.hms.support.api.push.service.HmsMsgService")
    ).apply { iconKey = "hwp" },
    AppTagInfo(
        id = AppTagInfo.ID_MSG_XIAOMI, category = 0.cat, icon = R.drawable.ic_xiaomi_72.icon,
        label = R.string.av_settings_tag_xiaomi_push.labels, rank = "34",
        desc = "com.xiaomi.mipush.sdk.MessageHandleService".mipushServiceResultDesc,
        requisites = listOf(pkgServicesRequisite(), xiaomiMsgRequisite()),
        expressing = xiaomiMsgExpressing()
    ).apply { iconKey = "mip" },
    AppTagInfo(
        id = AppTagInfo.ID_MSG_MEIZU, category = 0.cat, icon = R.drawable.ic_meizu_72.icon,
        label = R.string.av_settings_tag_meizu_push.labels, rank = "35",
        desc = "com.meizu.cloud.pushsdk.NotificationService".serviceResultDesc,
        requisites = pkgServicesRequisite().list,
        expressing = serviceExpressing("com.meizu.cloud.pushsdk.NotificationService")
    ).apply { iconKey = "mzp" },
    AppTagInfo(
        id = AppTagInfo.ID_MSG_OPPO, category = 0.cat, icon = R.drawable.ic_oppo_72.icon,
        label = R.string.av_settings_tag_oppo_push.labels, rank = "36",
        desc = "com.heytap.msp.push.service.DataMessageCallbackService".serviceResultDesc,
        requisites = pkgServicesRequisite().list,
        expressing = serviceExpressing(
            "com.heytap.msp.push.service.DataMessageCallbackService",
            "com.heytap.mcssdk.AppPushService",
        )
    ).apply { iconKey = "oop" },
    AppTagInfo(
        id = AppTagInfo.ID_MSG_VIVO, category = 0.cat, icon = R.drawable.ic_vivo_72.icon,
        label = R.string.av_settings_tag_vivo_push.labels, rank = "37",
        desc = "com.vivo.push.sdk.service.CommandClientService".serviceResultDesc,
        requisites = pkgServicesRequisite().list,
        expressing = serviceExpressing("com.vivo.push.sdk.service.CommandClientService")
    ).apply { iconKey = "vvp" },
    AppTagInfo(
        id = AppTagInfo.ID_MSG_JPUSH, category = 0.cat, icon = R.drawable.ic_aurora_72.icon,
        label = (R.string.av_tag_jpush_normal to R.string.av_settings_tag_jpush).resLabels, rank = "38",
        desc = "cn.jpush.android.service.PushService".serviceResultDesc,
        requisites = pkgServicesRequisite().list,
        expressing = serviceExpressing("cn.jpush.android.service.PushService")
    ).apply { iconKey = "j-p" },
    AppTagInfo(
        id = AppTagInfo.ID_MSG_UPUSH, category = 0.cat, icon = R.drawable.ic_umeng_72.icon,
        label = (R.string.av_tag_upush_normal to R.string.av_settings_tag_upush).resLabels, rank = "39",
        desc = "com.umeng.message.UmengIntentService".serviceResultDesc,
        requisites = pkgServicesRequisite().list,
        expressing = serviceExpressing("com.umeng.message.UmengIntentService")
    ).apply { iconKey = "u-p" },
    AppTagInfo(
        id = AppTagInfo.ID_MSG_TPNS, category = 0.cat, icon = R.drawable.ic_tpns_72.icon,
        label = (R.string.av_tag_tpns_normal to R.string.av_settings_tag_tpns).resLabels, rank = "40",
        desc = "com.tencent.android.tpush.service.XGVipPushService".serviceResultDesc,
        requisites = pkgServicesRequisite().list,
        expressing = serviceExpressing("com.tencent.android.tpush.service.XGVipPushService")
    ).apply { iconKey = "tpn" },
    AppTagInfo(
        id = AppTagInfo.ID_MSG_ALI, category = 0.cat, icon = R.drawable.ic_emas_72.icon,
        label = (R.string.av_tag_ali_push_normal to R.string.av_settings_tag_ali_push).resLabels, rank = "41",
        desc = "org.android.agoo.accs.AgooService".serviceResultDesc,
        requisites = pkgServicesRequisite().list,
        expressing = serviceExpressing("org.android.agoo.accs.AgooService")
    ).apply { iconKey = "alp" },
    AppTagInfo(
        id = AppTagInfo.ID_MSG_BAIDU, category = 0.cat, icon = R.drawable.ic_baidu_push_72.icon,
        label = R.string.av_settings_tag_baidu_push.labels, rank = "42",
        desc = "com.baidu.android.pushservice.PushService".serviceResultDesc,
        requisites = pkgServicesRequisite().list,
        expressing = serviceExpressing("com.baidu.android.pushservice.PushService")
    ).apply { iconKey = "dup" },
    AppTagInfo(
        id = AppTagInfo.ID_MSG_GETUI, category = 0.cat, icon = R.drawable.ic_getui_72.icon,
        label = R.string.av_settings_tag_getui.labels, rank = "43",
        desc = "com.igexin.sdk.PushService".serviceResultDesc,
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

private val Int.resultDesc: AppTagInfo.Description
    get() = AppTagInfo.Description(checkResultDesc = { this.label })

private val CharSequence.resultDesc: AppTagInfo.Description
    get() = AppTagInfo.Description(checkResultDesc = { this.label })

private val CharSequence.serviceResultDesc: AppTagInfo.Description
    get() = AppTagInfo.Description(checkResultDesc = { it.context.getString(R.string.av_tag_result_service, this).label })

private val CharSequence.mipushServiceResultDesc: AppTagInfo.Description
    get() = AppTagInfo.Description(checkResultDesc = { it.context.getString(R.string.av_tag_result_mipush_service, this).label })

private val CharSequence.fileResultDesc: AppTagInfo.Description
    get() = AppTagInfo.Description(checkResultDesc = { it.context.getString(R.string.av_tag_result_file, this).label })

private val CharSequence.packageResultDesc: AppTagInfo.Description
    get() = AppTagInfo.Description(checkResultDesc = { it.context.getString(R.string.av_tag_result_pkg, this).label })

private val installerResultDesc: AppTagInfo.Description
    get() = AppTagInfo.Description(checkResultDesc = {
        val name = AppListService().getInstallerName(it.context, it.app.pkgInstaller)
        it.context.getString(R.string.av_tag_result_installer, name).label
    })

private val Any.cat: AppTagInfo.Category
    get() = AppTagInfo.Category()

private fun sdkAvailable(int: Int): AppTagInfo.Availability {
    return AppTagInfo.Availability { OsUtils.satisfy(int) }
}

private fun expressing(block: ExpressibleTag.(AppTagInfo.Resources) -> Boolean): AppTagInfo.Expressing {
    return AppTagInfo.Expressing(block)
}

private fun commonExpressing(checker: (ApiViewingApp) -> Boolean)
        : ExpressibleTag.(AppTagInfo.Resources) -> Boolean = expressing { res ->
    checker.invoke(res.app)
}

private fun serviceExpressing(vararg comp: String)
        : ExpressibleTag.(AppTagInfo.Resources) -> Boolean = expressing { res ->
    val serviceClasses = res.app.serviceFamilyClasses.orEmpty()
    comp.any(serviceClasses::contains)
}

private val AppTagInfo.Requisite.list: List<AppTagInfo.Requisite>
    get() = listOf(this)


// Private functions

private fun xiaomiMsgExpressing() = expressing { res ->
    when (val enabled = res.app.isMiPushEnabled) {
        true, false -> enabled
        null -> serviceExpressing("com.xiaomi.mipush.sdk.MessageHandleService").invoke(this, res)
    }
}

private fun xiaomiMsgRequisite(): AppTagInfo.Requisite =
    AppTagInfo.Requisite(
        id = "ReqXiaomiMsg",
        checker = { res -> res.app.isMiPushSdkChecked },
    ) { res ->

    val context = res.context
    val app = res.app
    // "com.xiaomi.mipush.sdk.ManifestChecker" to "checkServices"
    val method = "com.xiaomi.mipush.sdk.m" to "d"
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
    app.isMiPushEnabled = checkerMethod?.let {
        try {
            // wait for services, throw exception on timeout
            withTimeout(10_000) {
                while (true) {
                    if (app.pkgInfo?.services != null) break
                    if (app.serviceFamilyClasses != null) break
                    delay(25)
                }
            }
            checkerMethod.invoke(null, context, res.app.pkgInfo)
            true
        } catch (e: Throwable) {
            val message = e.cause?.message
            val appName = app.name
            val appPackage = app.packageName
            val appVer = app.verName
            Log.w("av.main.tag", "$message ($appPackage, $appName $appVer)")
            false
        }
    }
}

private fun archiveEntriesRequisite(): AppTagInfo.Requisite = AppTagInfo.Requisite(
    id = "ReqArchiveEntries",
    checker = { res -> res.app.archiveEntryFlags.isValidRev },
    loader = { res -> res.app.retrieveArchiveEntries() }
)

private fun kotlinRequisite(): AppTagInfo.Requisite = AppTagInfo.Requisite(
    id = "ReqKotlin",
    checker = { res ->
        res.app.archiveEntryFlags.run { isValidRev && contains(ArchiveEntryFlags.BIT_KOTLIN) } ||
                res.app.dexPackageFlags.isValidRev
    },
    loader = { res ->
        if (!res.app.archiveEntryFlags.isValidRev) res.app.retrieveArchiveEntries()
        if (ArchiveEntryFlags.BIT_KOTLIN !in res.app.archiveEntryFlags) res.app.retrieveThirdPartyPackages()
    }
)

private fun mauiRequisite(): AppTagInfo.Requisite = AppTagInfo.Requisite(
    id = "ReqMaui",
    checker = { res ->
        res.app.archiveEntryFlags.run { isValidRev && contains(ArchiveEntryFlags.BIT_LIB_MAUI) } ||
                res.app.dexPackageFlags.isValidRev
    },
    loader = { res ->
        if (!res.app.archiveEntryFlags.isValidRev) res.app.retrieveArchiveEntries()
        if (ArchiveEntryFlags.BIT_LIB_MAUI !in res.app.archiveEntryFlags) res.app.retrieveThirdPartyPackages()
    }
)

private fun appIconRequisite(): AppTagInfo.Requisite = AppTagInfo.Requisite(
    id = "ReqAppIcon",
    checker = { res -> res.app.hasIcon },
    loader = { res ->
        val context = res.context
        val app = res.app
        run icon@{
            if (app.hasIcon) return@icon
            val appInfo = app.getApplicationInfo(context) ?: return@icon
            val icon = context.packageManager.getApplicationIcon(appInfo).mutate()
            val set = listOf(icon) + ManifestUtil.getIconSet(context, appInfo, app.appPackage.basePath)
            app.retrieveAppIconInfo(set)
        }
    }
)

private fun pkgServicesRequisite(): AppTagInfo.Requisite = AppTagInfo.Requisite(
    id = "ReqPkgServices",
    checker = { res -> res.app.serviceFamilyClasses != null },
    loader = { res ->
        val app = res.app
        val pm = res.context.packageManager
        val flagGetDisabled = if (OsUtils.satisfy(OsUtils.N)) PackageManager.MATCH_DISABLED_COMPONENTS
        else flagGetDisabledLegacy()
        val extraFlags = PackageManager.GET_SERVICES or PackageManager.GET_RECEIVERS or flagGetDisabled
        app.pkgInfo = try {
            when {
                app.isArchive -> PackageCompat.getArchivePackage(pm, app.appPackage.basePath, extraFlags)
                else -> PackageCompat.getInstalledPackage(pm, app.packageName, extraFlags)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }?.apply {
            if (services == null) services = emptyArray()
        }

        // services declared in manifests are not guaranteed to be found in DEX files
        val services = app.pkgInfo?.services?.run { mapTo(HashSet(size), ServiceInfo::name) }.orEmpty()
        val finder = AsyncDexLibSuperFinder()
        app.serviceFamilyClasses = finder.resolve(app.appPackage.apkPaths, services)
        // todo reduce service set to only ones of interest, to save more memory
    }
)

@Suppress("DEPRECATION")
private fun flagGetDisabledLegacy() = PackageManager.GET_DISABLED_COMPONENTS

private fun thirdPartyPkgRequisite(): AppTagInfo.Requisite = AppTagInfo.Requisite(
    id = "ReqThirdPartyPkg",
    checker = { res -> res.app.dexPackageFlags.isValidRev },
    loader = { res -> res.app.retrieveThirdPartyPackages() }
)

private fun pkgInstallerRequisite(): AppTagInfo.Requisite = AppTagInfo.Requisite(
    id = "ReqPkgInstaller",
    checker = { res ->
        when {
            res.app.isArchive -> true
            res.app.isPkgInstallerNull -> true
            // ensure dynamic requisite properties
            else -> res.dynamicRequisiteLabels.containsKey("ReqPkgInstaller")
        }
    },
    loader = { res ->
        val context = res.context
        val app = res.app
        // must enable package installer to know unknown installer, which in turn invalidate package installer
        val installer = if (app.isPkgInstallerLoaded) {
            // avoid duplicated loading while ensuring dynamic requisite properties
            app.pkgInstaller
        } else if (OsUtils.satisfy(OsUtils.R)) {
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
            app.pkgInstaller = null
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
        app.pkgInstaller = installer
        val reqId = "ReqPkgInstaller"
        res.dynamicRequisiteLabels[reqId] = installer
        res.dynamicRequisiteIconKeys[reqId] = installer
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
