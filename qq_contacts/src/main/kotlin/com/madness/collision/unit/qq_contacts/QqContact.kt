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

package com.madness.collision.unit.qq_contacts

import android.content.Context
import android.content.pm.ShortcutInfo
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.RequiresApi
import com.madness.collision.R
import com.madness.collision.util.F
import com.madness.collision.util.ImageUtil
import com.madness.collision.util.notifyBriefly
import java.io.File

internal class QqContact(val no: String): Parcelable {
    companion object{
        @JvmField
        val CREATOR = object : Parcelable.Creator<QqContact> {
            override fun createFromParcel(parcel: Parcel): QqContact {
                return QqContact(parcel)
            }

            override fun newArray(size: Int): Array<QqContact?> {
                return arrayOfNulls(size)
            }
        }

        const val SHORTCUT_ID_PREFIX = "qq"

        @RequiresApi(Build.VERSION_CODES.N_MR1)
        fun fromShortcut(shortcutInfo: ShortcutInfo): QqContact {
            return byShortcutId(shortcutInfo.id).apply {
                name = shortcutInfo.longLabel ?: ""
                miniName = shortcutInfo.shortLabel ?: ""
            }
        }

        fun byShortcutId(shortcutId: String): QqContact {
            return QqContact(getNo(shortcutId))
        }

        fun getNo(shortcutId: String): String{
            return shortcutId.substring(SHORTCUT_ID_PREFIX.length)
        }

        fun getProfilePhotoFolderPath(context: Context): String{
            return F.createPath(F.cachePublicPath(context), Environment.DIRECTORY_PICTURES, "qqInstantManager")
        }
    }

    val shortcutId = "$SHORTCUT_ID_PREFIX$no"
    var name: CharSequence = ""
    var miniName: CharSequence = ""

    constructor(parcel: Parcel) : this(parcel.readString() ?: "") {
        name = parcel.readString() ?: ""
        miniName = parcel.readString() ?: ""
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(no)
        parcel.writeString(name.toString())
        parcel.writeString(miniName.toString())
    }

    override fun describeContents(): Int {
        return 0
    }

    fun getProfilePhotoPath(context: Context): String{
        return F.createPath(getProfilePhotoFolderPath(context), "$shortcutId.avatar")
    }

    fun getProfilePhoto(context: Context): Bitmap?{
        val profilePhotoPath = getProfilePhotoPath(context)
        val profilePhotoFile = File(profilePhotoPath)
        return if (profilePhotoFile.exists()) {
            try {
                ImageUtil.getBitmap(profilePhotoPath)
            } catch (e: OutOfMemoryError) {
                e.printStackTrace()
                context.notifyBriefly(R.string.text_error)
                null
            } catch (e: Exception) {
                e.printStackTrace()
                context.notifyBriefly(R.string.text_error)
                null
            }
        } else null
    }
}