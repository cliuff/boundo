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

package com.madness.collision.unit.api_viewing.tag

import android.content.Context
import com.madness.collision.unit.api_viewing.R
import com.madness.collision.unit.api_viewing.data.ApiViewingApp

/**
 * Immutable
 */
internal open class PackageTag(val id: Int, val expressing: (ExpressibleTag.(Context, ApiViewingApp) -> Boolean)? = null,
                          val relatives: List<Pair<Int, TagRelation>>): Expression {

    companion object {
        const val TAG_ID_GP = R.string.prefAvTagsValuePackageInstallerGp
        const val TAG_ID_PI = R.string.prefAvTagsValuePackageInstallerPi
        const val TAG_ID_FLU = R.string.prefAvTagsValueCrossPlatformFlu
        const val TAG_ID_RN = R.string.prefAvTagsValueCrossPlatformRn
        const val TAG_ID_XAM = R.string.prefAvTagsValueCrossPlatformXam
        const val TAG_ID_KOT = R.string.prefAvTagsValueKotlin
        const val TAG_ID_X_CMP = R.string.prefAvTagsValueJetpackCompose
        const val TAG_ID_64B = R.string.prefAvTagsValue64Bit
        const val TAG_ID_ARM = R.string.prefAvTagsValueNativeLibArm
        const val TAG_ID_X86 = R.string.prefAvTagsValueNativeLibX86
        const val TAG_ID_HID = R.string.prefAvTagsValueHidden
        const val TAG_ID_SYS = R.string.prefAvTagsValuePrivilegeSys
        const val TAG_ID_SPL = R.string.prefAvTagsValueHasSplits
        const val TAG_ID_AI = R.string.prefAvTagsValueIconAdaptive
        const val TAG_ID_FCM = R.string.prefAvTagsValuePushFcm
        const val TAG_ID_HWP = R.string.prefAvTagsValuePushHuawei
        const val TAG_ID_MIP = R.string.prefAvTagsValuePushMi
        const val TAG_ID_MZP = R.string.prefAvTagsValuePushMeizu
        const val TAG_ID_OOP = R.string.prefAvTagsValuePushOppo
        const val TAG_ID_VVP = R.string.prefAvTagsValuePushVivo
        const val TAG_ID_J_P = R.string.prefAvTagsValuePushJpush
        const val TAG_ID_U_P = R.string.prefAvTagsValuePushUpush
        const val TAG_ID_TCP = R.string.prefAvTagsValuePushTpns
        const val TAG_ID_ALP = R.string.prefAvTagsValuePushAli
        const val TAG_ID_DUP = R.string.prefAvTagsValuePushDu
        const val TAG_ID_GTP = R.string.prefAvTagsValuePushGetui

        val TAG_IDS = arrayOf(
            TAG_ID_GP, TAG_ID_PI, TAG_ID_FLU, TAG_ID_RN, TAG_ID_XAM,
            TAG_ID_KOT, TAG_ID_X_CMP, TAG_ID_64B, TAG_ID_ARM, TAG_ID_X86,
            TAG_ID_HID, TAG_ID_SYS, TAG_ID_SPL, TAG_ID_AI, TAG_ID_FCM,
            TAG_ID_HWP, TAG_ID_MIP, TAG_ID_MZP, TAG_ID_OOP, TAG_ID_VVP,
            TAG_ID_J_P, TAG_ID_U_P, TAG_ID_TCP, TAG_ID_ALP, TAG_ID_DUP,
            TAG_ID_GTP,
        )
    }

    constructor(id: Int, expressing: (ExpressibleTag.(Context, ApiViewingApp) -> Boolean)? = null,
                vararg relatives: Pair<Int, TagRelation>): this(id, expressing, relatives.toList())

    override fun express(): Boolean {
        return false
    }
}
