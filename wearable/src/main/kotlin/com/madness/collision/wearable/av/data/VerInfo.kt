package com.madness.collision.wearable.av.data

import com.madness.collision.wearable.util.X

internal data class VerInfo(val api: Int, val sdk: String) {

    companion object {
        fun targetDisplay(app: ApiViewingApp) = VerInfo(app.targetAPI, app.targetSDKDisplay)
    }

    constructor(api: Int, isExact: Boolean = false): this(api, X.getAndroidVersionByAPI(api, isExact))
}
