package com.madness.collision.wearable.misc

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.madness.collision.wearable.BuildConfig
import com.madness.collision.wearable.util.F
import com.madness.collision.wearable.util.P
import java.io.File

internal object MiscMain {

    fun clearCache(context: Context){
    }

    /**
     * Check app version and do certain updates when app updates
     */
    fun ensureUpdate(context: Context, prefSettings: SharedPreferences){
        val ver = BuildConfig.VERSION_CODE
        val verOri = prefSettings.getInt(P.APPLICATION_V, ver)
        // below: update registered version
        prefSettings.edit { putInt(P.APPLICATION_V, ver) }
        // below: app gone through update process or erased data
        if (ver == verOri) return
        // below: app in update process to the newest
        // below: apply actions
        if (verOri in 0 until 19091423) deleteDirs(F.cachePublicPath(context))
    }

    private fun deleteDirs(vararg paths: String){
        paths.forEach {
            val f = File(it)
            if (f.exists()) f.deleteRecursively()
        }
    }
}
