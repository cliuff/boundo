package com.madness.collision.util

import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.madness.collision.R

/**
 * notification related
 */
object NotificationsUtil{
    const val NO = "20030917"
    const val ID_SCREEN_CAPTURING = 2003091701
    const val ID_AUDIO_TIMER = 2003091702
    const val CHANNEL_SERVICE = "channelService"
    const val CHANNEL_AUDIO_TIMER = "channelAudioTimer"
    private val ALL_CHANNELS: Array<String>
        get() = arrayOf(CHANNEL_SERVICE, CHANNEL_AUDIO_TIMER)
    const val CHANNEL_GROUP_APP = "groupApp"
    private val ALL_CHANNEL_GROUPS: Array<String>
        get() = arrayOf(CHANNEL_GROUP_APP)

    private fun getImportance(id: String): Int{
        return when(id){
            CHANNEL_SERVICE -> NotificationManagerCompat.IMPORTANCE_MIN
            CHANNEL_AUDIO_TIMER -> NotificationManagerCompat.IMPORTANCE_MIN
            else -> NotificationManagerCompat.IMPORTANCE_DEFAULT
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannel(context: Context, id: String): NotificationChannel {
        val localeContext = SystemUtil.getLocaleContextSys(context)
        return when(id){
            CHANNEL_SERVICE -> {
                channel(localeContext, id, R.string.notifyChannelService).apply {
                    description = localeContext.getString(R.string.notifyChannelServiceDesc)
                    group = CHANNEL_GROUP_APP
                }
            }
            CHANNEL_AUDIO_TIMER -> {
                channel(localeContext, id, R.string.unit_audio_timer).apply {
                    description = localeContext.getString(R.string.unit_audio_timer)
                    group = CHANNEL_GROUP_APP
                }
            }
            else -> NotificationChannel(id, localeContext.getString(R.string.app_name), NotificationManager.IMPORTANCE_DEFAULT)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun channel(localeContext: Context, id: String, nameRes: Int): NotificationChannel {
        return NotificationChannel(id, localeContext.getString(nameRes), getImportance(id))
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannelGroup(context: Context, id: String): NotificationChannelGroup {
        val localeContext = SystemUtil.getLocaleContextSys(context)
        return when(id){
            CHANNEL_GROUP_APP -> {
                NotificationChannelGroup(id, localeContext.getString(R.string.notifyChannelGroupApp)).apply {
                    if (X.aboveOn(X.P)) description = localeContext.getString(R.string.notifyChannelGroupAppDesc)
                }
            }
            else -> NotificationChannelGroup(id, localeContext.getString(R.string.app_name))
        }
    }

    fun updateAllChannels(context: Context){
        if (X.belowOff(X.O)) return
        val manager = NotificationManagerCompat.from(context)
        manager.notificationChannels.forEach {
            if (it.id !in ALL_CHANNELS) manager.deleteNotificationChannel(it.id)
        }
        ALL_CHANNELS.map { createChannel(context, it) }.let {
            manager.createNotificationChannels(it)
        }
    }

    fun updateAllGroups(context: Context){
        if (X.belowOff(X.O)) return
        val manager = NotificationManagerCompat.from(context)
        manager.notificationChannelGroups.forEach {
            if (it.id !in ALL_CHANNEL_GROUPS) manager.deleteNotificationChannelGroup(it.id)
        }
        ALL_CHANNEL_GROUPS.map { createChannelGroup(context, it) }.let {
            manager.createNotificationChannelGroups(it)
        }
    }

    private fun getPriority(importance: Int): Int{
        return when(importance){
            NotificationManagerCompat.IMPORTANCE_DEFAULT -> NotificationCompat.PRIORITY_DEFAULT
            NotificationManagerCompat.IMPORTANCE_HIGH -> NotificationCompat.PRIORITY_HIGH
            NotificationManagerCompat.IMPORTANCE_LOW -> NotificationCompat.PRIORITY_LOW
            NotificationManagerCompat.IMPORTANCE_MAX -> NotificationCompat.PRIORITY_MAX
            NotificationManagerCompat.IMPORTANCE_MIN -> NotificationCompat.PRIORITY_MIN
            else -> NotificationCompat.PRIORITY_DEFAULT
        }
    }

    /**
     * set priority for below android O
     */
    fun Builder(context: Context, channelId: String): NotificationCompat.Builder{
        val re = NotificationCompat.Builder(context, channelId)
        if (X.aboveOn(X.O)) return re
        return re.setPriority(getPriority(getImportance(channelId)))
    }
}
