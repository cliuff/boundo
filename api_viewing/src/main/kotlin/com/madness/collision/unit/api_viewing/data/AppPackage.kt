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

package com.madness.collision.unit.api_viewing.data

import android.content.pm.ApplicationInfo
import android.os.Parcel
import android.os.Parcelable
import com.madness.collision.util.simpleToJson

class AppPackage private constructor(val basePath: String, val splitPaths: List<String>): Parcelable {

    val hasSplits: Boolean = splitPaths.isNotEmpty()
    // base + split apks
    val apkPaths: List<String> = makeApkPaths()

    constructor(basePath: String): this(basePath, emptyList())

    constructor(apkPaths: List<String>): this(apkPaths[0], apkPaths.subList(1, apkPaths.size))

    constructor(applicationInfo: ApplicationInfo): this(
            applicationInfo.publicSourceDir ?: "",
            applicationInfo.splitPublicSourceDirs?.toList() ?: emptyList()
    )

    private fun makeApkPaths(): List<String> {
        val paths = ArrayList<String>(splitPaths.size + 1)
        paths.add(basePath)
        paths.addAll(splitPaths)
        return paths
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AppPackage) return false
        if (apkPaths != other.apkPaths) return false
        return true
    }

    override fun hashCode(): Int {
        return apkPaths.hashCode()
    }

    override fun toString(): String {
        return apkPaths.simpleToJson()
    }

    constructor(parcel: Parcel) : this(
            parcel.readString() ?: "",
            parcel.createStringArrayList() ?: emptyList()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(basePath)
        parcel.writeStringList(splitPaths)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<AppPackage> {
        override fun createFromParcel(parcel: Parcel): AppPackage {
            return AppPackage(parcel)
        }

        override fun newArray(size: Int): Array<AppPackage?> {
            return arrayOfNulls(size)
        }
    }

}
