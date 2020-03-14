package com.madness.collision.wearable.util

import android.content.res.TypedArray
import com.madness.collision.wearable.BuildConfig

object P {
    const val PACKAGE_CHANGED_BOOT_COUNT = "changedPkgBootCount"
    const val PACKAGE_CHANGED_SEQUENCE_NO = "changedPkgSequenceNo"

    const val NOTIFICATION_CHANNELS_NO = "notificationChannelsNo"

    const val WEBP_COMPRESS_QUALITY_FIRST = 100
    const val WEBP_COMPRESS_SPACE_FIRST = 85

    const val URI_EXTERNAL_PRIMARY = "externalPrimaryUri"

    const val DART_PLACEHOLDER = "T" // this character has a simple shape and few strokes

    const val PREF_SETTINGS = "PrefSettings"

    const val APPLICATION_INITIATE = "initiateApp" // boolean
    const val APPLICATION_V = "v" // int

    const val APP_INFO_PACKAGE = "toolsAppPackageName"
    const val APP_INFO_PACKAGE_DEFAULT = BuildConfig.BUILD_PACKAGE
    const val ADVANCED = "debug"
    const val SETTINGS_LANGUAGE = "language"

    const val DIR_NAME_LOG = "Log"

    const val CONTACT_EMAIL = "ballupon@gmail.com"
    const val CONTACT_QQ = "909713819"

    const val IMMORTAL_EXTRA_LAUNCH_MODE = "immortalLaunchMode"
    const val IMMORTAL_EXTRA_LAUNCH_MODE_MORTAL = "mortal"

    const val SETTINGS_THEME_NONE = -1

    fun getPrefIndexedEntry(value: String, entries: TypedArray, values: TypedArray): IndexedValue<String>{
        var re = IndexedValue(0, "")
        for (i in 0 until values.length()){
            if (values.getString(i) != value) continue
            re = IndexedValue(i, entries.getString(i) ?: "")
            break
        }
        entries.recycle()
        values.recycle()
        return re
    }

    fun getPrefIndex(value: String, entries: TypedArray, values: TypedArray): Int{
        var re = 0
        for (i in 0 until values.length()){
            if (values.getString(i) != value) continue
            re = i
            break
        }
        entries.recycle()
        values.recycle()
        return re
    }
}