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

package com.madness.collision.util

import android.content.res.TypedArray
import androidx.core.content.res.use

object P {
    const val AT_TIME_HOUR = "atTimeHour"
    const val AT_TIME_MINUTE = "atTimeMinute"

    const val UNIT_FREQUENCIES = "unitFrequencies"
    const val UNIT_PINNED = "unitPinned"
    const val UNIT_DISABLED = "unitDisabled"

    const val PACKAGE_CHANGED_BOOT_COUNT = "changedPkgBootCount"
    const val PACKAGE_CHANGED_SEQUENCE_NO = "changedPkgSequenceNo"
    const val PACKAGE_CHANGED_TIMESTAMP = "changedPkgTimestamp"
    const val PACKAGE_CHANGED_DIFF_TIME = "changedPkgDiffTime"

    const val NOTIFICATION_CHANNELS_NO = "notificationChannelsNo"

    const val WEBP_COMPRESS_QUALITY_FIRST = 100
    const val WEBP_COMPRESS_SPACE_FIRST = 85

    const val URI_SCANNING_FOLDER = "externalPrimaryUri"

    const val DART_PLACEHOLDER = "T" // this character has a simple shape and few strokes

//    const val AD_ID_DEVICE = BuildConfig.AD_PUBLISHER_ID
//    const val AD_ID_SAMPLE = "ca-app-pub-3940256099942544~3347511713"
//    const val AD_UNIT_DONATE = "ca-app-pub-7448935830323449/7838524430"
//    const val AD_UNIT_SAMPLE = "7448935830323449/7838524430"
//    const val AD_BUILD_TEST = "CC60137A04D550C74DDD54C3966FE20F"

    const val PREF_SETTINGS = "SettingsPreferences"

    const val APPLICATION_INITIATE = "initiateApp" // boolean
    const val APPLICATION_V = "v" // int

    const val ADVANCED = "debug"
    const val SETTINGS_DARK_STATUS_ICON = "DarkStatusBarIcon"
    const val SETTINGS_DARK_NAV_ICON = "DarkNavigationBarIcon"
    const val SETTINGS_UPDATE_NOTIFY = "updateNotify"
    const val SETTINGS_UPDATE_VIA_SETTINGS = "updateViaSettings"
    const val SETTINGS_LANGUAGE = "language"

    const val SC_ID_API_VIEWER = "instant_sdk" // SC => shortcut
    const val SC_ID_IMMORTAL = "immortal"
    const val SC_ID_AUDIO_TIMER = "unitAT"
    const val SC_ID_DEVICE_MANAGER = "unitDM"

    const val DIR_NAME_LOG = "Log"

    const val CONTACT_EMAIL = "libekliff@gmail.com"

    const val LINK_SOURCE_CODE = "https://github.com/cliuff/boundo"
    const val LINK_TELEGRAM_GROUP = "https://t.me/cliuff_boundo"
    const val LINK_TWITTER_ACCOUNT = "https://twitter.com/libekliff"

    const val IMMORTAL_EXTRA_LAUNCH_MODE = "immortalLaunchMode"
    const val IMMORTAL_EXTRA_LAUNCH_MODE_MORTAL = "mortal"

    const val SETTINGS_THEME_NONE = -1

    /**
     * Get the corresponding entry and index of the given [value]
     * Note: [entries] and [values] will be recycled
     */
    fun getPrefIndexedEntry(value: String, entries: TypedArray, values: TypedArray): IndexedValue<String> {
        var re = IndexedValue(0, "")
        values.use {
            for (i in 0 until it.length()){
                if (it.getString(i) != value) continue
                re = IndexedValue(i, entries.use { e -> e.getString(i) ?: "" })
                break
            }
        }
        return re
    }

    /**
     * Get the corresponding index of the given [value]
     * Note: [values] will be recycled
     */
    fun getPrefIndex(value: String, values: TypedArray): Int {
        var re = 0
        values.use {
            for (i in 0 until it.length()){
                if (it.getString(i) != value) continue
                re = i
                break
            }
        }
        return re
    }
}