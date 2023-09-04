/*
 * Copyright 2023 Clifford Liu
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

package com.madness.collision.main

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import com.madness.collision.BuildConfig
import com.madness.collision.util.dev.idString

class MainLifecycleCallbacks : Application.ActivityLifecycleCallbacks {
    private val TAG = "MainLifecycle"
    private val Activity.id: String get() = getDebugId()

    private fun Activity.getDebugId(): String {
        if (this is MainPageActivity) {
            val pageClass = intent?.getStringExtra(MainPageActivity.ARG_PAGE)
            if (pageClass != null) {
                val pageName = pageClass.replaceFirst(BuildConfig.BUILD_PACKAGE, "")
                return "page@$idString/$pageName"
            }
            val pendingPage = MainPageActivity.peek()
            if (pendingPage != null) {
                val pageName = pendingPage::class.qualifiedName
                    ?.replaceFirst(BuildConfig.BUILD_PACKAGE, "")
                return "page@$idString/$pageName@${pendingPage.idString}"
            }
            return "page@$idString"
        }
        return "${this::class.simpleName}@$idString"
    }

    override fun onActivityCreated(p0: Activity, p1: Bundle?) {
        Log.d(TAG, "created/${p0.id}")
    }

    override fun onActivityStarted(p0: Activity) {
    }

    override fun onActivityResumed(p0: Activity) {
        Log.d(TAG, "resumed/${p0.id}")
    }

    override fun onActivityPaused(p0: Activity) {
    }

    override fun onActivityStopped(p0: Activity) {
    }

    override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {
    }

    override fun onActivityDestroyed(p0: Activity) {
    }
}