/*
 * Copyright 2022 Clifford Liu
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

package com.madness.collision.unit.api_viewing.list

import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import coil.load
import com.madness.collision.misc.MiscApp
import com.madness.collision.R
import com.madness.collision.unit.api_viewing.ApiViewingViewModel
import com.madness.collision.unit.api_viewing.R as MyR
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.data.AppPackageInfo
import com.madness.collision.util.CollisionDialog
import com.madness.collision.util.X
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class AppInfoService {

    companion object {
        private const val packageCoolApk = ApiViewingApp.packageCoolApk
        private const val packagePlayStore = ApiViewingApp.packagePlayStore
        private const val packageSettings = "com.android.settings"

        private var initializedStoreLink = false
        private val storeMap = mutableMapOf<String, ApiViewingApp>()

        fun clearStores() {
            initializedStoreLink = false
            storeMap.clear()
        }
    }

    private var popStore: CollisionDialog? = null

    fun actionStores(fragment: DialogFragment, app: ApiViewingApp, scope: CoroutineScope) {
        val context = fragment.context ?: return
        fragment.actionStores(context, app, scope)
    }

    private fun DialogFragment.actionStores(context: Context, app: ApiViewingApp, scope: CoroutineScope) {
        if (!initializedStoreLink) {
            scope.launch(Dispatchers.Default) {
                if (!initStores(context)) return@launch
                launch(Dispatchers.Main) {
                    // dismiss after viewModel access in initStores
                    dismiss()
                    showStores(context, app)
                }
            }
        } else {
            dismiss()
            showStores(context, app)
        }
    }

    private fun Fragment.initStores(context: Context): Boolean {
        // by activityViewModels() may produce IllegalArgumentException due to getActivity null value
        // by activityViewModels() may produce IllegalStateException[java.lang.IllegalStateException: Can't access ViewModels from detached fragment]
        // due to fragment being detached?
        if (activity == null || isDetached || !isAdded) {
            X.toast(context, R.string.text_error, Toast.LENGTH_SHORT)
            return false
        }
        initializedStoreLink = true
        val searchSize = 3
        val avViewModel: ApiViewingViewModel by activityViewModels()
        // find from loaded apps
        val filtered = avViewModel.findApps(searchSize) {
            val pn = it.packageName
            pn == packageCoolApk || pn == packagePlayStore || pn == packageSettings
        }
        for (listApp in filtered) {
            storeMap[listApp.packageName] = listApp
        }
        // manually initialize those not found in loaded apps
        for (name in arrayOf(packageCoolApk, packagePlayStore, packageSettings)) {
            if (storeMap[name] != null) continue
            val pi = MiscApp.getPackageInfo(context, packageName = name) ?: continue
            storeMap[name] = ApiViewingApp(context, pi, preloadProcess = true, archive = false)
        }
        return true
    }

    private fun showStores(context: Context, app: ApiViewingApp) {
        val vCoolApk : ImageView
        val vPlayStore : ImageView
        val vSettings: ImageView
        popStore = CollisionDialog(context, R.string.text_forgetit).apply {
            setListener { dismiss() }
            setTitleCollision(R.string.avStoreLink, 0, 0)
            setContent(0)
            setCustomContent(MyR.layout.pop_av_store_link)
            vCoolApk = findViewById(MyR.id.vCoolApk)
            vPlayStore = findViewById(MyR.id.vPlayStore)
            vSettings = findViewById(MyR.id.vSettings)
        }
        val iconWidth = X.size(context, 60f, X.DP).roundToInt()
        val storePackages = arrayOf(packageCoolApk to storeMap[packageCoolApk],
            packagePlayStore to storeMap[packagePlayStore])
        for ((name, storeIcon) in storePackages) {
            var listener: View.OnClickListener? = null
            if (storeIcon != null) {
                listener = View.OnClickListener {
                    try {
                        context.startActivity(app.storePage(name, true))
                    } catch (e: Exception) {
                        e.printStackTrace()
                        context.startActivity(app.storePage(name, false))
                    }
                    popStore?.dismiss()
                }
            }
            when(name) {
                packageCoolApk -> vCoolApk
                packagePlayStore -> vPlayStore
                else -> null
            }?.run {
                if (storeIcon == null) {
                    setPadding(0, 0, 0, 0)
                } else {
                    load(AppPackageInfo(context, storeIcon))
                    setOnClickListener(listener)
                }
            }
        }

        if (app.isNotArchive || X.belowOff(X.Q)) {
            val settingsIcon = storeMap[packageSettings]
            if (settingsIcon != null) vSettings.load(AppPackageInfo(context, settingsIcon))
            else vSettings.load(R.mipmap.logo_settings)
            vSettings.setOnClickListener {
                if (app.isNotArchive) {
                    context.startActivity(app.settingsPage())
                } else {
                    try {
                        context.startActivity(app.apkPage())
                    } catch (e: Exception) {
                        e.printStackTrace()
                        CollisionDialog.infoCopyable(context, app.appPackage.basePath).show()
                    }
                }
                popStore?.dismiss()
            }
        } else {
            vSettings.setPadding(0, 0, 0, 0)
        }
        popStore?.show()
    }
}
