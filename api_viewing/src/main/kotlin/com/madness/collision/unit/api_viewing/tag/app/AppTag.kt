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
import android.graphics.drawable.Drawable
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.tag.Expression
import com.madness.collision.util.ui.appContext

internal class AppTagInfo(
    val id: String,
    val icon: Icon,
    val label: Labels,
    val category: Category,
    val desc: Description? = null,
    // digits only (0-9), follow comparison rules of strings
    val rank: String = RANK_UNSPECIFIED,
    val availability: Availability = Available,
    // requisites will be prepared if isAvailable is true
    val requisites: List<Requisite>? = null,
    val valueExpressing: ValueExpressing? = null,
    @Deprecated("") val expressing: Expressing,
) : Expression {
    var iconKey: String? = null

    override fun express(): Boolean {
        return false
    }

    class Icon(
        // No.1
        val drawableResId: Int? = null,
        // No.2
        val drawable: Drawable? = null,
        // No.3
        val text: Label? = null,
        // No.4 app package name to get runtime app icon
        val pkgName: String? = null,
        // use dynamic icon key
        val isDynamic: Boolean = false,
    )

    class Label(
        // No.1
        val stringResId: Int? = null,
        // No.2
        val string: CharSequence? = null,
    )

    class Labels(
        val normal: Label? = null,
        val full: Label? = null,
        val condensed: Label? = null,
        // dynamic normal label, full label is mandatory
        val isDynamic: Boolean = false,
    )

    class Category

    class Description(
        val tagDesc: Label? = null,
        val checkResultDesc: ((Resources) -> Label)? = null,
    )

    class Resources(
        val context: Context,
        val app: ApiViewingApp,
        // requisite ID as key, values are requisite (rather than tag) bound
        val dynamicRequisiteIconKeys: MutableMap<String, String> = hashMapOf(),
        val dynamicRequisiteLabels: MutableMap<String, String> = hashMapOf(),
    )

    fun interface Availability : (Context) -> Boolean

    object Available : Availability {
        override fun invoke(p1: Context): Boolean = true
    }

    class Requisite(
        val id: String,
        val checker: (Resources) -> Boolean,
        val loader: suspend (Resources) -> Unit,
    )

    @Deprecated("Use ValueExpressing instead to express non-binary values.")
    fun interface Expressing : (ExpressibleTag, Resources) -> Boolean

    fun interface ValueExpressing : (ExpressibleTag, Resources) -> String?

    companion object {
        const val RANK_UNSPECIFIED = "\u0000"  // NULL

        const val ID_APP_INSTALLER_PLAY = "avTagsValPiGp"
        const val ID_APP_INSTALLER = "avTagsValPiPi"
        const val ID_TECH_FLUTTER = "avTagsValCpFlu"
        const val ID_TECH_REACT_NATIVE = "avTagsValCpRn"
        const val ID_TECH_XAMARIN = "avTagsValCpXar"
        const val ID_TECH_KOTLIN = "avTagsValKot"
        const val ID_TECH_X_COMPOSE = "avTagsValXCmp"
        const val ID_PKG_64BIT = "avTagsVal64b"
        const val ID_PKG_ARM32 = "avTagsValPkgArm32"
        const val ID_PKG_ARM64 = "avTagsValPkgArm64"
        const val ID_PKG_X86 = "avTagsValPkgX86"
        const val ID_PKG_X64 = "avTagsValPkgX64"
        const val ID_APP_HIDDEN = "avTagsValueHidden"
        const val ID_APP_SYSTEM = "avTagsValPriSys"
        const val ID_APP_SYSTEM_CORE = "avTagsValSysCore"
        const val ID_APP_SYSTEM_MODULE = "avTagsValSysMod"
        const val ID_APP_CATEGORY = "avTagsValCat"
        const val ID_TYPE_OVERLAY = "avTagsValTypeOverlay"
        const val ID_TYPE_INSTANT = "avTagsValTypeInstant"
        const val ID_TYPE_WEB_APK = "avTagsValTypeWebApk"
        const val ID_PKG_AAB = "avTagsValHasSplits"
        const val ID_APP_ADAPTIVE_ICON = "avTagsValIconAda"
        const val ID_APP_PREDICTIVE_BACK = "avTagsValPreBack"
        const val ID_MSG_FCM = "avTagsValPushFcm"
        const val ID_MSG_HUAWEI = "avTagsValPushHw"
        const val ID_MSG_XIAOMI = "avTagsValPushMi"
        const val ID_MSG_MEIZU = "avTagsValPushMz"
        const val ID_MSG_OPPO = "avTagsValPushOo"
        const val ID_MSG_VIVO = "avTagsValPushVv"
        const val ID_MSG_JPUSH = "avTagsValPushJp"
        const val ID_MSG_UPUSH = "avTagsValPushUp"
        const val ID_MSG_TPNS = "avTagsValPushTp"
        const val ID_MSG_ALI = "avTagsValPushAli"
        const val ID_MSG_BAIDU = "avTagsValPushDu"
        const val ID_MSG_GETUI = "avTagsValPushGt"
    }

    object IdGroup {
        val BUILT_IN: List<String> = listOf(
            ID_APP_INSTALLER_PLAY,
            ID_APP_INSTALLER,
            ID_TECH_FLUTTER,
            ID_TECH_REACT_NATIVE,
            ID_TECH_XAMARIN,
            ID_TECH_KOTLIN,
            ID_TECH_X_COMPOSE,
            ID_PKG_64BIT,
            ID_PKG_ARM32,
            ID_PKG_ARM64,
            ID_PKG_X86,
            ID_PKG_X64,
            ID_APP_HIDDEN,
            ID_APP_SYSTEM,
            ID_APP_SYSTEM_CORE,
            ID_APP_SYSTEM_MODULE,
            ID_APP_CATEGORY,
            ID_TYPE_OVERLAY,
            ID_TYPE_INSTANT,
            ID_TYPE_WEB_APK,
            ID_PKG_AAB,
            ID_APP_ADAPTIVE_ICON,
            ID_APP_PREDICTIVE_BACK,
            ID_MSG_FCM,
            ID_MSG_HUAWEI,
            ID_MSG_XIAOMI,
            ID_MSG_MEIZU,
            ID_MSG_OPPO,
            ID_MSG_VIVO,
            ID_MSG_JPUSH,
            ID_MSG_UPUSH,
            ID_MSG_TPNS,
            ID_MSG_ALI,
            ID_MSG_BAIDU,
            ID_MSG_GETUI,
        )
        val STATIC_ICON: List<String> = listOf(
            ID_TECH_FLUTTER,
            ID_TECH_REACT_NATIVE,
            ID_TECH_XAMARIN,
            ID_TECH_KOTLIN,
            ID_TECH_X_COMPOSE,
            ID_PKG_64BIT,
            ID_APP_HIDDEN,
            ID_APP_SYSTEM,
            ID_TYPE_WEB_APK,
            ID_PKG_AAB,
            ID_APP_ADAPTIVE_ICON,
            ID_MSG_FCM,
            ID_MSG_HUAWEI,
            ID_MSG_XIAOMI,
            ID_MSG_MEIZU,
            ID_MSG_OPPO,
            ID_MSG_VIVO,
            ID_MSG_JPUSH,
            ID_MSG_UPUSH,
            ID_MSG_TPNS,
            ID_MSG_ALI,
            ID_MSG_BAIDU,
            ID_MSG_GETUI,
        )
    }
}

internal fun AppTagInfo.Label?.get(context: Context): CharSequence? = when {
    this == null -> null
    stringResId != null -> context.getString(stringResId)
    string != null -> string.toString()
    else -> null
}

internal fun AppTagInfo.Labels.getFullLabel(context: Context) = (full ?: normal).get(context)

internal fun AppTagInfo.Labels.getNormalLabel(context: Context) = (normal ?: full).get(context)

internal fun AppTagInfo.getFullLabel(context: Context) = label.getFullLabel(context)

internal fun AppTagInfo.getNormalLabel(context: Context) = label.getNormalLabel(context)

internal fun AppTagInfo.isAvailable(context: Context) = availability(context)

internal class ExpressibleTag(private val tagInfo: AppTagInfo, var isAnti: Boolean = false) : Expression {
    var res: AppTagInfo.Resources? = null

    fun expressValueOrNull(): String? {
        val res = res ?: return null
        val exp = tagInfo.valueExpressing ?: return null
        return exp(this, res)
    }

    override fun express(): Boolean {
        val res = res ?: return false
        val expressed = tagInfo.expressing(this, res)
        return if (isAnti) expressed.not() else expressed
    }

    fun setRes(res: AppTagInfo.Resources): ExpressibleTag {
        this.res = res
        return this
    }

    fun anti(): ExpressibleTag {
        isAnti = true
        return this
    }
}

internal fun AppTagInfo.toExpressible(): ExpressibleTag {
    return ExpressibleTag(this)
}

internal object AppTagManager {
    val tags: Map<String, AppTagInfo> by lazy {
        val context = appContext
        builtInTags().filterValues { it.isAvailable(context) }
    }
}
