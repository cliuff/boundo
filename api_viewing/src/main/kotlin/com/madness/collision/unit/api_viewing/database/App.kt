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

package com.madness.collision.unit.api_viewing.database

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.madness.collision.unit.api_viewing.data.AppPackage
import com.madness.collision.util.jsonSimpleTo
import com.madness.collision.util.simpleToJson

data class ApiViewingIconInfo(
    @Embedded(prefix = "icS_")  // SystemICon from API (modified by system, may be from an icon pack)
    val system: ApiViewingIconDetails,
    @Embedded(prefix = "icN_")  // NormalICon from APK (unmodified original app icon)
    val normal: ApiViewingIconDetails,
    @Embedded(prefix = "icR_")  // RoundICon from APK (unmodified original app icon)
    val round: ApiViewingIconDetails,
)

class ApiViewingIconDetails(val isDefined: Boolean, val isAdaptive: Boolean)

@Entity(tableName = "app")
class AppEntity(
    @PrimaryKey
    val packageName: String,
    val verName: String,
    val verCode: Long,
    val targetAPI: Int,
    val minAPI: Int,
    val apiUnit: Int,
    val updateTime: Long,
    val isLaunchable: Boolean,
    val appPackage: AppPackage,
    @ColumnInfo(name = "apk_entries", defaultValue = "-1")
    val archiveEntryFlags: Int,
    @ColumnInfo(name = "dex_pkgs", defaultValue = "-1")
    val dexPackageFlags: Int,
    @Embedded
    val iconInfo: ApiViewingIconInfo?,
)


internal class AppConverters {

    @TypeConverter
    fun appPackageToString(appPackage: AppPackage): String {
        return appPackage.apkPaths.simpleToJson()
    }

    @TypeConverter
    fun fromAppPackageString(string: String?): AppPackage {
        return AppPackage(string?.jsonSimpleTo<List<String>>() ?: listOf(""))
    }
}
