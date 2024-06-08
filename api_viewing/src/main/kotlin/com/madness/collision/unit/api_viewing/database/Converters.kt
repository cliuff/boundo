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

package com.madness.collision.unit.api_viewing.database

import androidx.room.TypeConverter
import com.madness.collision.unit.api_viewing.data.AppPackage
import com.madness.collision.unit.api_viewing.util.ApkUtil
import com.madness.collision.util.jsonSimpleTo
import com.madness.collision.util.simpleToJson

internal class Converters {

    @TypeConverter
    fun booleanArrayToString(array: BooleanArray): String {
        return array.simpleToJson()
    }

    @TypeConverter
    fun fromBooleanArrayString(string: String?): BooleanArray {
        return string?.jsonSimpleTo<BooleanArray>() ?: BooleanArray(ApkUtil.NATIVE_LIB_SUPPORT_SIZE) { false }
    }

    @TypeConverter
    fun appPackageToString(appPackage: AppPackage): String {
        return appPackage.apkPaths.simpleToJson()
    }

    @TypeConverter
    fun fromAppPackageString(string: String?): AppPackage {
        return AppPackage(string?.jsonSimpleTo<List<String>>() ?: listOf(""))
    }
}
