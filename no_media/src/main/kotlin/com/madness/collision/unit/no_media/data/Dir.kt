package com.madness.collision.unit.no_media.data

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import java.io.File

internal class Dir() : Parcelable {
    val images: MutableList<Media> = mutableListOf()
    var path: String = ""
    /**
     * path, followed by a separator
     */
    val asDirectory: String
        get() = path + File.separator
    lateinit var nmFile: File
    val nm: Boolean
        get() = nmFile.exists()

    fun getCleanPath(pathPrefix: String): String{
        if (!path.startsWith(pathPrefix)) return path
        return path.replaceFirst(pathPrefix, "").run {
            if (this.isEmpty()) File.separator else this
        }
    }

    private fun initNMFile() {
        nmFile = File("$asDirectory.nomedia")
    }

    constructor(context: Context, path: String, list: List<BasicInfo>, limitAmount: Boolean) : this() {
        this.path = path
        initNMFile()
        for (item in list) {
            if (images.size == 1 && limitAmount) break
            images.add(Media(context, item))
        }
    }

    constructor(parcel: Parcel) : this() {
        path = parcel.readString() ?: ""
        initNMFile()
        parcel.readList(images as List<*>, ClassLoader.getSystemClassLoader())
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(path)
        parcel.writeList(images as List<*>?)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Dir> {
        override fun createFromParcel(parcel: Parcel): Dir {
            return Dir(parcel)
        }

        override fun newArray(size: Int): Array<Dir?> {
            return arrayOfNulls(size)
        }
    }
}
