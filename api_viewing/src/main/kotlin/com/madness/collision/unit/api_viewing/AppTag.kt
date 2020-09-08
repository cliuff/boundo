/*
 * Copyright 2020 Clifford Liu
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
import androidx.core.view.forEach
import com.madness.collision.unit.api_viewing.data.ApiUnit
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.databinding.AdapterAvTagBinding
import com.madness.collision.unit.api_viewing.tag.ExpressibleTag
import com.madness.collision.unit.api_viewing.tag.PackageTag
import com.madness.collision.unit.api_viewing.tag.PackageTag.Companion.TAG_ID_GP
import com.madness.collision.unit.api_viewing.tag.PackageTag.Companion.TAG_ID_PI
import com.madness.collision.unit.api_viewing.tag.PackageTag.Companion.TAG_ID_FLU
import com.madness.collision.unit.api_viewing.tag.PackageTag.Companion.TAG_ID_RN
import com.madness.collision.unit.api_viewing.tag.PackageTag.Companion.TAG_ID_XAM
import com.madness.collision.unit.api_viewing.tag.PackageTag.Companion.TAG_ID_KOT
import com.madness.collision.unit.api_viewing.tag.PackageTag.Companion.TAG_ID_64B
import com.madness.collision.unit.api_viewing.tag.PackageTag.Companion.TAG_ID_ARM
import com.madness.collision.unit.api_viewing.tag.PackageTag.Companion.TAG_ID_X86
import com.madness.collision.unit.api_viewing.tag.PackageTag.Companion.TAG_ID_HID
import com.madness.collision.unit.api_viewing.tag.PackageTag.Companion.TAG_ID_SYS
import com.madness.collision.unit.api_viewing.tag.PackageTag.Companion.TAG_ID_SPL
import com.madness.collision.unit.api_viewing.tag.PackageTag.Companion.TAG_ID_AI
import com.madness.collision.unit.api_viewing.tag.TagRelation
import com.madness.collision.unit.api_viewing.tag.toExpressible
import com.madness.collision.unit.api_viewing.util.PrefUtil
import com.madness.collision.util.ThemeUtil
import com.madness.collision.util.X
import com.madness.collision.R as MainR
import kotlin.math.roundToInt

internal object AppTag {

    private const val TAG_KEY_FLUTTER = "flu"
    private const val TAG_KEY_REACT_NATIVE = "rn"
    private const val TAG_KEY_Xamarin = "xam"
    private const val TAG_KEY_Kotlin = "kot"

    private val tagIcons = mutableMapOf<String, Bitmap>()
    private var colorBackground: Int? = null
    private var colorItem: Int? = null
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
    private var shouldShowTagHidden = false
    private var shouldShowTagPrivilegeSystem = false
    private var shouldShowTagHasSplits = false
    private var shouldShowTagIconAdaptive = false

    fun clearCache() {
        tagIcons.clear()
    }

    fun clearContext() {
        colorBackground = null
        colorItem = null
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
        return if (APIAdapter.shouldShowDesserts) {
            colorBackground ?: ThemeUtil.getColor(context, MainR.attr.colorABackground).also {
                colorBackground = it
            }
        } else {
            colorItem ?: ThemeUtil.getColor(context, MainR.attr.colorAItem).also {
                colorItem = it
            }
        }
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
     * prepare res for native lib tags
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

    /**
     * prepare res for native lib tags
     */
    private fun ensureNativeLibs(context: Context, appInfo: ApiViewingApp) {
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

    private fun APIAdapter.Holder.inflateTag(context: Context, nameResId: Int, icon: Bitmap? = null) {
        inflateTag(context, nameResId, icon, this.tags)
    }

    private fun APIAdapter.Holder.inflateTag(context: Context, name: String, icon: Bitmap? = null) {
        inflateTag(context, name, icon, this.tags)
    }

    private fun tagPackageInstaller(context: Context, installer: String?, holder: APIAdapter.Holder) {
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
            holder.inflateTag(context, installer, installerIcon)
        }
    }

    private fun tagNativeLibs(context: Context, appInfo: ApiViewingApp, holder: APIAdapter.Holder) {
        val isAntiedFlu = isAntied(context, TAG_ID_FLU)
        val isAntiedRn = isAntied(context, TAG_ID_RN)
        val isAntiedXam = isAntied(context, TAG_ID_XAM)
        val nls = appInfo.nativeLibraries
        if ((shouldShowTagCrossPlatformFlutter || (!isAntiedFlu && (isAntiedRn || isAntiedXam))) && nls[4]) {
            holder.inflateTag(context, "Flutter", tagIcons[TAG_KEY_FLUTTER])
        }
        if ((shouldShowTagCrossPlatformReactNative || (!isAntiedRn && (isAntiedFlu || isAntiedXam))) && nls[5]) {
            holder.inflateTag(context, "React Native", tagIcons[TAG_KEY_REACT_NATIVE])
        }
        if ((shouldShowTagCrossPlatformXamarin || (!isAntiedXam && (isAntiedRn || isAntiedFlu))) && nls[6]) {
            holder.inflateTag(context, "Xamarin", tagIcons[TAG_KEY_Xamarin])
        }
        if (shouldShowTagKotlin && !isAntied(context, TAG_ID_KOT) && nls[7]) {
            holder.inflateTag(context, "Kotlin", tagIcons[TAG_KEY_Kotlin])
        }
        val state64B = displayingTags.getNonNull(context, TAG_ID_64B)
        // todo remove anti check
        if (state64B.isSelected && !state64B.isAntiSelected) {
            val doInf = TagRelation.TAGS[TAG_ID_64B]?.toExpressible()?.setRes(context, appInfo)?.express()
            if (doInf == true) holder.inflateTag(context, R.string.av_settings_tag_64b)
        }
        if (shouldShowTagNativeLibArm && !isAntied(context, TAG_ID_ARM)) {
            if (nls[0]) holder.inflateTag(context, "ARM")
            if (nls[1]) holder.inflateTag(context, "ARM 64")
        }
        if (shouldShowTagNativeLibX86 && !isAntied(context, TAG_ID_X86)) {
            if (nls[2]) holder.inflateTag(context, "x86")
            if (nls[3]) holder.inflateTag(context, "x64")
        }
    }

    private fun tagDirect(context: Context, appInfo: ApiViewingApp, holder: APIAdapter.Holder, includeTagAi: Boolean) {
        if (shouldShowTagHidden && !appInfo.isLaunchable) {
            holder.inflateTag(context, R.string.av_adapter_tag_hidden)
        }
        if (shouldShowTagPrivilegeSystem && appInfo.apiUnit == ApiUnit.SYS) {
            holder.inflateTag(context, R.string.av_adapter_tag_system)
        }
        if (shouldShowTagHasSplits && appInfo.appPackage.hasSplits) {
            holder.inflateTag(context, R.string.av_tag_has_splits)
        }
        if (includeTagAi) {
            tagAdaptiveIcon(context, appInfo, holder)
        }
    }

    fun tagAdaptiveIcon(context: Context, appInfo: ApiViewingApp, holder: APIAdapter.Holder) {
        if (shouldShowTagIconAdaptive && appInfo.adaptiveIcon) {
            holder.inflateTag(context, R.string.av_ai)
        }
    }

    fun ensureResources(context: Context, appInfo: ApiViewingApp): String? {
        ensureNativeLibs(context, appInfo)
        return ensureInstaller(context, appInfo)
    }

    // first inflate native lib tags then has splits tag and last ai tag
    fun inflateTags(context: Context, appInfo: ApiViewingApp, holder: APIAdapter.Holder, installer: String?, includeTagAi: Boolean) {
        tagPackageInstaller(context, installer, holder)
        tagNativeLibs(context, appInfo, holder)
        tagDirect(context, appInfo, holder, includeTagAi)
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
        var re = false
        for (t in TagRelation.TAGS.values) {
            val exTag = loadExpressibleTag(t, context, app) ?: continue
            var exTagOther: ExpressibleTag? = null
            if (t.relatives.size == 1) {
                val (tag2Id, relation) = t.relatives[0]
                // Check relative if it is complimentary and checking, no matter anti or not
                if (relation.isComplimentary) {
                    val tag2 = TagRelation.TAGS[tag2Id]
                    if (tag2 != null) exTagOther = loadExpressibleTag(tag2, context, app)
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
        shouldShowTagHidden = isSelected(context, TAG_ID_HID)
        shouldShowTagPrivilegeSystem = isSelected(context, TAG_ID_SYS)
        shouldShowTagHasSplits = isSelected(context, TAG_ID_SPL)
        shouldShowTagIconAdaptive = isSelected(context, TAG_ID_AI)
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
