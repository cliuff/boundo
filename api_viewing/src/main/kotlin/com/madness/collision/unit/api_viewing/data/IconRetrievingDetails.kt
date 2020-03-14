package com.madness.collision.unit.api_viewing.data

import android.os.Parcel
import android.os.Parcelable


internal class IconRetrievingDetails() : Parcelable {
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
