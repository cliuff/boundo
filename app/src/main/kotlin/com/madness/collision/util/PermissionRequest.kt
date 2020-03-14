package com.madness.collision.util

import android.app.Activity
import android.os.Handler
import androidx.core.app.ActivityCompat
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

interface PermissionRequest {
    fun requestPermission(permissions: Array<String>, requestCode: Int)
}

class PermissionRequestHandler(activity: Activity) : Handler(), PermissionRequest {
    var resumeJob: Runnable = Runnable { }
    private val activity = MutableLiveData(activity)

    override fun requestPermission(permissions: Array<String>, requestCode: Int) {
        activity.value?.let {
            GlobalScope.launch {
                ActivityCompat.requestPermissions(it, permissions, requestCode)
            }
        }
    }
}
