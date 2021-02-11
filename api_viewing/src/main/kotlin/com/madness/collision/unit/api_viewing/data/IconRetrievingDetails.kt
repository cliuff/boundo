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

import android.os.Parcel
import android.os.Parcelable

class IconRetrievingDetails() : Parcelable {
    var width: Int = 0
    var height: Int = 0
    var isDefault: Boolean = false
    var standardWidth: Int = 0

    var shouldClip: Boolean = false
    var shouldStroke: Boolean = false

    constructor(parcel: Parcel) : this() {
        width = parcel.readInt()
        height = parcel.readInt()
        isDefault = parcel.readByte() != 0.toByte()
        standardWidth = parcel.readInt()
        shouldClip = parcel.readByte() != 0.toByte()
        shouldStroke = parcel.readByte() != 0.toByte()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(width)
        parcel.writeInt(height)
        parcel.writeByte(if (isDefault) 1 else 0)
        parcel.writeInt(standardWidth)
        parcel.writeByte(if (shouldClip) 1 else 0)
        parcel.writeByte(if (shouldStroke) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<IconRetrievingDetails> {
        override fun createFromParcel(parcel: Parcel): IconRetrievingDetails {
            return IconRetrievingDetails(parcel)
        }

        override fun newArray(size: Int): Array<IconRetrievingDetails?> {
            return arrayOfNulls(size)
        }
    }
}
