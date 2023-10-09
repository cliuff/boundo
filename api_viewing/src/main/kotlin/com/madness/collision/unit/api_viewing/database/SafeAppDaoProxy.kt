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

package com.madness.collision.unit.api_viewing.database

import com.madness.collision.unit.api_viewing.data.ApiViewingApp

class SafeAppDaoProxy(private val dao: AppDao) : AppDao {
    private inline fun safely(block: () -> Unit) = safely(block) { }

    private inline fun <R> safely(block: () -> R, onError: () -> R): R {
        return try {
            block()
        } catch (e: DataMaintainer.InterceptException) {
            e.printStackTrace()
            onError()
        }
    }

    override fun insert(vararg app: ApiViewingApp) {
        safely { dao.insert(*app) }
    }

    override fun insert(apps: List<ApiViewingApp>) {
        safely { dao.insert(apps) }
    }

    override fun delete(vararg app: ApiViewingApp) {
        safely { dao.delete(*app) }
    }

    override fun delete(apps: List<ApiViewingApp>) {
        safely { dao.delete(apps) }
    }

    override fun deletePackageName(vararg packageName: String) {
        safely { dao.deletePackageName(*packageName) }
    }

    override fun deletePackageNames(packageNames: List<String>) {
        safely { dao.deletePackageNames(packageNames) }
    }

    override fun deleteNonExistPackageNames(packageNames: List<String>) {
        safely { dao.deleteNonExistPackageNames(packageNames) }
    }

    override fun deleteAll() {
        safely { dao.deleteAll() }
    }

    override fun selectApps(unit: Int): List<ApiViewingApp> {
        return safely({ dao.selectApps(unit) }, { emptyList() })
    }

    override fun selectApps(packageNames: List<String>): List<ApiViewingApp> {
        return safely({ dao.selectApps(packageNames) }, { emptyList() })
    }

    override fun selectApp(packageName: String): ApiViewingApp? {
        return safely({ dao.selectApp(packageName) }, { null })
    }

    override fun selectIsExist(packageName: String): Boolean {
        return safely({ dao.selectIsExist(packageName) }, { false })
    }

    override fun selectCount(): Int? {
        return safely({ dao.selectCount() }, { null })
    }

    override fun selectUpdateTime(packageName: String): Long? {
        return safely({ dao.selectUpdateTime(packageName) }, { null })
    }
}
