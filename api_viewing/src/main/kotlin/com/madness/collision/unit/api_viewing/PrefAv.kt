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
import androidx.core.content.edit
import com.madness.collision.unit.api_viewing.tag.app.AppTagManager
import com.madness.collision.unit.api_viewing.tag.app.getFullLabel
import com.madness.collision.unit.api_viewing.util.PrefUtil
import com.madness.collision.util.P
import com.madness.collision.util.PopupUtil

        internal fun showTagsPrefPopup(context: Context) {
            val prefKeyTags = PrefUtil.AV_TAGS
            val pref = context.getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE)
            val prefValue = pref?.getStringSet(prefKeyTags, null) ?: emptySet()
            val tags = AppTagManager.tags
            val rankedTags = tags.values.sortedBy { it.rank }
            val checkedIndexes = prefValue.mapNotNullTo(mutableSetOf()) { id ->
                rankedTags.indexOfFirst { it.id == id }.takeIf { it >= 0 }
            }
            val filterTags = rankedTags.map { it.getFullLabel(context)?.toString() ?: "" }
            val tagIcons = rankedTags.map { it.icon.drawableResId }
            PopupUtil.selectMulti(context, R.string.av_settings_tags, filterTags, tagIcons, checkedIndexes) { pop, _, indexes ->
                pop.dismiss()
                val resultSet = indexes.mapTo(mutableSetOf()) { rankedTags[it].id }
                pref?.edit { putStringSet(prefKeyTags, resultSet) }
            }.show()
        }