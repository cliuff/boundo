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
import com.madness.collision.chief.chiefContext
import com.madness.collision.unit.api_viewing.data.ApiUnit
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.database.AppDao
import com.madness.collision.unit.api_viewing.database.AppRoom
import com.madness.collision.unit.api_viewing.database.RecordMaintainer
import com.madness.collision.unit.api_viewing.database.maintainer.RecordMtn
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.yield
import java.lang.ref.WeakReference

interface AppRepository {
    fun addApp(app: ApiViewingApp)
    fun getApp(pkgName: String): ApiViewingApp?
    fun getApps(unit: Int): List<ApiViewingApp>
    fun getMaintainedApp(): ApiViewingApp
    fun queryApps(query: String): List<ApiViewingApp>
    suspend fun maintainRecords(context: Context)
}

internal object AppRepo {
    // enable unified state sharing of repo
    private var cachedRepo: WeakReference<AppRepoImpl> = WeakReference(null)

    fun dumb(context: Context): DumbAppRepo {
        val dao = AppRoom.getDatabase(context).appDao()
        return DumbAppRepo(AppMediatorRepo(dao, context.applicationContext))
    }

    fun impl(context: Context, pkgProvider: PackageInfoProvider): AppRepoImpl {
        cachedRepo.get()?.let { return it }
        val dao = AppRoom.getDatabase(context).appDao()
        val mediator = AppMediatorRepo(dao, context.applicationContext)
        return AppRepoImpl(dao, mediator, pkgProvider)
            .also { cachedRepo = WeakReference(it) }
    }
}

class DumbAppRepo(private val medRepo: AppMediatorRepo) : AppRepository {
    override fun addApp(app: ApiViewingApp) = medRepo.add(app)
    override fun getApp(pkgName: String) = medRepo.get(pkgName)
    override fun getApps(unit: Int) = medRepo.get(unit)
    override fun getMaintainedApp(): ApiViewingApp = medRepo.getMaintainedApp()
    override fun queryApps(query: String) = error("No-op")
    override suspend fun maintainRecords(context: Context) = error("No-op")
}

class AppRepoImpl(
    private val appDao: AppDao,
    private val medRepo: AppMediatorRepo,
    private val pkgProvider: PackageInfoProvider,
) : AppRepository {
    private val mutexMtn = Mutex()
    var lastMaintenanceTime: Long = -1L
        private set

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
        val packages = pkgProvider.getAll()
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

    override suspend fun maintainRecords(context: Context) = mutexMtn.withLock {
        val allPackages = pkgProvider.getAll()
        yield()  // cooperative

        // update records
        val dao = appDao
        RecordMaintainer<PackageInfo>(context, dao).run {
            update(allPackages)
            checkRemoval(allPackages)
        }
        lastMaintenanceTime = System.currentTimeMillis()
        yield()  // cooperative

        // Maintainer diff
        if (RecordMtn.shouldDiff(context)) {
            val dataDiff = RecordMtn.diff(context, allPackages, medRepo.getAll(init = false))
            yield()  // cooperative
            RecordMtn.apply(context, dataDiff, allPackages, dao)
        }
    }
}
