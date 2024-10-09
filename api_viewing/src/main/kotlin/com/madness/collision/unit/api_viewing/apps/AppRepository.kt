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

package com.madness.collision.unit.api_viewing.apps

import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.coroutineScope
import com.madness.collision.chief.chiefContext
import com.madness.collision.unit.api_viewing.data.ApiUnit
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.database.AppDao
import com.madness.collision.unit.api_viewing.database.AppRoom
import com.madness.collision.unit.api_viewing.database.RecordMaintainer
import com.madness.collision.unit.api_viewing.database.maintainer.RecordMtn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield

interface AppRepository {
    fun addApp(app: ApiViewingApp)
    fun getApp(pkgName: String): ApiViewingApp?
    fun getApps(unit: Int): List<ApiViewingApp>
    fun getMaintainedApp(): ApiViewingApp
    fun queryApps(query: String): List<ApiViewingApp>
    fun maintainRecords(context: Context)
}

internal object AppRepo {
    fun dumb(context: Context): DumbAppRepo {
        val dao = AppRoom.getDatabase(context).appDao()
        return DumbAppRepo(AppMediatorRepo(dao, context))
    }

    fun impl(context: Context, lifecycleOwner: LifecycleOwner): AppRepoImpl {
        val dao = AppRoom.getDatabase(context).appDao()
        return AppRepoImpl(dao, lifecycleOwner, AppMediatorRepo(dao, context))
    }
}

class DumbAppRepo(private val medRepo: AppMediatorRepo) : AppRepository {
    override fun addApp(app: ApiViewingApp) = medRepo.add(app)
    override fun getApp(pkgName: String) = medRepo.get(pkgName)
    override fun getApps(unit: Int) = medRepo.get(unit)
    override fun getMaintainedApp(): ApiViewingApp = medRepo.getMaintainedApp()
    override fun queryApps(query: String) = error("No-op")
    override fun maintainRecords(context: Context) = error("No-op")
}

class AppRepoImpl(
    private val appDao: AppDao,
    private val lifecycleOwner: LifecycleOwner,
    private val medRepo: AppMediatorRepo
) : AppRepository {
    override fun addApp(app: ApiViewingApp) {
        medRepo.add(app)
    }

    override fun getApp(pkgName: String): ApiViewingApp? {
        return medRepo.get(pkgName)
    }

    override fun getApps(unit: Int): List<ApiViewingApp> {
        if ((appDao.selectCount() ?: 0) == 0) return fetchAppsFromPlatform(chiefContext, unit)
        return medRepo.get(unit)
    }

    override fun getMaintainedApp(): ApiViewingApp {
        return medRepo.getMaintainedApp()
    }

    override fun queryApps(query: String): List<ApiViewingApp> {
        if (query.isBlank()) return emptyList()
        val q = query.trim()
        return medRepo.getAll().filter { it.name.contains(q, ignoreCase = true) }
    }

    private fun fetchAppsFromPlatform(context: Context, unit: Int): List<ApiViewingApp> {
        val packages = PlatformAppsFetcher(context).withSession(includeApex = false).getRawList()
        val apps = runBlocking { packages.toPkgApps(context, getMaintainedApp()) }
        medRepo.add(apps)
        return when (unit) {
            ApiUnit.USER, ApiUnit.SYS -> apps.filter { it.apiUnit == unit }
            else -> apps
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getChangedPackageNames(context: Context): List<String> {
        val pref = com.madness.collision.util.P
        val prefSettings = context.getSharedPreferences(pref.PREF_SETTINGS, Context.MODE_PRIVATE)
        val bootCount = Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, 0)
        val sequenceNum = if (bootCount == prefSettings.getInt(pref.PACKAGE_CHANGED_BOOT_COUNT, 0))
            prefSettings.getInt(pref.PACKAGE_CHANGED_SEQUENCE_NO, 0)
        else 0
        val changedPackages = context.packageManager.getChangedPackages(sequenceNum)
        prefSettings.edit {
            putInt(pref.PACKAGE_CHANGED_BOOT_COUNT, bootCount)
            putInt(pref.PACKAGE_CHANGED_SEQUENCE_NO, changedPackages?.sequenceNumber ?: sequenceNum)
        }
        changedPackages ?: return emptyList()
        return changedPackages.packageNames
    }

    fun getNewPackages(changedPackages: List<PackageInfo>): List<PackageInfo> {
        return changedPackages.filter {
            it.lastUpdateTime == it.firstInstallTime
        }
    }

    override fun maintainRecords(context: Context) {
        val allPackages = PlatformAppsFetcher(context).withSession(includeApex = false).getRawList()
        maintainRecords(allPackages, context)
    }

    private fun maintainRecords(allPackages: List<PackageInfo>, context: Context) {
        // update records
        val dao = appDao
        RecordMaintainer<PackageInfo>(context, dao).run {
            update(allPackages)
            checkRemoval(allPackages)
        }
        // Maintainer diff
        val lifecycle = lifecycleOwner.lifecycle
        if (RecordMtn.shouldDiff(context) && lifecycle.currentState >= Lifecycle.State.INITIALIZED) {
            // run asynchronously (bind to lifecycle following DAO)
            lifecycle.coroutineScope.launch(Dispatchers.Default) {
                val dataDiff = RecordMtn.diff(context, allPackages, medRepo.getAll(init = false))
                yield()  // cooperative
                RecordMtn.apply(context, dataDiff, allPackages, dao)
            }
        }
    }
}
