/*
 * Copyright 2022 Clifford Liu
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
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.text.format.Formatter
import com.madness.collision.unit.api_viewing.AppTag
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.data.AppPackage
import com.madness.collision.unit.api_viewing.list.AppListService
import com.madness.collision.unit.api_viewing.tag.app.AppTagInfo
import com.madness.collision.unit.api_viewing.tag.app.AppTagManager
import com.madness.collision.unit.api_viewing.tag.app.get
import com.madness.collision.unit.api_viewing.tag.app.toExpressible
import com.madness.collision.unit.api_viewing.tag.inflater.AppTagInflater
import com.madness.collision.util.os.OsUtils
import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.name

internal class ExpressedTag(
    val intrinsic: AppTagInfo,
    val label: String,
    val info: AppTagInflater.TagInfo,
    val desc: String?,
    val activated: Boolean,
)

internal object AppInfo {
    private fun getTagViewInfo(tag: AppTagInfo, res: AppTagInfo.Resources, context: Context) = run m@{
        if (tag.requisites?.all { it.checker(res) } == false) return@m null
        val (expVal, express) = tag.toExpressible().setRes(res).run { expressValueOrNull() to express() }
        val viewInfo = AppTag.getTagViewInfo(tag, res, context) {
            // use express value as tag label
            if (expVal != null) AppTagInfo.Label(string = expVal)
            else it.label.run { if (express) (full ?: normal) else normal }
        }
        viewInfo ?: return@m null
        val desc = tag.desc?.checkResultDesc?.let { chk -> chk(res).get(context)?.toString() }
        (tag to viewInfo) to (express to desc)
    }

    private fun getExpressedTags(tags: List<AppTagInfo>, res: AppTagInfo.Resources, context: Context)
            : List<ExpressedTag> {
        val rawList = tags.mapNotNull { getTagViewInfo(it, res, context) }
        val (list, expressList) = rawList.unzip()
        // g-play is subset of package installer
        val playIndex = list.indexOfFirst { it.first.id == AppTagInfo.ID_APP_INSTALLER_PLAY }
        val removeIndex = if (expressList.getOrNull(playIndex)?.first != true) playIndex
        else list.indexOfFirst { it.first.id == AppTagInfo.ID_APP_INSTALLER }
        val infoList = list.filterIndexed { i, _ -> i != removeIndex }
        val ex = expressList.filterIndexed { i, _ -> i != removeIndex }
        val listService = AppListService()
        val pkgRegex = """[\w.]+""".toRegex()
        return infoList.zip(ex) { tagInfo, express ->
            val info = tagInfo.second
            val activated = express.first
            val label = run {
                if (activated && info.name?.matches(pkgRegex) == true) {
                    return@run listService.getInstallerName(context, info.name)
                }
                info.name ?: info.nameResId?.let { context.getString(it) } ?: "Unknown"
            }
            ExpressedTag(tagInfo.first, label, tagInfo.second, express.second, express.first)
        }
    }

    suspend fun expressTags(app: ApiViewingApp, context: Context, updateValue: (List<ExpressedTag>) -> Unit) {
        val allTags = AppTagManager.tags.values
        val emptyRes = AppTagInfo.Resources(context, app)
        // init tags and icons that do not have requisites or are satisfied first
        val selTags = allTags.filter { i ->
            i.requisites ?: return@filter true
            i.requisites.all { it.checker(emptyRes) }
        }
        val selTagIdSet = selTags.mapTo(HashSet(selTags.size)) { it.id }
        AppTag.ensureTagIcons(context) { it.id in selTagIdSet }
        val update1 = getExpressedTags(selTags, emptyRes, context).sortedBy { it.intrinsic.rank }
        updateValue(update1)

        val tags = AppTagManager.tags
        AppTag.ensureAllTagIcons(context)
        var updates = update1
        AppTag.ensureRequisitesForAllAsync(context, app) { _, ids, res ->
            val reqTags = ids.mapNotNull { id -> tags[id]?.takeIf { it.id !in selTagIdSet } }
            updates = getExpressedTags(reqTags, res, context) + updates
            val updatedValue = updates.sortedBy { it.intrinsic.rank }
            updateValue(updatedValue)
        }
    }

    fun getApkSizeList(pkg: AppPackage, context: Context): List<Pair<String, String?>> {
        val baseName = if (OsUtils.satisfy(OsUtils.O)) Path(pkg.basePath).name else File(pkg.basePath).name
        val parentPath = pkg.basePath.replaceFirst(baseName, "")
        val sizes = pkg.apkPaths.map { path ->
            val fileName = path.replaceFirst(parentPath, "")
            val file = File(path).takeIf { it.exists() } ?: return@map fileName to null
            fileName to file.length()
        }
        val totalBytes = sizes.sumOf { it.second ?: 0 }.takeIf { it > 0 }
        val totalSize = totalBytes?.let { Formatter.formatFileSize(context, it) }
        val itemSizeList = sizes.map apkSize@{ (name, bytes) ->
            bytes ?: return@apkSize name to null
            name to Formatter.formatFileSize(context, bytes)
        }
        return buildList(itemSizeList.size + 1) {
            add("" to totalSize)
            addAll(itemSizeList)
        }
    }
}