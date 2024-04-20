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

package com.madness.collision.unit.api_viewing.ui.list

import android.net.Uri
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.util.StringUtils
import java.util.TreeSet

enum class AppListOrder(val code: Int) {
    LowerApi(0),
    HigherApi(1),
    AppName(2),
    UpdateTime(3),
}

enum class AppApiMode { Compile, Target, Minimum }

fun AppListOrderOrDefault(code: Int) = when (code) {
    0 -> AppListOrder.LowerApi
    1 -> AppListOrder.HigherApi
    2 -> AppListOrder.AppName
    3 -> AppListOrder.UpdateTime
    else -> AppListOrder.UpdateTime
}

fun AppListOrder.getComparator(apiMode: AppApiMode): Comparator<ApiViewingApp> {
    fun selectApi() = when (apiMode) {
        AppApiMode.Compile -> ApiViewingApp::compileAPI
        AppApiMode.Target -> ApiViewingApp::targetAPI
        AppApiMode.Minimum -> ApiViewingApp::minAPI
    }
    return when (this) {
        AppListOrder.LowerApi -> compareBy(selectApi())
        AppListOrder.HigherApi -> compareBy(selectApi()).reversed()
        AppListOrder.UpdateTime -> compareBy(ApiViewingApp::updateTime).reversed()
        AppListOrder.AppName -> return compareBy(StringUtils.comparator, ApiViewingApp::name)
    }.thenBy(StringUtils.comparator, ApiViewingApp::name)
}


typealias ListSrcKey<T> = AppListSrc.Key<T>

sealed interface AppListSrc {
    /** identify instance by key */
    val key: Key<out AppListSrc>

    sealed interface Key<Src : AppListSrc>

    object SystemApps : AppListSrc, ListSrcKey<SystemApps> {
        override val key: ListSrcKey<SystemApps> = this
    }
    object UserApps : AppListSrc, ListSrcKey<UserApps> {
        override val key: ListSrcKey<UserApps> = this
    }
    object DeviceApks : AppListSrc, ListSrcKey<DeviceApks> {
        override val key: ListSrcKey<DeviceApks> = this
    }

    class SelectApks(val uriList: List<Uri>) : AppListSrc {
        companion object Key : ListSrcKey<SelectApks>
        override val key: ListSrcKey<SelectApks> = Key
    }
    class SelectVolume(val uri: Uri?) : AppListSrc {
        companion object Key : ListSrcKey<SelectVolume>
        override val key: ListSrcKey<SelectVolume> = Key
    }
    object DragAndDrop : AppListSrc, ListSrcKey<DragAndDrop> {
        override val key: ListSrcKey<DragAndDrop> = this
    }

    class TagFilter(val targetCat: ListSrcCat, val checkedTags: Map<String, Boolean>) : AppListSrc {
        companion object Key : ListSrcKey<TagFilter>
        override val key: ListSrcKey<TagFilter> = Key
    }
    class DataSourceQuery(val value: String) : AppListSrc {
        companion object Key : ListSrcKey<DataSourceQuery>
        override val key: ListSrcKey<DataSourceQuery> = Key
    }
}


/** ListSrcCategory */
enum class ListSrcCat { Platform, Storage, Temporary, Filter }

val AppListSrc.cat: ListSrcCat
    get() = key.cat
val ListSrcKey<*>.cat: ListSrcCat
    get() = when (this) {
        AppListSrc.SystemApps -> ListSrcCat.Platform
        AppListSrc.UserApps -> ListSrcCat.Platform
        AppListSrc.DeviceApks -> ListSrcCat.Storage
        AppListSrc.SelectApks -> ListSrcCat.Temporary
        AppListSrc.SelectVolume -> ListSrcCat.Temporary
        AppListSrc.DragAndDrop -> ListSrcCat.Temporary
        AppListSrc.TagFilter -> ListSrcCat.Filter
        AppListSrc.DataSourceQuery -> ListSrcCat.Filter
    }
val ListSrcKeys: List<ListSrcKey<*>>
    get() = listOf(
        AppListSrc.SystemApps,
        AppListSrc.UserApps,
        AppListSrc.DeviceApks,
        AppListSrc.SelectApks,
        AppListSrc.SelectVolume,
        AppListSrc.DragAndDrop,
        AppListSrc.TagFilter,
        AppListSrc.DataSourceQuery,
    )

class OrderedAppList(private var options: Options) {
    private val srcSet: MutableSet<AppListSrc> = hashSetOf()
    private val srcMap: MutableMap<AppListSrc, Set<ApiViewingApp>> = hashMapOf()
    private var treeSet: TreeSet<ApiViewingApp> = TreeSet(options.order.getComparator(options.apiMode))

    class Options(val order: AppListOrder, val apiMode: AppApiMode)

    constructor(order: AppListOrder, apiMode: AppApiMode) : this(Options(order, apiMode))

    fun setOptions(order: AppListOrder = options.order, apiMode: AppApiMode = options.apiMode): OrderedAppList {
        val isChanged = (order != options.order) || when (order) {
            // reorder list by different API mode
            AppListOrder.LowerApi, AppListOrder.HigherApi -> apiMode != options.apiMode
            // list ordering not affected by API mode change
            AppListOrder.AppName, AppListOrder.UpdateTime -> false
        }
        options = Options(order, apiMode)
        if (isChanged) treeSet = TreeSet(order.getComparator(apiMode)).apply { addAll(treeSet) }
        return this
    }

    private fun srcList(src: AppListSrc): Set<ApiViewingApp> {
        return srcMap[src].orEmpty()
    }

    fun addItem(src: AppListSrc, item: ApiViewingApp) {
        treeSet.add(item)
        srcSet.add(src)
        srcMap[src] = srcList(src) + item
    }

    fun addAllItems(src: AppListSrc, items: Collection<ApiViewingApp>) {
        treeSet.addAll(items)
        srcSet.add(src)
        srcMap[src] = srcList(src) + items
    }

    /** remove the src matching one single AppListSrc instance */
    fun removeAppSrc(src: AppListSrc) {
        if (src !in srcSet) return
        treeSet.removeAll(srcList(src))
        srcSet.remove(src)
        srcMap.remove(src)
    }

    /** remove all src matching any AppListSrc instances of the same ListSrcKey */
    fun removeAppSrc(srcKey: ListSrcKey<*>) {
        srcSet.filter { it.key == srcKey }.forEach(::removeAppSrc)
    }

    fun clearAll() {
        treeSet.clear()
        srcSet.clear()
        srcMap.clear()
    }

    fun containsSrc(srcKey: ListSrcKey<*>): Boolean {
        return srcSet.any { it.key == srcKey }
    }

    val size: Int get() = treeSet.size
    fun isNotEmpty(): Boolean = size > 0
    fun getList(): List<ApiViewingApp> = treeSet.toList()
}

class MultiSrcApps(order: AppListOrder, apiMode: AppApiMode) {
    private val srcAppsMap = ListSrcCat.entries.run {
        associateWithTo(HashMap(size)) { OrderedAppList(order, apiMode) }
    }

    operator fun get(key: ListSrcCat): OrderedAppList = srcAppsMap.getValue(key)

//    fun setAppList(key: ListSrcCat, list: Collection<ApiViewingApp>, options: OrderedAppList.Options) {
//        srcAppsMap[key] = OrderedAppList(options)
//    }

    fun clearAll() {
        srcAppsMap.values.forEach(OrderedAppList::clearAll)
    }
}
