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

package com.madness.collision.unit.api_viewing.database.maintainer

import android.content.Context
import android.content.pm.PackageInfo
import android.util.Log
import androidx.core.content.edit
import com.madness.collision.unit.api_viewing.data.ApiUnit
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.data.toEntities
import com.madness.collision.unit.api_viewing.database.AppDao
import com.madness.collision.unit.api_viewing.database.AppRoom
import com.madness.collision.util.P
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.yield
import kotlin.random.Random
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds

object RecordMtn {
    fun shouldDiff(context: Context): Boolean {
        val prefs = context.getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE)
        val lastDiffTime = prefs.getLong(P.PACKAGE_CHANGED_DIFF_TIME, -1L)
        // not checked before
        if (lastDiffTime <= 0) return true
        val checkTimeMills = System.currentTimeMillis()
        val elapsedTime = (checkTimeMills - lastDiffTime).milliseconds
        // already checked within 3 days
        if (elapsedTime <= 3.days) return false
        // not checked for 7 days
        if (elapsedTime >= 7.days) return true
        // 1/10 probability
        return Random.nextInt(10) == 0
    }

    suspend fun diff(context: Context, packList: List<PackageInfo>, oldList: List<ApiViewingApp>): MaintainedDataDiff =
        supervisorScope diffData@{
            val diffTimeMills = System.currentTimeMillis()
            val dataDiff = detectDiff(context, packList, oldList)
            yield()  // cooperative

            val insertJob = launch(Dispatchers.IO) {
                val changes = mapDiffChanges(dataDiff)
                val dao = AppRoom.getDatabase(context).diffDao()
                // delete old empty records more than 60 days ago
                val oldMillis = diffTimeMills - 60.days.inWholeMilliseconds
                dao.deleteEmptyRecordsBy(oldMillis)
                dao.insertAll(changes)
            }
            val logJob = launch(Dispatchers.Default) { logDiff(dataDiff, oldList) }
            // wait for jobs to finish before updating diff time
            insertJob.join(); logJob.join()

            val prefs = context.getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE)
            prefs.edit { putLong(P.PACKAGE_CHANGED_DIFF_TIME, diffTimeMills) }
            dataDiff
        }

    fun apply(context: Context, dataDiff: MaintainedDataDiff, allPackages: List<PackageInfo>, dao: AppDao) {
        if (dataDiff.isEmpty) return
        dataDiff.removed.takeIf { it.isNotEmpty() }
            ?.let { dao.deletePackageNames(it) }
        val insertList = dataDiff.added + dataDiff.changed.map { it.pkgName }
        if (insertList.isNotEmpty()) {
            val pkgSet = allPackages.mapTo(LinkedHashSet(allPackages.size)) { it.packageName }
            val insertApps = insertList.mapNotNull inApp@{ pkg ->
                val index = pkgSet.indexOf(pkg)
                val pack = allPackages.getOrNull(index) ?: return@inApp null
                ApiViewingApp(context, pack, preloadProcess = true, archive = false)
            }
            dao.insert(insertApps.toEntities())
        }
    }
}

class RecordDiff(val columnName: String, val oldValue: String?, val newValue: String?)

class PackDiffChange(val pkgName: String, val diffs: List<RecordDiff>)

class MaintainedDataDiff(val added: List<String>, val removed: List<String>, val changed: List<PackDiffChange>) {
    val size: Int = added.size + removed.size + changed.size
    val isEmpty: Boolean get() = size <= 0
}

private fun detectDiff(context: Context, packList: List<PackageInfo>, oldList: List<ApiViewingApp>): MaintainedDataDiff {
    val oldPacks = oldList.mapTo(LinkedHashSet(oldList.size)) { it.packageName }
    val newPacks = packList.mapTo(LinkedHashSet(packList.size)) { it.packageName }
    val addedPacks = newPacks - oldPacks
    val removedPacks = oldPacks - newPacks
    // column names from data table
    val checkers = listOf(
        "targetAPI" to ApiViewingApp::targetAPI,
        "minAPI" to ApiViewingApp::minAPI,
        "verCode" to ApiViewingApp::verCode,
        "verName" to ApiViewingApp::verName,
        "updateTime" to ApiViewingApp::updateTime,
        "appPackage" to ApiViewingApp::appPackage,
    )
    val changedPacks = oldList.mapNotNull diff@{ oldApp ->
        val index = newPacks.indexOf(oldApp.packageName)
        val newPack = packList.getOrNull(index) ?: return@diff null
        if (newPack.packageName != oldApp.packageName) throw Exception("Pkg mismatch")
        val newApp = ApiViewingApp(context, newPack, preloadProcess = true, archive = false)
        val diffCols = checkers.mapNotNull { (col, f) ->
            val (oldVal, newVal) = f(oldApp) to f(newApp)
            if (oldVal == newVal) return@mapNotNull null
            RecordDiff(col, oldVal.toString(), newVal.toString())
        }
        if (diffCols.isEmpty()) return@diff null
        PackDiffChange(oldApp.packageName, diffCols)
    }
    return MaintainedDataDiff(addedPacks.toList(), removedPacks.toList(), changedPacks)
}

private fun mapDiffChanges(dataDiff: MaintainedDataDiff): List<DiffChange> {
    val diffSize = dataDiff.size
    val timeMills = System.currentTimeMillis()
    val diffId = "D01.$timeMills.$diffSize"
    if (dataDiff.isEmpty) {
        val diff = DiffInfo(diffId, timeMills, "")
        val change = DiffChange(diff = diff, type = DiffType.None, columnName = "", oldValue = "", newValue = "")
        return listOf(change)
    }
    return buildList(diffSize) {
        val addChanges = dataDiff.added.map { pkg ->
            val diff = DiffInfo(diffId, timeMills, pkg)
            DiffChange(diff = diff, type = DiffType.Add, columnName = "", oldValue = "", newValue = "")
        }
        val removeChanges = dataDiff.removed.map { pkg ->
            val diff = DiffInfo(diffId, timeMills, pkg)
            DiffChange(diff = diff, type = DiffType.Remove, columnName = "", oldValue = "", newValue = "")
        }
        val columnChanges = dataDiff.changed.map { diffChange ->
            val diff = DiffInfo(diffId, timeMills, diffChange.pkgName)
            diffChange.diffs.map { change ->
                DiffChange(
                    diff = diff,
                    type = DiffType.Change,
                    columnName = change.columnName,
                    oldValue = change.oldValue.orEmpty(),
                    newValue = change.newValue.orEmpty(),
                )
            }
        }
        addAll(addChanges)
        addAll(removeChanges)
        addAll(columnChanges.flatten())
    }
}

private fun logDiff(dataDiff: MaintainedDataDiff, oldList: List<ApiViewingApp>) {
    if (dataDiff.isEmpty) {
        Log.d("av.data.mtn", "No diff")
        return
    }
    val (addedPacks, removedPacks, changedPacks) =
        Triple(dataDiff.added, dataDiff.removed, dataDiff.changed)
    val msg = buildString {
        if (addedPacks.isNotEmpty()) {
            append("\nAdded ${addedPacks.size}:\n")
            append(addedPacks.joinToString("\n"))
        }
        if (removedPacks.isNotEmpty()) {
            append("\n\nRemoved ${removedPacks.size}:\n")
            append(removedPacks.joinToString("\n"))
        }
        if (changedPacks.isNotEmpty()) {
            append("\n\nChanged ${changedPacks.size}:\n")
            val oldPacks = oldList.mapTo(LinkedHashSet(oldList.size)) { it.packageName }
            val changed = changedPacks.joinToString("\n") { diffChange ->
                val pkg = diffChange.pkgName
                val appIndex = oldPacks.indexOf(pkg)
                val app = oldList.getOrNull(appIndex)
                val isSys = app?.run { if (apiUnit == ApiUnit.SYS) "SYS" else "USR" } ?: "N/A"
                diffChange.diffs.joinToString(prefix = "$isSys $pkg:\n\t") { diff ->
                    "${diff.columnName}:${diff.oldValue}->${diff.newValue}"
                }
            }
            append(changed)
        }
    }
    Log.d("av.data.mtn", msg)
}
