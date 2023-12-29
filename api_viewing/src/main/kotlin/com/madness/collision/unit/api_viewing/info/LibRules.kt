/*
 * Copyright 2023 Clifford Liu
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

package com.madness.collision.unit.api_viewing.info

import android.content.Context
import com.absinthe.rulesbundle.ACTIVITY
import com.absinthe.rulesbundle.DEX
import com.absinthe.rulesbundle.IconResMap
import com.absinthe.rulesbundle.LCRules
import com.absinthe.rulesbundle.NATIVE
import com.absinthe.rulesbundle.PROVIDER
import com.absinthe.rulesbundle.RECEIVER
import com.absinthe.rulesbundle.RuleEntity
import com.absinthe.rulesbundle.RuleRepository
import com.absinthe.rulesbundle.SERVICE
import com.madness.collision.chief.chiefContext
import com.madness.collision.unit.api_viewing.BuildConfig
import com.madness.collision.unit.api_viewing.R
import kotlin.concurrent.thread

private val PackCompType.libType
    get() = when (this) {
        PackCompType.Activity -> ACTIVITY
        PackCompType.Service -> SERVICE
        PackCompType.Receiver -> RECEIVER
        PackCompType.Provider -> PROVIDER
        PackCompType.DexPackage -> DEX
        PackCompType.NativeLibrary -> NATIVE
    }

object LibRules {
    /** The [LibCheckerRules][LCRules] version [LibRules] is compatible with. */
    private const val CompatVersion = "34.7"

    fun init(context: Context) {
        when (val v = BuildConfig.LIBCHECKER_RULES_VER) {
            CompatVersion -> LCRules.init(context.applicationContext)
            else -> thread { throw Exception("Incompatible LibRules impl with LibCheckerRules $v") }
        }
    }

    fun getLibMark(compName: String, compType: PackCompType): LibMark? {
        val rule = getRule(compName, compType) ?: return null
        val label = mapLocalizedLabelId(rule)?.let(chiefContext::getString) ?: rule.label
        val iconId = IconResMap.getIconRes(rule.iconIndex)
        val isIconMono = IconResMap.isSingleColorIcon(rule.iconIndex)
        return LibMarkImpl(label, iconId, isIconMono)
    }

    private fun getRule(compName: String, compType: PackCompType): RuleEntity? {
        RuleRepository.rules?.get(compName)?.let { return it }
        val rules = RuleRepository.regexRules ?: return null
        val libType = compType.libType
        for ((pattern, r) in rules) {
            if (r.type != libType) continue
            if (pattern.matcher(compName).matches()) return r
        }
        return null
    }

    private fun mapLocalizedLabelId(rule: RuleEntity) = when (rule.id) {
        102, 104 -> R.string.av_lib_rule_cpp_shared
        247, 297, 305, 307, 329, 362, 366, 556, 723 -> R.string.av_lib_rule_alipay
        else -> null
    }
}
