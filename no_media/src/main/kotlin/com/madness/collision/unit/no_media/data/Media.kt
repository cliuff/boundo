package com.madness.collision.unit.no_media.data

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import java.io.File

internal class Media private constructor(/*
        context: Context,*/
        val id: Long,
        val path: String,
        val name: String,
        val isVideo: Boolean
) : Parcelable {
    fun getCleanPath(pathPrefix: String): String{
        if (!path.startsWith(pathPrefix)) return path
        return path.replaceFirst(pathPrefix, "").run {
            if (this.isEmpty()) File.separator else this
        }
    }

    //Bitmap image, thumbnailMini, thumbnailMicro;
    /*
    init{
        this.image = BitmapFactory.decodeFile(path);
        if (isVideo){
            this.thumbnailMini = MediaStore.Video.Thumbnails.getThumbnail(
                    context.getContentResolver(), id,
                    MediaStore.Video.Thumbnails.MINI_KIND,
                    new BitmapFactory.Options()
            );
            this.thumbnailMicro = MediaStore.Video.Thumbnails.getThumbnail(
                    context.getContentResolver(), id,
                    MediaStore.Video.Thumbnails.MICRO_KIND,
                    new BitmapFactory.Options()
            );
        }else {
            this.thumbnailMini = MediaStore.Images.Thumbnails.getThumbnail(
                    context.getContentResolver(), id,
                    MediaStore.Images.Thumbnails.MINI_KIND,
                    new BitmapFactory.Options());
            this.thumbnailMicro = MediaStore.Images.Thumbnails.getThumbnail(
                    context.getContentResolver(), id,
                    MediaStore.Images.Thumbnails.MICRO_KIND,
                    new BitmapFactory.Options());
        }
    }*/

    constructor(parcel: Parcel) : this(
            parcel.readLong(),
            parcel.readString() ?: "",
            parcel.readString() ?: "",
            parcel.readByte() != 0.toByte()
    )

    constructor(context: Context, info: BasicInfo) : this(info.id, info.path, info.displayName, info.isVideo)

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeString(path)
        parcel.writeString(name)
        parcel.writeByte(if (isVideo) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Media> {
        override fun createFromParcel(parcel: Parcel): Media {
            return Media(parcel)
        }

        override fun newArray(size: Int): Array<Media?> {
            return arrayOfNulls(size)
        }
    }
}
