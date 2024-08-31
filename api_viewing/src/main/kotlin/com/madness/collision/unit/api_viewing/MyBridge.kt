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
import android.content.pm.PackageInfo
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import androidx.core.content.edit
import androidx.core.database.getFloatOrNull
import androidx.core.database.getIntOrNull
import androidx.core.database.getStringOrNull
import androidx.fragment.app.Fragment
import androidx.sqlite.db.SupportSQLiteQueryBuilder
import com.madness.collision.chief.chiefContext
import com.madness.collision.misc.MiscApp
import com.madness.collision.unit.Bridge
import com.madness.collision.unit.Unit
import com.madness.collision.unit.UpdatesProvider
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.database.AppRoom
import com.madness.collision.unit.api_viewing.info.LibRules
import com.madness.collision.unit.api_viewing.tag.app.AppTagInfo
import com.madness.collision.unit.api_viewing.ui.home.AppHomeFragment
import com.madness.collision.unit.api_viewing.util.ApkRetriever
import com.madness.collision.unit.api_viewing.util.PrefUtil
import com.madness.collision.util.P
import com.madness.collision.util.Page
import kotlin.reflect.KClass
import com.madness.collision.R as MainR

object MyBridge: Bridge() {

    override val unitName: String = Unit.UNIT_NAME_API_VIEWING
    override val args: List<KClass<*>> = listOf(Bundle::class)

    /**
     * @param args extras: [Bundle]?
     */
    override fun getUnitInstance(vararg args: Any?): Unit {
        return MyUnit().apply { arguments = args[0] as Bundle? }
    }

    override fun getSettings(): Fragment {
        return Page<PrefAv>(MainR.string.apiViewer)
    }

    override fun getAccessor(): Any {
        return ApiViewingAccessorImpl()
    }
}

class ApiViewingAccessorImpl : ApiViewingAccessor {
    override fun initUnit(context: Context) {
        LibRules.init(context)
    }

    override fun clearTags() {
        AppTag.clearCache()
    }

    override fun clearContext() {
        AppTag.clearContext()
    }

    /**
     * Add only the selected ones because adding all the tags will harm performance
     * Users will have a glimpse of available features in Filter by Tag
     * Consequently tag settings alone does not serve that purpose any more
     */
    override fun initTagSettings(pref: SharedPreferences) {
        val tagSettings = setOf(
            AppTagInfo.ID_APP_INSTALLER_PLAY,
            AppTagInfo.ID_APP_INSTALLER,
            AppTagInfo.ID_TECH_FLUTTER,
            AppTagInfo.ID_TECH_REACT_NATIVE,
            AppTagInfo.ID_TECH_XAMARIN,
            AppTagInfo.ID_APP_SYSTEM_MODULE,
            AppTagInfo.ID_TYPE_OVERLAY,
        )
        pref.edit {
            putStringSet(PrefUtil.AV_TAGS, tagSettings)
        }
    }

    override fun addModOverlayTags() {
        val prefSettings = chiefContext.getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE)
        val tags = prefSettings.getStringSet(PrefUtil.AV_TAGS, null)?.toHashSet() ?: return
        tags.addAll(listOf(AppTagInfo.ID_APP_SYSTEM_MODULE, AppTagInfo.ID_TYPE_OVERLAY))
        prefSettings.edit { putStringSet(PrefUtil.AV_TAGS, tags) }
    }

    override fun resolveUri(context: Context, uri: Uri): PackageInfo? {
        val retriever = ApkRetriever(context)
        val file = retriever.toFile(uri) ?: return null
        return MiscApp.getPackageInfo(context, apkPath = file.path)
    }

    override fun clearRoom(context: Context) {
        AppRoom.getDatabase(context).clearAllTables()
    }

    override fun getRoomInfo(context: Context): String {
        val helper = AppRoom.getDatabase(context).openHelper
        val iName = helper.databaseName ?: "Unknown Database"
        helper.readableDatabase.run {
            val peakQuery = SupportSQLiteQueryBuilder.builder(iName)
                .orderBy(ApiViewingApp::updateTime.name + " DESC").limit("5").create()
            val query = query(peakQuery)
            val columns = query.columnNames.joinToString()
            val records = query.readAll().joinToString(separator = "\n\n")
            val iIntegrity = if (isDatabaseIntegrityOk) "OK" else "not OK"
            val title = "$iName v$version (integrity $iIntegrity)"
            val iDbs = attachedDbs?.joinToString { "${it.first}(${it.second})" } ?: "none"
            return "$title\nPath: $path\nDatabases: $iDbs\nColumns: $columns\n\n$records"
        }
    }

    private fun Cursor.readAll(): List<String> = readDatabase(this)

    private fun readDatabase(cursor: Cursor): List<String> {
        val dataRows: MutableList<String> = ArrayList(cursor.count)
        val columns = cursor.columnNames
        val colIndexes = columns.map { cursor.getColumnIndex(it) }
        while (cursor.moveToNext()) {
            columns.mapIndexed { i, col ->
                val index = colIndexes[i]
                when (cursor.getType(index)) {
                    Cursor.FIELD_TYPE_STRING -> cursor.getStringOrNull(index)
                    Cursor.FIELD_TYPE_INTEGER -> cursor.getIntOrNull(index).toString()
                    Cursor.FIELD_TYPE_FLOAT -> cursor.getFloatOrNull(index).toString()
                    else -> "?"
                }.let { "$col: $it" }
            }.joinToString().let { dataRows.add(it) }
        }
        return dataRows
    }

    override fun nukeAppRoom(context: Context): Boolean {
        val room = AppRoom.getDatabase(context)
        val name = room.openHelper.databaseName ?: return false
        AppRoom.clearInstance()
        room.close()
        return context.deleteDatabase(name)
    }

    override fun getHomeFragment(): Fragment {
        return AppHomeFragment()
    }
}
