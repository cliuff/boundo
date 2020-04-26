/*
 * Copyright 2020 Clifford Liu
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
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import com.madness.collision.R as MainR
import com.madness.collision.unit.Bridge
import com.madness.collision.unit.Unit
import com.madness.collision.unit.UpdatesProvider
import com.madness.collision.unit.api_viewing.util.PrefUtil
import com.madness.collision.util.Page
import kotlin.reflect.KClass

object MyBridge: Bridge() {

    override val unitName: String = Unit.UNIT_NAME_API_VIEWING
    override val args: List<KClass<*>> = listOf(Bundle::class)

    /**
     * @param args extras: [Bundle]?
     */
    override fun getUnitInstance(vararg args: Any?): Unit {
        return MyUnit().apply { arguments = args[0] as Bundle? }
    }

    override fun getUpdates(): UpdatesProvider? {
        return MyUpdatesProvider()
    }

    override fun getSettings(): Fragment? {
        return Page<PrefAv>(MainR.string.apiViewer)
    }

    @Suppress("unused")
    fun clearSeals() {
        APIAdapter.seals.clear()
        APIAdapter.sealBack.clear()
    }

    @Suppress("unused")
    fun clearApps(activity: ComponentActivity) {
        val viewModel: ApiViewingViewModel by activity.viewModels()
        viewModel.clearCache()
    }

    @Suppress("unused")
    fun initTagSettings(context: Context, prefSettings: SharedPreferences) {
        val tagSettings = mutableSetOf<String>()
        listOf(
                R.string.prefAvTagsValuePackageInstallerGp,
                R.string.prefAvTagsValuePackageInstallerPi,
                R.string.prefAvTagsValueCrossPlatformFlu,
                R.string.prefAvTagsValueCrossPlatformRn,
                R.string.prefAvTagsValueCrossPlatformXar,
                R.string.prefAvTagsValueNativeLibArm,
                R.string.prefAvTagsValueNativeLibX86,
                R.string.prefAvTagsValuePrivilegeSys,
                R.string.prefAvTagsValueIconAdaptive
        ).forEach {
            tagSettings.add(context.getString(it))
        }
        prefSettings.edit {
            putStringSet(PrefUtil.AV_TAGS, tagSettings)
        }
    }

    @Suppress("unused")
    fun updateTagSettingsAi(context: Context, prefSettings: SharedPreferences) {
        val tagSettings = prefSettings.getStringSet(PrefUtil.AV_TAGS, emptySet())!!.toMutableSet()
        tagSettings.add(context.getString(R.string.prefAvTagsValueIconAdaptive))
        prefSettings.edit {
            putStringSet(PrefUtil.AV_TAGS, tagSettings)
        }
    }
    
}
