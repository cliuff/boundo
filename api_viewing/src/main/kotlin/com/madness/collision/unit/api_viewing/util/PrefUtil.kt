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

package com.madness.collision.unit.api_viewing.util

import com.madness.collision.unit.api_viewing.MyUnit

internal object PrefUtil {
    const val AV_TAGS = "AvTags"
    const val API_APK_PRELOAD = "apiAPKPreload"
    const val API_APK_PRELOAD_DEFAULT = false
    const val API_CIRCULAR_ICON = "SDKCircularIcon"
    const val API_CIRCULAR_ICON_DEFAULT = true
    const val API_PACKAGE_ROUND_ICON = "APIPackageRoundIcon"
    const val API_PACKAGE_ROUND_ICON_DEFAULT = false
    const val AV_CLIP_ROUND = "AVClip2Round"
    const val AV_CLIP_ROUND_DEFAULT = true
    const val AV_SWEET = "AVSweet"
    const val AV_SWEET_DEFAULT = true
    const val AV_VIEWING_TARGET = "AVViewingTarget"
    const val AV_VIEWING_TARGET_DEFAULT = true
    const val AV_INCLUDE_DISABLED = "AVIncludeDisabled"
    const val AV_INCLUDE_DISABLED_DEFAULT = false
    const val AV_INIT_NOTIFY = "APIViewerInitialized"
    const val AV_INIT_NOTIFY_DEFAULT = false
    const val AV_SORT_ITEM = "SDKCheckSortSpinnerSelection"
    const val AV_SORT_ITEM_DEFAULT = MyUnit.SORT_POSITION_API_TIME
    const val AV_LIST_SRC_ITEM = "SDKCheckDisplaySpinnerSelection"
    const val AV_LIST_SRC_ITEM_DEFAULT = MyUnit.DISPLAY_APPS_USER
}
