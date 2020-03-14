package com.madness.collision.unit.no_media.data

import android.os.Parcel
import android.os.Parcelable

internal class BasicInfo(
        val dir: String,
        val displayName: String,
        val path: String,
        val id: Long,
        val isVideo: Boolean
) : Parcelable {

    constructor(parcel: Parcel) : this(
            parcel.readString() ?: "",
            parcel.readString() ?: "",
            parcel.readString() ?: "",
            parcel.readLong(),
            parcel.readByte() != 0.toByte())

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(dir)
        parcel.writeString(displayName)
        parcel.writeString(path)
        parcel.writeLong(id)
        parcel.writeByte(if (isVideo) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<BasicInfo> {
        override fun createFromParcel(parcel: Parcel): BasicInfo {
            return BasicInfo(parcel)
        }

        override fun newArray(size: Int): Array<BasicInfo?> {
            return arrayOfNulls(size)
        }
    }
}
