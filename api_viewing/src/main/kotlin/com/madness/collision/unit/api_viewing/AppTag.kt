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

package com.madness.collision.unit.api_viewing

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.forEach
import com.madness.collision.unit.api_viewing.data.ApiUnit
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.databinding.AdapterAvTagBinding
import com.madness.collision.unit.api_viewing.tag.*
import com.madness.collision.unit.api_viewing.tag.PackageTag.Companion.TAG_ID_64B
import com.madness.collision.unit.api_viewing.tag.PackageTag.Companion.TAG_ID_AI
import com.madness.collision.unit.api_viewing.tag.PackageTag.Companion.TAG_ID_ARM
import com.madness.collision.unit.api_viewing.tag.PackageTag.Companion.TAG_ID_FLU
import com.madness.collision.unit.api_viewing.tag.PackageTag.Companion.TAG_ID_GP
import com.madness.collision.unit.api_viewing.tag.PackageTag.Companion.TAG_ID_HID
import com.madness.collision.unit.api_viewing.tag.PackageTag.Companion.TAG_ID_KOT
import com.madness.collision.unit.api_viewing.tag.PackageTag.Companion.TAG_ID_PI
import com.madness.collision.unit.api_viewing.tag.PackageTag.Companion.TAG_ID_RN
import com.madness.collision.unit.api_viewing.tag.PackageTag.Companion.TAG_ID_SPL
import com.madness.collision.unit.api_viewing.tag.PackageTag.Companion.TAG_ID_SYS
import com.madness.collision.unit.api_viewing.tag.PackageTag.Companion.TAG_ID_X86
import com.madness.collision.unit.api_viewing.tag.PackageTag.Companion.TAG_ID_XAM
import com.madness.collision.unit.api_viewing.util.PrefUtil
import com.madness.collision.util.ThemeUtil
import com.madness.collision.util.X
import com.madness.collision.util.os.OsUtils
import kotlin.math.roundToInt
import com.madness.collision.R as MainR

internal object AppTag {

    private const val TAG_KEY_FLUTTER = "flu"
    private const val TAG_KEY_REACT_NATIVE = "rn"
    private const val TAG_KEY_Xamarin = "xam"
    private const val TAG_KEY_Kotlin = "kot"
    private const val TAG_KEY_64B = "64b"
    private const val TAG_KEY_SYS = "sys"
    private const val TAG_KEY_HID = "hid"
    private const val TAG_KEY_AAB = "aab"
    private const val TAG_KEY_AI = "ai"
    private const val TAG_KEY_FCM = "fcm"
    private const val TAG_KEY_HUAWEI_PUSH = "hwp"
    private const val TAG_KEY_XIAOMI_PUSH = "mip"
    private const val TAG_KEY_MEIZU_PUSH = "mzp"
    private const val TAG_KEY_OPPO_PUSH = "oop"
    private const val TAG_KEY_VIVO_PUSH = "vvp"
    private const val TAG_KEY_JPUSH = "j-p"
    private const val TAG_KEY_UPUSH = "u-p"
    private const val TAG_KEY_TPNS = "tpn"
    private const val TAG_KEY_ALI_PUSH = "alp"
    private const val TAG_KEY_BAIDU_PUSH = "dup"
    private const val TAG_KEY_GETUI = "get"

    private val tagIcons = mutableMapOf<String, Bitmap>()
    private var colorBackground: Int? = null
    private val tagIds = mutableMapOf<Int, String>()
    private val displayingTagsPrivate = mutableMapOf<String, TriStateSelectable>()
    private val displayingTags: Map<String, TriStateSelectable>
        get() = displayingTagsPrivate
    private var shouldShowTagCrossPlatformFlutter = false
    private var shouldShowTagCrossPlatformReactNative = false
    private var shouldShowTagCrossPlatformXamarin = false
    private var shouldShowTagKotlin = false
    private val shouldShowTagCrossPlatform: Boolean
        get() = shouldShowTagCrossPlatformFlutter || shouldShowTagCrossPlatformReactNative || shouldShowTagCrossPlatformXamarin
    private var shouldShowTagNativeLibArm = false
    private var shouldShowTagNativeLibX86 = false
    private val shouldShowTagNativeLib: Boolean
        get() = shouldShowTagNativeLibArm || shouldShowTagNativeLibX86

    fun clearCache() {
        tagIcons.clear()
    }

    fun clearContext() {
        colorBackground = null
    }

    private fun <V> Map<String, V>.get(context: Context, id: Int) = this[tagId(context, id)]

    private fun Map<String, TriStateSelectable>.getNonNull(context: Context, id: Int): TriStateSelectable {
        val name = tagId(context, id)
        return this[name] ?: TriStateSelectable(name, TriStateSelectable.STATE_DESELECTED)
    }

    private fun <V> MutableMap<String, V>.put(context: Context, id: Int, value: V) {
        this[tagId(context, id)] = value
    }

    private fun isSelected(context: Context, id: Int): Boolean {
        return displayingTags.get(context, id)?.isSelected == true
    }

    private fun isAntied(context: Context, resId: Int): Boolean {
        return displayingTags.get(context, resId)?.isAntiSelected == true
    }

    private fun isChecking(context: Context, resId: Int): Boolean {
        return isSelected(context, resId) || isAntied(context, resId)
    }

    private fun getTagColor(context: Context): Int {
        return colorBackground ?: ThemeUtil.getColor(context, MainR.attr.colorAOnBackground).let {
            ColorUtils.setAlphaComponent(it, 14)
        }.also { colorBackground = it }
    }

    private fun tagId(context: Context, resId: Int): String {
        return tagIds[resId] ?: context.getString(resId).also {
            tagIds[resId] = it
        }
    }

    private fun makeTagIcon(context: Context, resId: Int): Bitmap? {
        val drawable = try {
            ContextCompat.getDrawable(context, resId)
        } catch (e: Resources.NotFoundException) {
            e.printStackTrace()
            null
        } ?: return null
        return makeTagIcon(context, drawable)
    }

    private fun makeTagIcon(context: Context, drawable: Drawable): Bitmap? {
        val width = X.size(context, 12f, X.DP).roundToInt()
        val bitmap = Bitmap.createBitmap(width, width, Bitmap.Config.ARGB_8888)
        drawable.setBounds(0, 0, width, width)
        drawable.draw(Canvas(bitmap))
        return bitmap
    }

    private fun ensureTagIcon(context: Context, key: String, resId: Int): Bitmap? {
        val icon = tagIcons[key]
        if (icon != null) return icon
        val re = makeTagIcon(context, resId)
        if (re != null) {
            tagIcons[key] = re
        }
        return re
    }

    private fun ensureTagIcon(context: Context, key: String, drawable: Drawable): Bitmap? {
        val icon = tagIcons[key]
        if (icon != null) return icon
        val re = makeTagIcon(context, drawable)
        if (re != null) {
            tagIcons[key] = re
        }
        return re
    }

    private fun initInstaller(context: Context, name: String) {
        val iconDrawable = try {
            context.packageManager.getApplicationIcon(name)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            null
        }
        if (iconDrawable != null) {
            ensureTagIcon(context, name, iconDrawable)
        }
    }

    /**
     * prepare res for package installer tags
     */
    fun ensureInstaller(context: Context, appInfo: ApiViewingApp): String? {
        if (!isChecking(context, TAG_ID_GP) && !isChecking(context, TAG_ID_PI)) return null
        // must enable package installer to know unknown installer, which in turn invalidate package installer
        val installer = if (X.aboveOn(X.R)) {
            try {
                context.packageManager.getInstallSourceInfo(appInfo.packageName)
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
                null
            }?.installingPackageName
        } else {
            getInstallerLegacy(context, appInfo)
        } ?: return null
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
        if (tagIcons[installer] == null) {
            initInstaller(context, installer)
        }
        return installer
    }

    @Suppress("deprecation")
    private fun getInstallerLegacy(context: Context, app: ApiViewingApp): String? {
        return try {
            context.packageManager.getInstallerPackageName(app.packageName)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            null
        }
    }

    private fun ensureTagIcons(context: Context) {
        val isAntiedFlu = isAntied(context, TAG_ID_FLU)
        val isAntiedRn = isAntied(context, TAG_ID_RN)
        val isAntiedXam = isAntied(context, TAG_ID_XAM)
        val isAntiedKot = isAntied(context, TAG_ID_KOT)
        if (shouldShowTagCrossPlatformFlutter || (!isAntiedFlu && (isAntiedRn || isAntiedXam))) {
            ensureTagIcon(context, TAG_KEY_FLUTTER, R.drawable.ic_flutter_72)
        }
        if (shouldShowTagCrossPlatformReactNative || (!isAntiedRn && (isAntiedFlu || isAntiedXam))) {
            ensureTagIcon(context, TAG_KEY_REACT_NATIVE, R.drawable.ic_react_72)
        }
        if (shouldShowTagCrossPlatformXamarin || (!isAntiedXam && (isAntiedRn || isAntiedFlu))) {
            ensureTagIcon(context, TAG_KEY_Xamarin, R.drawable.ic_xamarin_72)
        }
        if (shouldShowTagKotlin || !isAntiedKot) {
            ensureTagIcon(context, TAG_KEY_Kotlin, R.drawable.ic_kotlin_72)
        }

        if (isSelected(context, TAG_ID_64B)) {
            ensureTagIcon(context, TAG_KEY_64B, R.drawable.ic_64b_72)
        }

        val serviceTags = listOf(
                PackageTag.TAG_ID_FCM, PackageTag.TAG_ID_HWP, PackageTag.TAG_ID_MIP,
                PackageTag.TAG_ID_MZP, PackageTag.TAG_ID_OOP, PackageTag.TAG_ID_VVP,
                PackageTag.TAG_ID_J_P, PackageTag.TAG_ID_U_P, PackageTag.TAG_ID_TCP,
                PackageTag.TAG_ID_ALP, PackageTag.TAG_ID_DUP, PackageTag.TAG_ID_GTP
        )
        val icons = listOf(
                TAG_KEY_FCM to R.drawable.ic_firebase_72,
                TAG_KEY_HUAWEI_PUSH to R.drawable.ic_huawei_72,
                TAG_KEY_XIAOMI_PUSH to R.drawable.ic_xiaomi_72,
                TAG_KEY_MEIZU_PUSH to R.drawable.ic_meizu_72,
                TAG_KEY_OPPO_PUSH to R.drawable.ic_oppo_72,
                TAG_KEY_VIVO_PUSH to R.drawable.ic_vivo_72,
                TAG_KEY_JPUSH to R.drawable.ic_aurora_72,
                TAG_KEY_UPUSH to R.drawable.ic_umeng_72,
                TAG_KEY_TPNS to R.drawable.ic_tpns_72,
                TAG_KEY_ALI_PUSH to R.drawable.ic_emas_72,
                TAG_KEY_BAIDU_PUSH to R.drawable.ic_baidu_push_72,
                TAG_KEY_GETUI to R.drawable.ic_getui_72,
        )
        serviceTags.forEachIndexed { index, tag ->
            val state = displayingTags.getNonNull(context, tag)
            if (state.isSelected) {
                val icon = icons[index]
                ensureTagIcon(context, icon.first, icon.second)
            }
        }

        if (isSelected(context, TAG_ID_SYS)) {
            ensureTagIcon(context, TAG_KEY_SYS, R.drawable.ic_system_72)
        }
        if (isSelected(context, TAG_ID_HID)) {
            ensureTagIcon(context, TAG_KEY_HID, R.drawable.ic_hidden_72)
        }
        if (isSelected(context, TAG_ID_SPL)) {
            ensureTagIcon(context, TAG_KEY_AAB, R.drawable.ic_aab_72)
        }
        if (isSelected(context, TAG_ID_AI)) {
            ensureTagIcon(context, TAG_KEY_AI, R.drawable.ic_ai_72)
        }
    }

    /**
     * prepare res for native lib tags
     */
    private fun ensureNativeLibs(context: Context, appInfo: ApiViewingApp) {
        val isAntiedFlu = isAntied(context, TAG_ID_FLU)
        val isAntiedRn = isAntied(context, TAG_ID_RN)
        val isAntiedXam = isAntied(context, TAG_ID_XAM)
        val isAntiedKot = isAntied(context, TAG_ID_KOT)
        val state64B = displayingTags.getNonNull(context, TAG_ID_64B)
        // anti any one requires further look-up to confirm
        val isAnyAntied = isAntiedFlu || isAntiedRn || isAntiedXam || isAntiedKot ||
                state64B.isAntiSelected || isAntied(context, TAG_ID_ARM) || isAntied(context, TAG_ID_X86)
        if (shouldShowTagCrossPlatform || shouldShowTagKotlin || state64B.isSelected || shouldShowTagNativeLib || isAnyAntied) {
            if (!appInfo.isNativeLibrariesRetrieved) {
                appInfo.retrieveNativeLibraries()
            }
        }
    }

    private fun ensureServices(context: Context, appInfo: ApiViewingApp) : PackageInfo? {
        val serviceTags = listOf(
                PackageTag.TAG_ID_FCM, PackageTag.TAG_ID_HWP, PackageTag.TAG_ID_MIP,
                PackageTag.TAG_ID_MZP, PackageTag.TAG_ID_OOP, PackageTag.TAG_ID_VVP,
                PackageTag.TAG_ID_J_P, PackageTag.TAG_ID_U_P, PackageTag.TAG_ID_TCP,
                PackageTag.TAG_ID_ALP, PackageTag.TAG_ID_DUP, PackageTag.TAG_ID_GTP
        )
        var doSkip = true
        serviceTags.forEach { tag ->
            val state = displayingTags.getNonNull(context, tag)
            doSkip = doSkip && state.isDeselected
        }
        if (doSkip) return null

        val pm = context.packageManager
        val flagGetDisabled = if (OsUtils.satisfy(OsUtils.N)) PackageManager.MATCH_DISABLED_COMPONENTS
        else flagGetDisabledLegacy
        val extraFlags = PackageManager.GET_SERVICES or PackageManager.GET_RECEIVERS or flagGetDisabled
        return try {
            if (appInfo.isArchive) pm.getPackageArchiveInfo(appInfo.appPackage.basePath, extraFlags)
            else pm.getPackageInfo(appInfo.packageName, extraFlags)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }?.apply {
            if (services == null) services = emptyArray()
        }
    }

    @Suppress("deprecation")
    private val flagGetDisabledLegacy = PackageManager.GET_DISABLED_COMPONENTS

    private fun inflateTag(context: Context, nameResId: Int, icon: Bitmap?, parent: ViewGroup) {
        // cache tag display name string
        inflateTag(context, tagId(context, nameResId), icon, parent)
    }

    private fun inflateTag(context: Context, name: String, icon: Bitmap?, parent: ViewGroup) {
        parent.forEach {
            if (it is LinearLayout && (it.tag as String) == name) return
        }
        val inflater = LayoutInflater.from(context)
        AdapterAvTagBinding.inflate(inflater, parent, true).apply {
            avAdapterInfoTag.tag = name
            avAdapterInfoTag.background.setTint(getTagColor(context))
            if (icon != null) {
                avAdapterInfoTagIcon.setImageBitmap(icon)
                avAdapterInfoTagName.visibility = View.GONE
            } else {
                avAdapterInfoTagName.text = name
                avAdapterInfoTagIcon.visibility = View.GONE
            }
        }
    }

    private fun ViewGroup.inflateTag(context: Context, nameResId: Int, icon: Bitmap? = null) {
        inflateTag(context, nameResId, icon, this)
    }

    private fun ViewGroup.inflateTag(context: Context, name: String, icon: Bitmap? = null) {
        inflateTag(context, name, icon, this)
    }

    private fun tagPackageInstaller(context: Context, installer: String?, container: ViewGroup) {
        installer ?: return
        val stateGp = displayingTags.getNonNull(context, TAG_ID_GP)
        val statePi = displayingTags.getNonNull(context, TAG_ID_PI)
        if (stateGp.isDeselected && statePi.isDeselected) return
        val isGp = installer == ApiViewingApp.packagePlayStore
        val showGp = isGp && (stateGp.isSelected || (!stateGp.isAntiSelected && statePi.isAntiSelected))
        val showPi = !isGp && (statePi.isSelected || (!statePi.isAntiSelected && stateGp.isAntiSelected))
        if (!showGp && !showPi) return
        val installerIcon = tagIcons[installer]
//        if (name != null) holder.inflateTag(name, installerIcon)
        if (installerIcon != null) {
            container.inflateTag(context, installer, installerIcon)
        }
    }

    private fun tagNativeLibs(context: Context, appInfo: ApiViewingApp, container: ViewGroup) {
        val isAntiedFlu = isAntied(context, TAG_ID_FLU)
        val isAntiedRn = isAntied(context, TAG_ID_RN)
        val isAntiedXam = isAntied(context, TAG_ID_XAM)
        val nls = appInfo.nativeLibraries
        if ((shouldShowTagCrossPlatformFlutter || (!isAntiedFlu && (isAntiedRn || isAntiedXam))) && nls[4]) {
            container.inflateTag(context, "Flutter", tagIcons[TAG_KEY_FLUTTER])
        }
        if ((shouldShowTagCrossPlatformReactNative || (!isAntiedRn && (isAntiedFlu || isAntiedXam))) && nls[5]) {
            container.inflateTag(context, "React Native", tagIcons[TAG_KEY_REACT_NATIVE])
        }
        if ((shouldShowTagCrossPlatformXamarin || (!isAntiedXam && (isAntiedRn || isAntiedFlu))) && nls[6]) {
            container.inflateTag(context, "Xamarin", tagIcons[TAG_KEY_Xamarin])
        }
        if (shouldShowTagKotlin && !isAntied(context, TAG_ID_KOT) && nls[7]) {
            container.inflateTag(context, "Kotlin", tagIcons[TAG_KEY_Kotlin])
        }
        val state64B = displayingTags.getNonNull(context, TAG_ID_64B)
        // todo remove anti check
        if (state64B.isSelected && !state64B.isAntiSelected) {
            val doInf = TagRelation.TAGS[TAG_ID_64B]?.toExpressible()?.setRes(context, appInfo)?.express()
            if (doInf == true) container.inflateTag(context, R.string.av_settings_tag_64b, tagIcons[TAG_KEY_64B])
        }
        if (shouldShowTagNativeLibArm && !isAntied(context, TAG_ID_ARM)) {
            if (nls[0]) container.inflateTag(context, "ARM")
            if (nls[1]) container.inflateTag(context, "ARM 64")
        }
        if (shouldShowTagNativeLibX86 && !isAntied(context, TAG_ID_X86)) {
            if (nls[2]) container.inflateTag(context, "x86")
            if (nls[3]) container.inflateTag(context, "x64")
        }
    }

    private fun tagServices(context: Context, checkerApp: TagCheckerApp, container: ViewGroup) {
        checkerApp.packageInfo ?: return
        val iconKeys = listOf(
                TAG_KEY_FCM,
                TAG_KEY_HUAWEI_PUSH,
                TAG_KEY_XIAOMI_PUSH,
                TAG_KEY_MEIZU_PUSH,
                TAG_KEY_OPPO_PUSH,
                TAG_KEY_VIVO_PUSH,
                TAG_KEY_JPUSH,
                TAG_KEY_UPUSH,
                TAG_KEY_TPNS,
                TAG_KEY_ALI_PUSH,
                TAG_KEY_BAIDU_PUSH,
                TAG_KEY_GETUI,
        )
        listOf(
                PackageTag.TAG_ID_FCM to "FCM",
                PackageTag.TAG_ID_HWP to "Huawei push",
                PackageTag.TAG_ID_MIP to "Xiaomi push",
                PackageTag.TAG_ID_MZP to "Meizu push",
                PackageTag.TAG_ID_OOP to "Oppo push",
                PackageTag.TAG_ID_VVP to "Vivo push",
                PackageTag.TAG_ID_J_P to "JPush",
                PackageTag.TAG_ID_U_P to "U-Push",
                PackageTag.TAG_ID_TCP to "TPNS",
                PackageTag.TAG_ID_ALP to "Alibaba Cloud push",
                PackageTag.TAG_ID_DUP to "Baidu push",
                PackageTag.TAG_ID_GTP to "Getui push"
        ).forEachIndexed { index, tagRes ->
            val (it, res) = tagRes
            val state = displayingTags.getNonNull(context, it)
            // todo remove anti check too?
            if (state.isSelected && !state.isAntiSelected) {
                val doInf = TagRelation.TAGS[it]?.toExpressible()?.setRes(context, checkerApp)?.express()
                if (doInf == true) container.inflateTag(context, res, tagIcons[iconKeys[index]])
            }
        }
    }

    private fun tagDirect(context: Context, appInfo: ApiViewingApp, container: ViewGroup, includeTagAi: Boolean) {
        if (isSelected(context, TAG_ID_SYS) && appInfo.apiUnit == ApiUnit.SYS) {
            container.inflateTag(context, R.string.av_adapter_tag_system, tagIcons[TAG_KEY_SYS])
        }
        if (isSelected(context, TAG_ID_HID) && !appInfo.isLaunchable) {
            container.inflateTag(context, R.string.av_adapter_tag_hidden, tagIcons[TAG_KEY_HID])
        }
        if (isSelected(context, TAG_ID_SPL) && appInfo.appPackage.hasSplits) {
            container.inflateTag(context, R.string.av_tag_has_splits, tagIcons[TAG_KEY_AAB])
        }
        if (includeTagAi) {
            tagAdaptiveIcon(context, appInfo, container)
        }
    }

    fun tagAdaptiveIcon(context: Context, appInfo: ApiViewingApp, container: ViewGroup) {
        if (isSelected(context, TAG_ID_AI) && appInfo.adaptiveIcon) {
            container.inflateTag(context, R.string.av_ai, tagIcons[TAG_KEY_AI])
        }
    }

    fun ensureResources(context: Context, appInfo: ApiViewingApp): TagCheckerApp {
        ensureTagIcons(context)
        ensureNativeLibs(context, appInfo)
        return TagCheckerApp(appInfo).apply {
            packageInfo = ensureServices(context, appInfo)
            installer = ensureInstaller(context, appInfo)
        }
    }

    // first inflate native lib tags then has splits tag and last ai tag
    fun inflateTags(context: Context, container: ViewGroup, checkerApp: TagCheckerApp, includeTagAi: Boolean) {
        tagPackageInstaller(context, checkerApp.installer, container)
        tagNativeLibs(context, checkerApp.app, container)
        tagServices(context, checkerApp, container)
        tagDirect(context, checkerApp.app, container, includeTagAi)
    }

    fun filterTags(context: Context, app: ApiViewingApp): Boolean {
        // filtering, resource loading and tag inflating are different cases
        // filtering: whether to be included in list. tag relationship should be considered.
        //     e.g. google play and package manager, cross-platform frameworks
        // resource loading: whether to load resource.
        //     e.g. anti any tag requires resource loading to confirm it
        // tag inflating: whether to inflate tag.
        //     any item can be included in list but without any tag inflated
        ensureNativeLibs(context, app)
        val packageInfo = ensureServices(context, app)
        val checkerApp = if (packageInfo != null) TagCheckerApp(app).apply {
            this.packageInfo = packageInfo
        } else null
        val serviceTags = if (packageInfo != null) listOf(
                PackageTag.TAG_ID_FCM, PackageTag.TAG_ID_HWP, PackageTag.TAG_ID_MIP,
                PackageTag.TAG_ID_MZP, PackageTag.TAG_ID_OOP, PackageTag.TAG_ID_VVP,
                PackageTag.TAG_ID_J_P, PackageTag.TAG_ID_U_P, PackageTag.TAG_ID_TCP,
                PackageTag.TAG_ID_ALP, PackageTag.TAG_ID_DUP, PackageTag.TAG_ID_GTP
        ) else emptyList()
        var re = false
        for (t in TagRelation.TAGS.values) {
            val getApp = { tagId: Int ->
                if (tagId in serviceTags && checkerApp != null) checkerApp else app
            }
            val exTag = loadExpressibleTag(t, context, getApp.invoke(t.id)) ?: continue
            var exTagOther: ExpressibleTag? = null
            if (t.relatives.size == 1) {
                val (tag2Id, relation) = t.relatives[0]
                // Check relative if it is complimentary and checking, no matter anti or not
                if (relation.isComplimentary) {
                    val tag2 = TagRelation.TAGS[tag2Id]
                    if (tag2 != null) exTagOther = loadExpressibleTag(tag2, context, getApp.invoke(tag2Id))
                }
            }
            re = exTag.express()
            if (exTagOther != null) re = when {
                // Satisfy both when both are anti
                exTag.isAnti && exTagOther.isAnti -> re && exTagOther.express()
                // Satisfy non-anti if either is anti
                exTag.isAnti || exTagOther.isAnti -> if (exTag.isAnti) exTagOther.express() else re
                // Satisfy either when both are non-anti
                !exTag.isAnti && !exTagOther.isAnti -> re || exTagOther.express()
                else -> re
            }
            if (re) break
        }
        return re
    }

    private fun loadExpressibleTag(tag: PackageTag, context: Context,
                                   app: ApiViewingApp): ExpressibleTag? {
        var expressibleTag: ExpressibleTag? = null
        val state = displayingTags.getNonNull(context, tag.id)
        if (!state.isDeselected) expressibleTag = tag.toExpressible().setRes(context, app)
        if (state.isAntiSelected) expressibleTag?.anti()
        return expressibleTag
    }

    /**
     * @return true if changed, false if not changed or null if lazy and changed
     */
    private fun Map<String, TriStateSelectable>.changed(context: Context, resId: Int, isLazy: Boolean): Boolean? {
        val newValue = get(context, resId)
        val oldValue = displayingTags.get(context, resId)
        if (newValue == null && oldValue != null) {
            displayingTagsPrivate.remove(tagId(context, resId))
        } else if (newValue != null) {
            displayingTagsPrivate.put(context, resId, newValue)
        }
        val isChanged = newValue != oldValue
        if (isLazy && isChanged) return null
        return isChanged
    }

    private fun loadTriStateTagSettings(context: Context, tagSettings: Map<String, TriStateSelectable>, isLazy: Boolean): Boolean {
        var isChanged = false
        PackageTag.TAG_IDS.forEach {
            isChanged = (tagSettings.changed(context, it, isLazy) ?: return true) || isChanged
        }
        return isChanged
    }

    fun loadTagSettings(context: Context, tagSettings: Map<String, TriStateSelectable>, isLazy: Boolean): Boolean {
        val isChanged = loadTriStateTagSettings(context, tagSettings, isLazy)
        shouldShowTagCrossPlatformFlutter = isSelected(context, TAG_ID_FLU)
        shouldShowTagCrossPlatformReactNative = isSelected(context, TAG_ID_RN)
        shouldShowTagCrossPlatformXamarin = isSelected(context, TAG_ID_XAM)
        shouldShowTagKotlin = isSelected(context, TAG_ID_KOT)
        shouldShowTagNativeLibArm = isSelected(context, TAG_ID_ARM)
        shouldShowTagNativeLibX86 = isSelected(context, TAG_ID_X86)
        return isChanged
    }

    fun loadTagSettings(context: Context, prefSettings: SharedPreferences, isLazy: Boolean): Boolean {
        val tagSettings = prefSettings.getStringSet(PrefUtil.AV_TAGS, HashSet())!!
        val triStateMap = tagSettings.associateWith {
            TriStateSelectable(it, true)
        }
        return loadTagSettings(context, triStateMap, isLazy)
    }
}
