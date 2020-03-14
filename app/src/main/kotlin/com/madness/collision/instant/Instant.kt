package com.madness.collision.instant

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import com.madness.collision.R
import com.madness.collision.main.ImmortalActivity
import com.madness.collision.main.MainActivity
import com.madness.collision.unit.Unit
import com.madness.collision.util.P
import com.madness.collision.util.SystemUtil
import com.madness.collision.util.X

class Instant(private val context: Context, private val manager: ShortcutManager){
    val dynamicShortcuts: List<ShortcutInfo>
    @RequiresApi(Build.VERSION_CODES.N_MR1)
    get() = manager.dynamicShortcuts

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    fun buildShortcut(id: String): ShortcutInfo{
        return ShortcutInfo.Builder(context, id).run {
            when (id) {
                P.SC_ID_API_VIEWER -> {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.EMPTY, context, MainActivity::class.java)
                    intent.putExtras(MainActivity.forItem(Unit.UNIT_NAME_API_VIEWING))
                    val iconRes = if (X.aboveOn(X.O)) R.mipmap.shortcut_api else R.drawable.shortcut_api_vector
                    val localeContext = SystemUtil.getLocaleContextSys(context)
                    setShortLabel(localeContext.getString(R.string.apiViewerShort))
                    setLongLabel(localeContext.getString(R.string.apiViewer))
                    setIntent(intent)
                    setIcon(Icon.createWithResource(context, iconRes))
                    build()
                }
                P.SC_ID_IMMORTAL -> {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.EMPTY, context, ImmortalActivity::class.java)
                    setShortLabel("Immortal")
                    setLongLabel("Immortal")
                    setIntent(intent)
                    build()
                }
                P.SC_ID_AUDIO_TIMER -> {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.EMPTY, context, MainActivity::class.java)
                    intent.putExtras(MainActivity.forItem(Unit.UNIT_NAME_AUDIO_TIMER))
                    val iconRes = if (X.aboveOn(X.O)) R.mipmap.ic_shortcut_audio_timer else R.drawable.ic_shortcut_audio_timer_legacy
                    val localeContext = SystemUtil.getLocaleContextSys(context)
                    setShortLabel(localeContext.getString(R.string.unit_audio_timer))
                    setLongLabel(localeContext.getString(R.string.unit_audio_timer))
                    setIntent(intent)
                    setIcon(Icon.createWithResource(context, iconRes))
                    build()
                }
                else -> build()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    fun addDynamicShortcuts(vararg ids: String){
        if (ids.isEmpty()) return
        val shortcuts: MutableList<ShortcutInfo> = mutableListOf()
        ids.forEach { shortcuts.add(buildShortcut(it)) }
        manager.addDynamicShortcuts(shortcuts)
    }

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    fun removeDynamicShortcuts(vararg ids: String){
        if (ids.isEmpty()) return
        manager.removeDynamicShortcuts(ids.toList())
    }

    /**
     * Rebuild all the dynamic shortcuts and update.
     */
    @RequiresApi(Build.VERSION_CODES.N_MR1)
    fun refreshAllDynamicShortcuts() = mutableListOf<ShortcutInfo>().run {
        dynamicShortcuts.forEach { add(buildShortcut(it.id)) }
        manager.updateShortcuts(this)
    }

    /**
     * Rebuild the dynamic shortcuts with given ids.
     * Useful when shortcut resources are updated,
     * in which case the affected shortcuts will behave unexpectedly in launcher.
     */
    @RequiresApi(Build.VERSION_CODES.N_MR1)
    fun refreshDynamicShortcuts(vararg ids: String){
        val shortcuts = mutableListOf<ShortcutInfo>()
        // below: get all ids that can be found in the present dynamic shortcuts then rebuild them
        ids.toList().let { idList ->
            dynamicShortcuts.filter { idList.contains(it.id) }.map { it.id }
        }.forEach { shortcuts.add(buildShortcut(it)) }
        manager.updateShortcuts(shortcuts)
    }
}
