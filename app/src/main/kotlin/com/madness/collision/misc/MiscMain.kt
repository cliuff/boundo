package com.madness.collision.misc

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ShortcutManager
import android.graphics.drawable.Drawable
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.core.content.edit
import com.madness.collision.BuildConfig
import com.madness.collision.instant.Instant
import com.madness.collision.main.MainViewModel
import com.madness.collision.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import kotlin.random.Random

internal object MiscMain {

    fun clearCache(context: Context){
        deleteDirs(F.valCachePubAvApk(context), F.valCachePubAvLogo(context))
        if(Random(System.currentTimeMillis()).nextInt(100) == 1) deleteDirs(F.valCachePubAvSeal(context))
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
        if ((verOri in 0 until 20021716) && X.aboveOn(X.N_MR1)) {
            val sm = context.getSystemService(ShortcutManager::class.java)
            if (sm != null) Instant(context, sm).refreshDynamicShortcuts(P.SC_ID_API_VIEWER)
        }
        if (verOri in 0 until 19091423) deleteDirs(F.cachePublicPath(context))
    }

    private fun deleteDirs(vararg paths: String){
        paths.forEach {
            val f = File(it)
            if (f.exists()) f.deleteRecursively()
        }
    }

    fun updateExteriorBackgrounds(context: Context){
        GlobalScope.launch {
            loadExteriorBackgrounds(context)
            if (context is ComponentActivity){
                launch(Dispatchers.Main){
                    val mainViewModel: MainViewModel by context.viewModels()
                    mainViewModel.background.value = mainApplication.background
                }
            }
        }
    }

    private fun loadExteriorBackgrounds(context: Context){
        val app = mainApplication
        val backPath = if (app.isDarkTheme) F.valCachePubExteriorPortraitDark(context) else F.valCachePubExteriorPortrait(context)
        (File(backPath).exists()).let {
            if (it) app.background = Drawable.createFromPath(backPath)
            app.exterior = it
        }
    }

    fun registerNotificationChannels(context: Context, prefSettings: SharedPreferences){
        val ver = prefSettings.getString(P.NOTIFICATION_CHANNELS_NO, "") ?: ""
        prefSettings.edit { putString(P.NOTIFICATION_CHANNELS_NO, NotificationsUtil.NO) }
        if (ver == NotificationsUtil.NO) return
        NotificationsUtil.updateAllGroups(context)
        NotificationsUtil.updateAllChannels(context)
    }

}
