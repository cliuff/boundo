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

package com.madness.collision.util.notice

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.madness.collision.R
import com.madness.collision.util.CollisionDialog
import com.madness.collision.util.mainApplication
import com.madness.collision.util.os.OsUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ToastUtils {
    suspend fun toast(context: Context, messageRes: Int, duration: Int) {
        toast(context, context.getString(messageRes), duration)
    }

    suspend fun toast(context: Context, message: CharSequence, duration: Int) = withContext(Dispatchers.Main) {
        if (!mainApplication.notificationAvailable) {
            if (context is Activity) popRequestNotification(context)
            else popNotifyNotification(context)
        }
        Toast.makeText(context, message, duration).show()
    }

    fun popRequestNotification(activity: Activity) {
        val packageName = activity.packageName
        val intent = Intent().apply {
            action = "android.settings.APP_NOTIFICATION_SETTINGS"
            if (OsUtils.satisfy(OsUtils.O)) {
                putExtra("android.provider.extra.APP_PACKAGE", packageName)
            } else {
                putExtra("app_package", packageName)
                putExtra("app_uid", activity.applicationInfo.uid)
            }
        }
        CollisionDialog(activity, R.string.textGrantPermission).apply {
            setTitleCollision(0, 0, 0)
            setContent(R.string.textAsk4Notification)
            setListener {
                dismiss()
                activity.startActivity(intent)
            }
        }.show()
    }

    fun popNotifyNotification(context: Context) {
        CollisionDialog.alert(context, R.string.textAsk4Notification).show()
    }
}