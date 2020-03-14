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

    constructor(api: Int, isExact: Boolean = false): this(api, Utils.getAndroidVersionByAPI(api, isExact), Utils.getAndroidLetterByAPI(api))

    fun codeName(context: Context): String{
        if (codeName == null) codeName = Utils.getAndroidCodenameByAPI(context, api)
        return codeName ?: ""
    }
}
