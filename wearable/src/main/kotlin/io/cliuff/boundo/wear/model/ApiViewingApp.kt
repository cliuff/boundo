package io.cliuff.boundo.wear.model

import android.content.Context
import android.content.pm.PackageInfo
import androidx.core.content.pm.PackageInfoCompat

class ApiViewingApp(val packageName: String) {
    var verName: String = ""
    var verCode: Long = 0L
    var targetAPI: Int = -1
    var updateTime: Long = 0L

    var uid: Int = -1
    var name: String = ""

    constructor(context: Context, info: PackageInfo) : this(info.packageName) {
        verName = info.versionName ?: ""
        verCode = PackageInfoCompat.getLongVersionCode(info)
        updateTime = info.lastUpdateTime

        info.applicationInfo?.let { appInfo ->
            targetAPI = appInfo.targetSdkVersion

            uid = appInfo.uid
            name = context.packageManager.getApplicationLabel(appInfo).toString()
                .ifEmpty { info.packageName }
        }
    }
}
