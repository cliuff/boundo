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
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.tag.app.AppTagInfo
import com.madness.collision.unit.api_viewing.tag.app.AppTagManager
import com.madness.collision.unit.api_viewing.tag.app.get
import com.madness.collision.unit.api_viewing.tag.app.toExpressible
import com.madness.collision.unit.api_viewing.tag.inflater.AppTagInflater
import com.madness.collision.unit.api_viewing.util.PrefUtil
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal object AppTag {

    class TagStateMap(private val tags: Map<String, TriStateSelectable>) : Map<String, TriStateSelectable> by tags {
        override fun get(key: String): TriStateSelectable {
            return tags[key] ?: TriStateSelectable(key, TriStateSelectable.STATE_DESELECTED)
        }
    }

    private val displayingTagsPrivate = mutableMapOf<String, TriStateSelectable>()
    private val displayingTags: TagStateMap = TagStateMap(displayingTagsPrivate)
    private val tagReqMutex: MutableMap<String, Mutex> = hashMapOf()

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
    fun ensureTagIcons(context: Context, filter: ((AppTagInfo) -> Boolean)?) {
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
            val (reqId, requisite) = p
            if (requisite.checker(res)) return@forEach // continue
            if (tagIds.all { displayingTags[it].isDeselected }) return@forEach // continue
            val mutex = tagReqMutex[reqId] ?: Mutex()
            mutex.withLock {
                tagReqMutex[reqId] = mutex
                requisite.run { if (!checker(res)) loader(res) }
                tagReqMutex.remove(reqId, mutex)
            }
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
        val (checked, unchecked) = getGroupedRequisites().partition { (p, tagIds) ->
            p.second.checker(res) || (filter != null && filter(tagIds).not())
        }
        // use flow to order results by exec speed
        val uncheckedFlow = channelFlow {
        for ((p, tagIds) in unchecked) {
            val (reqId, requisite) = p
            launch(Dispatchers.Default) {
                val mutex = tagReqMutex[reqId] ?: Mutex()
                mutex.withLock {
                    tagReqMutex[reqId] = mutex
                    requisite.run { if (!checker(res)) loader(res) }
                    tagReqMutex.remove(reqId, mutex)
                }
                send(requisite to tagIds)
            }
        }
        }
        if (perRequisite != null) {
            checked.forEach { (p, tagIds) ->
                val (_, requisite) = p
                perRequisite(requisite, tagIds, res)
            }
            uncheckedFlow
                .onEach { (req, tagIds) -> perRequisite(req, tagIds, res) }
                .catch { it.printStackTrace() }
                .collect()
        } else {
            uncheckedFlow
                .catch { it.printStackTrace() }
                .collect()
        }
        res
    }

    // Direct tags that have no requisites are excluded
    private fun getGroupedRequisites(): List<Pair< Pair<String, AppTagInfo.Requisite>, List<String> >> {
        return AppTagManager.tags.flatMap m@{ (tagId, tagInfo) ->
            val requisites = tagInfo.requisites ?: return@m emptyList()
            requisites.map { requisite -> tagId to requisite }
        }.groupBy { (_, requisite) -> // group requisites of the same ID together
            requisite.id // this key is only used for grouping
        }.map { (reqId, tagIdList) ->
            // (requisiteId-requisite) - List{tagId}
            (reqId to tagIdList[0].second) to tagIdList.map { it.first }
        }
    }

    fun AppTagInfo.selExpressed(res: AppTagInfo.Resources): Boolean {
        return displayingTags[id].isSelected && express(res)
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
        val isChanged = newValue?.state != oldValue.state
        if (isLazy && isChanged) return null
        return isChanged
    }

    fun loadTagSettings(tagSettings: Map<String, TriStateSelectable>, isLazy: Boolean): Boolean {
        var isChanged = false
        AppTagInfo.IdGroup.BUILT_IN.forEach {
            isChanged = (tagSettings.changed(it, isLazy) ?: return true) || isChanged
        }
        return isChanged
    }

    fun loadTagSettings(prefSettings: SharedPreferences, isLazy: Boolean): Boolean {
        val tagSettings = prefSettings.getStringSet(PrefUtil.AV_TAGS, HashSet())!!
        val triStateMap = tagSettings.associateWith {
            TriStateSelectable(it, true)
        }
        return loadTagSettings(triStateMap, isLazy)
    }

    fun getTagSettings(): Map<String, TriStateSelectable> {
        return LinkedHashMap(displayingTags)
    }
}
