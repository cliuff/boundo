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

package com.madness.collision.util

import android.content.res.TypedArray
import com.madness.collision.BuildConfig
import java.util.*

object P {
    const val AT_TIME_HOUR = "atTimeHour"
    const val AT_TIME_MINUTE = "atTimeMinute"

    const val UNIT_FREQUENCIES = "unitFrequencies"
    const val UNIT_PINNED = "unitPinned"

    const val PACKAGE_CHANGED_BOOT_COUNT = "changedPkgBootCount"
    const val PACKAGE_CHANGED_SEQUENCE_NO = "changedPkgSequenceNo"
    const val PACKAGE_CHANGED_TIMESTAMP = "changedPkgTimestamp"

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
    const val PREF_TIMETABLE = "iCalendarPreferences"

    const val APPLICATION_INITIATE = "initiateApp" // boolean
    const val APPLICATION_V = "v" // int

    const val APP_INFO_PACKAGE = "toolsAppPackageName"
    const val APP_INFO_PACKAGE_DEFAULT = BuildConfig.BUILD_PACKAGE
    const val ADVANCED = "debug"
    const val SETTINGS_DARK_STATUS_ICON = "DarkStatusBarIcon"
    const val SETTINGS_DARK_NAV_ICON = "DarkNavigationBarIcon"
    const val SETTINGS_UPDATE_NOTIFY = "updateNotify"
    const val SETTINGS_UPDATE_VIA_SETTINGS = "updateViaSettings"
    const val SETTINGS_LANGUAGE = "language"
    const val TT_MANUAL = "icsInstructor"
    const val TT_CAL_DEFAULT_GOOGLE = "googleCalendarDefault"
    const val TT_APP_MODE = "iCalendarAppMode"
    const val TT_PATH_ICS = "icsFilePath"

    const val TT_DATE_START = "originalDateStart"
    val TT_DATE_START_DEFAULT: String
    get() {
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        return if ((cal.get(Calendar.MONTH) + 1) in 1 until 7) "${year}0101" // after the end of the former half semester(December) and before the start of summer holiday (July)
        else "${year}0901"
    }
    const val TT_TIME_MORNING = "morningTime"
    const val TT_TIME_MORNING_DEFAULT = "0830"
    const val TT_TIME_AFTERNOON = "afternoonTime"
    const val TT_TIME_AFTERNOON_DEFAULT = "1420"
    const val TT_TIME_EVENING = "eveningTime"
    const val TT_TIME_EVENING_DEFAULT = "1900"
    const val TT_TIME_CLASS = "classTime"
    const val TT_TIME_CLASS_DEFAULT = 45
    const val TT_TIME_BREAK_SUPERIOR = "superiorBreakTime"
    const val TT_TIME_BREAK_SUPERIOR_DEFAULT = 20
    const val TT_TIME_BREAK_INFERIOR = "inferiorBreakTime"
    const val TT_TIME_BREAK_INFERIOR_DEFAULT = 10
    const val TT_TIME_BREAK_MORNING_SUPERIOR = "morningBreakTimeSuperior"
    const val TT_TIME_BREAK_MORNING_INFERIOR = "morningBreakTimeInferior"
    const val TT_TIME_BREAK_AFTERNOON_SUPERIOR = "afternoonBreakTimeSuperior"
    const val TT_TIME_BREAK_AFTERNOON_INFERIOR = "afternoonBreakTimeInferior"
    const val TT_TIME_BREAK_EVENING_SUPERIOR = "eveningBreakTimeSuperior"
    const val TT_TIME_BREAK_EVENING_INFERIOR = "eveningBreakTimeInferior"

    const val SC_ID_API_VIEWER = "instant_sdk" // SC => shortcut
    const val SC_ID_IMMORTAL = "immortal"
    const val SC_ID_AUDIO_TIMER = "unitAT"

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