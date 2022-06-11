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
import android.view.ViewGroup
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.tag.*
import com.madness.collision.unit.api_viewing.tag.app.AppTagInfo
import com.madness.collision.unit.api_viewing.tag.app.AppTagManager
import com.madness.collision.unit.api_viewing.tag.app.get
import com.madness.collision.unit.api_viewing.tag.app.toExpressible
import com.madness.collision.unit.api_viewing.tag.inflater.AppTagInflater
import com.madness.collision.unit.api_viewing.util.PrefUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

internal object AppTag {

    class TagStateMap(private val tags: Map<String, TriStateSelectable>) : Map<String, TriStateSelectable> by tags {
        override fun get(key: String): TriStateSelectable {
            return tags[key] ?: TriStateSelectable(key, TriStateSelectable.STATE_DESELECTED)
        }
    }

    private val displayingTagsPrivate = mutableMapOf<String, TriStateSelectable>()
    private val displayingTags: TagStateMap = TagStateMap(displayingTagsPrivate)

    fun clearCache() = AppTagInflater.clearCache()

    fun clearContext() = AppTagInflater.clearContext()

    fun ensureAllTagIcons(context: Context) {
        ensureTagIcons(context, null)
    }

    // prepare icons
    fun ensureTagIcons(context: Context) {
        ensureTagIcons(context) { displayingTags[it.id].isSelected }
    }

    // prepare icons
    private fun ensureTagIcons(context: Context, filter: ((AppTagInfo) -> Boolean)?) {
        for (id in AppTagInfo.IdGroup.STATIC_ICON) {
            val tagInfo = AppTagManager.tags[id] ?: continue
            if (filter != null && filter(tagInfo).not()) continue
            val iconKey = tagInfo.iconKey ?: continue
            val icon = tagInfo.icon
            when {
                icon.drawableResId != null -> AppTagInflater.ensureTagIcon(context, iconKey, icon.drawableResId)
                icon.drawable != null -> AppTagInflater.ensureTagIcon(context, iconKey, icon.drawable)
                icon.pkgName != null -> AppTagInflater.ensureTagIcon(context, icon.pkgName)
            }
        }
    }

    // todo some requisites should be checked only once when loading settings
    suspend fun ensureRequisites(context: Context, app: ApiViewingApp): AppTagInfo.Resources {
        val res = AppTagInfo.Resources(context, app)
        getGroupedRequisites().forEach { (p, tagIds) ->
            val (_, requisite) = p
            if (requisite.checker(res)) return@forEach // continue
            if (tagIds.all { displayingTags[it].isDeselected }) return@forEach // continue
            requisite.loader(res)
        }
        return res
    }

    // for all tags
    suspend fun ensureRequisitesForAllAsync(context: Context, app: ApiViewingApp, perRequisite: (
    suspend (requisite: AppTagInfo.Requisite, tagIds: List<String>, res: AppTagInfo.Resources) -> Unit
    )?): AppTagInfo.Resources {
        return ensureRequisitesAsync(context, app, perRequisite, null)
    }

    suspend fun ensureRequisitesAsync(context: Context, app: ApiViewingApp, perRequisite: (
    suspend (requisite: AppTagInfo.Requisite, tagIds: List<String>, res: AppTagInfo.Resources) -> Unit
    )?): AppTagInfo.Resources {
        return ensureRequisitesAsync(context, app, perRequisite) { tagIds ->
            tagIds.any { displayingTags[it].isDeselected.not() }
        }
    }

    /**
     * Resource loading: selected or anti-selected (anti any tag requires resource loading to confirm).
     *
     * If requisite is checked, include it in [perRequisite] but without duplicate loading,
     * Also when requisite tags are all deselected (not checking, i.e., not selected or anti-selected).
     */
    private suspend fun ensureRequisitesAsync(context: Context, app: ApiViewingApp, perRequisite: (
    suspend (requisite: AppTagInfo.Requisite, tagIds: List<String>, res: AppTagInfo.Resources) -> Unit
    )?, filter: ((tagIds: List<String>) -> Boolean)?): AppTagInfo.Resources = coroutineScope {
        val res = AppTagInfo.Resources(context, app)
        getGroupedRequisites().map { (p, tagIds) ->
            val (_, requisite) = p
            async(Dispatchers.Default) {
                // skip when requisite is checked
                val skip = requisite.checker(res) || (filter != null && filter(tagIds).not())
                if (skip.not()) requisite.loader(res)
                requisite to tagIds
            }
        }.forEach { deferred ->
            val (requisite, tagIds) = deferred.await()
            perRequisite?.invoke(requisite, tagIds, res)
        }
        res
    }

    // Direct tags that have no requisites are excluded
    private fun getGroupedRequisites(): List<Pair< Pair<String, AppTagInfo.Requisite>, List<String> >> {
        return AppTagManager.tags.mapNotNull { (tagId, tagInfo) ->
            val requisites = tagInfo.requisites ?: return@mapNotNull null
            requisites.map { requisite -> tagId to requisite }
        }.flatten().groupBy { (_, requisite) -> // group requisites of the same ID together
            requisite.id // this key is only used for grouping
        }.map { (reqId, tagIdList) ->
            // (requisiteId-requisite) - List{tagId}
            (reqId to tagIdList[0].second) to tagIdList.map { it.first }
        }
    }

    /**
     * [ensureTagIcons] and [ensureRequisites] need to be called beforehand.
     */
    fun inflateAllTags(context: Context, container: ViewGroup, res: AppTagInfo.Resources) {
        AppTagManager.tags.values.sortedBy { it.rank }.forEach { tagInfo ->
            inflateSingleTag(context, container, tagInfo, res)
        }
    }

    /**
     * [ensureTagIcons] and [ensureRequisites] are called within.
     */
    suspend fun inflateAllTagsAsync(context: Context, container: ViewGroup, app: ApiViewingApp) {
        ensureTagIcons(context)
        // inflate tags that do not have requisite first
        val directTagIds = AppTagManager.tags.mapNotNull {
            if (it.value.requisites == null) it.key else null
        }
        withContext(Dispatchers.Main) {
            inflateMultipleTags(context, container, directTagIds, AppTagInfo.Resources(context, app))
        }
        // ensure resources and inflate requisite tags
        val inflatedTagIds = directTagIds.toMutableList()
        val res = ensureRequisitesAsync(context, app) { _, tagIds, res ->
            inflatedTagIds.addAll(tagIds)
            withContext(Dispatchers.Main) {
                inflateMultipleTags(context, container, tagIds, res)
            }
        }
        // inflate any tag left (should be none)
        val leftTagIds = (AppTagManager.tags.keys - inflatedTagIds)
        if (leftTagIds.isNotEmpty()) withContext(Dispatchers.Main) {
            inflateMultipleTags(context, container, leftTagIds, res)
        }
    }

    fun inflateMultipleTags(context: Context, container: ViewGroup, tagIds: Collection<String>, res: AppTagInfo.Resources) {
        tagIds.forEach { id ->
            val tag = AppTagManager.tags[id] ?: return@forEach // continue
            inflateSingleTag(context, container, tag, res)
        }
    }

    // Tag inflating: selected and expressed true (no anti-ed tag icon support yet).
    fun inflateSingleTag(context: Context, container: ViewGroup, tagInfo: AppTagInfo, res: AppTagInfo.Resources) {
        // terminate if any requisite not satisfied
        if (tagInfo.requisites?.any { it.checker(res).not() } == true) return
        // selected and expressed true
        if (displayingTags[tagInfo.id].isSelected && tagInfo.express(res)) {
            val info = getTagViewInfo(tagInfo, res, context) ?: return
            AppTagInflater.inflateTag(context, container, info)
        }
    }

    private fun getTagViewInfo(tagInfo: AppTagInfo, res: AppTagInfo.Resources, context: Context): AppTagInflater.TagInfo? {
        return getTagViewInfo(tagInfo, res, context) { it.label.normal }
    }

    fun getTagViewInfo(tagInfo: AppTagInfo, res: AppTagInfo.Resources, context: Context, labelSelector: (AppTagInfo) -> AppTagInfo.Label?): AppTagInflater.TagInfo? {
        // normal label or dynamic label
        val label = labelSelector(tagInfo) ?: kotlin.run {
            val string = if (tagInfo.label.isDynamic)
                tagInfo.requisites?.firstNotNullOfOrNull { res.dynamicRequisiteLabels[it.id] } else null
            AppTagInfo.Label(string = string)
        }
        val tagIcon = tagInfo.icon
        // support dynamic icon
        val (iconBitmap, isExternalIcon) = when {
            tagIcon.drawableResId != null || tagIcon.drawable != null -> tagInfo.iconKey to false
            tagIcon.text != null -> null to false
            tagIcon.pkgName != null -> tagIcon.pkgName to true
            tagIcon.isDynamic -> tagInfo.requisites?.firstNotNullOfOrNull { res.dynamicRequisiteIconKeys[it.id] } to true
            else -> null to false
        }.let { (iconKey, isExternal) ->
            val ic = if (iconKey == null) null else AppTagInflater.tagIcons[iconKey]
            ic to isExternal
        }
        val icon = AppTagInflater.TagInfo.Icon(iconBitmap, tagIcon.text.get(context), isExternalIcon)
        // terminate if no label string available
        if (label.stringResId == null && label.string == null) return null
        // construct tag info object
        return AppTagInflater.TagInfo(nameResId = label.stringResId,
            name = label.string?.toString(), icon = icon, rank = tagInfo.rank)
    }

    // Filtering: selected matches expressing, or ExpressibleTag.express().
    suspend fun filterTags(context: Context, app: ApiViewingApp): Boolean {
        ensureTagIcons(context)
        val res = ensureRequisitesAsync(context, app, null)
        return AppTagManager.tags.any { it.value.express(res) }
    }

    private fun AppTagInfo.express(res: AppTagInfo.Resources): Boolean {
        val state = displayingTags[id]
        if (state.isDeselected) return false
        return toExpressible().setRes(res).apply { if (state.isAntiSelected) anti() }.express()
    }

    /**
     * @return true if changed, false if not changed or null if lazy and changed
     */
    private fun Map<String, TriStateSelectable>.changed(key: String, isLazy: Boolean): Boolean? {
        val newValue = this[key]
        val oldValue = displayingTags[key]
        if (newValue == null) {
            displayingTagsPrivate.remove(key)
        } else {
            displayingTagsPrivate[key] = newValue
        }
        val isChanged = newValue != oldValue
        if (isLazy && isChanged) return null
        return isChanged
    }

    private fun loadTriStateTagSettings(tagSettings: Map<String, TriStateSelectable>, isLazy: Boolean): Boolean {
        var isChanged = false
        AppTagInfo.IdGroup.BUILT_IN.forEach {
            isChanged = (tagSettings.changed(it, isLazy) ?: return true) || isChanged
        }
        return isChanged
    }

    fun loadTagSettings(context: Context, tagSettings: Map<String, TriStateSelectable>, isLazy: Boolean): Boolean {
        return loadTagSettings(tagSettings, isLazy)
    }

    fun loadTagSettings(tagSettings: Map<String, TriStateSelectable>, isLazy: Boolean): Boolean {
        return loadTriStateTagSettings(tagSettings, isLazy)
    }

    fun loadTagSettings(context: Context, prefSettings: SharedPreferences, isLazy: Boolean): Boolean {
        return loadTagSettings(prefSettings, isLazy)
    }

    fun loadTagSettings(prefSettings: SharedPreferences, isLazy: Boolean): Boolean {
        val tagSettings = prefSettings.getStringSet(PrefUtil.AV_TAGS, HashSet())!!
        val triStateMap = tagSettings.associateWith {
            TriStateSelectable(it, true)
        }
        return loadTagSettings(triStateMap, isLazy)
    }
}
