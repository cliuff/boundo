/*
 * Copyright 2024 Clifford Liu
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

import android.graphics.Bitmap
import com.madness.collision.unit.api_viewing.tag.app.AppTagInfo
import com.madness.collision.unit.api_viewing.tag.app.get
import com.madness.collision.unit.api_viewing.tag.inflater.AppTagInflater

sealed interface ExpIcon {
    data class Res(val id: Int) : ExpIcon
    data class App(val packageName: String, val bitmap: Bitmap) : ExpIcon
    data class Text(val value: CharSequence) : ExpIcon
}

// ExpressedTag
interface ExpTag {
    val icon: ExpIcon
    val rank: String
}

data class CompactExpTag(
    override val icon: ExpIcon,
    override val rank: String,
) : ExpTag

internal data class FullExpTag(
    val label: AppTagInfo.Label,
    override val icon: ExpIcon,
    override val rank: String,
) : ExpTag


internal fun AppTagInfo.toCompactTag(res: AppTagInfo.Resources): CompactExpTag? {
    return CompactExpTag(icon = getIcon(res) ?: return null, rank = rank)
}

internal fun AppTagInfo.toFullTag(label: AppTagInfo.Label?, res: AppTagInfo.Resources): FullExpTag? {
    // normal label or dynamic label
    val labelOrDynamic = label ?: run label@{
        if (!this.label.isDynamic) return@label AppTagInfo.Label()
        val str = requisites?.firstNotNullOfOrNull { res.dynamicRequisiteLabels[it.id] }
        AppTagInfo.Label(string = str)
    }
    // terminate if no label string available
    if (labelOrDynamic.run { stringResId == null && string == null }) return null
    return FullExpTag(label = labelOrDynamic, icon = getIcon(res) ?: return null, rank = rank)
}

private fun AppTagInfo.getIcon(res: AppTagInfo.Resources): ExpIcon? {
    // support dynamic icon
    return when {
        icon.drawableResId != null /*|| icon.drawable != null*/ -> ExpIcon.Res(icon.drawableResId)
        icon.text != null -> ExpIcon.Text(icon.text.get(res.context) ?: "")
        icon.pkgName != null ->
            ExpIcon.App(icon.pkgName, AppTagInflater.tagIcons[icon.pkgName] ?: return null)
        icon.isDynamic -> requisites
            ?.firstNotNullOfOrNull { res.dynamicRequisiteIconKeys[it.id] }
            ?.let { k -> ExpIcon.App(k, AppTagInflater.tagIcons[k] ?: return null) }
        else -> null
    }
}
