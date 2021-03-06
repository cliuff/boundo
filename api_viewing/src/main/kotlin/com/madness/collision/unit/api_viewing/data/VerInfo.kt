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

import android.content.Context
import com.madness.collision.unit.api_viewing.Utils
import com.madness.collision.util.X

internal data class VerInfo(val api: Int, val sdk: String, val letter: Char) {

    companion object {
        fun targetDisplay(app: ApiViewingApp) = VerInfo(app.targetAPI, app.targetSDKDisplay, app.targetSDKLetter)

        fun minDisplay(app: ApiViewingApp) = VerInfo(app.minAPI, app.minSDKDisplay, app.minSDKLetter)
    }

    private var codeName: String? = null

    val displaySdk: String
        get() = if (api == X.DEV) "X" else sdk

    constructor(api: Int, isExact: Boolean = false, isCompact: Boolean = false)
            : this(api, Utils.getAndroidVersionByAPI(api, isExact, isCompact), Utils.getAndroidLetterByAPI(api))

    fun codeName(context: Context): String{
        if (codeName == null) codeName = Utils.getAndroidCodenameByAPI(context, api)
        return codeName ?: ""
    }
}
