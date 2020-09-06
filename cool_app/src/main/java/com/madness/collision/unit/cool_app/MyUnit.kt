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

package com.madness.collision.unit.cool_app

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.edit
import com.madness.collision.settings.SettingsFunc
import com.madness.collision.unit.Unit
import com.madness.collision.util.P
import com.madness.collision.util.SystemUtil
import com.madness.collision.versatile.AppInfoWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import com.madness.collision.R as MainR

class MyUnit: Unit() {

    override val id: String = "CA"

    override fun createOptions(context: Context, toolbar: Toolbar, iconColor: Int): Boolean {
        toolbar.setTitle(MainR.string.developertools_appinfowidget)
        return true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val context = context
        if (context != null) SettingsFunc.updateLanguage(context)
        return inflater.inflate(R.layout.unit_ca, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        democratize()
        val prefSettings = activity?.getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE)
        val packageName = prefSettings?.getString(
                P.APP_INFO_PACKAGE,
                P.APP_INFO_PACKAGE_DEFAULT)
                ?: P.APP_INFO_PACKAGE_DEFAULT
        sync(packageName)
        val view = view ?: return
        val focus: View = view.findViewById(R.id.app_info_focus)
        focus.requestFocus()
        val etName: EditText = view.findViewById(R.id.app_info_name)
        etName.setText(packageName)
        etName.setSelectAllOnFocus(true)
        etName.setOnEditorActionListener{ v, actionId, event ->
            if ((event != null && event.keyCode == KeyEvent.KEYCODE_ENTER) ||
                    actionId == EditorInfo.IME_ACTION_DONE) {
                if (v.text == null || v.text.toString().isEmpty()) {
                    return@setOnEditorActionListener false
                }
                focus.requestFocus()
                val context = context ?: return@setOnEditorActionListener true
                val window = activity?.window ?: return@setOnEditorActionListener true
                SystemUtil.hideImeCompat(context, v, window)
                val tvRating: TextView = view.findViewById(R.id.caRating)
                val tvDownloads: TextView = view.findViewById(R.id.app_info_downloads)
                val tvFlowers: TextView = view.findViewById(R.id.app_info_flowers)
                val tvComments: TextView = view.findViewById(R.id.app_info_comments)
                val progressBar: ProgressBar = view.findViewById(R.id.app_info_progress)
                tvComments.visibility = View.GONE
                tvFlowers.visibility = View.GONE
                tvDownloads.visibility = View.GONE
                tvRating.visibility = View.GONE
                progressBar.visibility = View.VISIBLE
                sync(v.text.toString())
                prefSettings?.edit { putString(P.APP_INFO_PACKAGE, v.text.toString()) }
                return@setOnEditorActionListener true
            }
            false
        }
    }

    private fun sync(packageName: String){
        lateinit var app: CoolApp
        GlobalScope.launch {
            app = CoolApp(packageName)
            app.retrieve()
        }.invokeOnCompletion {
            val view = view ?: return@invokeOnCompletion
            val tvRating: TextView = view.findViewById(R.id.caRating)
            val tvDownloads: TextView = view.findViewById(R.id.app_info_downloads)
            val tvFlowers: TextView = view.findViewById(R.id.app_info_flowers)
            val tvComments: TextView = view.findViewById(R.id.app_info_comments)
            val progressBar: ProgressBar = view.findViewById(R.id.app_info_progress)
            val vLogo: ImageView = view.findViewById(R.id.appInfoLogo)
            if (app.isNotHealthy()){
                GlobalScope.launch(Dispatchers.Main) {
                    tvDownloads.setText(MainR.string.text_parse_fails)
                    tvDownloads.visibility = View.VISIBLE
                    progressBar.visibility = View.GONE
                }
                return@invokeOnCompletion
            }
            val infoRating = String.format("%.1f", app.rating)
            val infoDownloads = String.format("%.0f", app.countDownloads)
            val infoFlowers = String.format("%.0f", app.countFlowers)
            val infoComments = String.format("%.0f", app.countComments)
            GlobalScope.launch(Dispatchers.Main) {
                tvRating.text = infoRating
                tvDownloads.text = infoDownloads
                tvFlowers.text = infoFlowers
                tvComments.text = infoComments
                vLogo.setImageBitmap(app.logo)
                progressBar.visibility = View.GONE
                tvRating.visibility = View.VISIBLE
                tvDownloads.visibility = View.VISIBLE
                tvFlowers.visibility = View.VISIBLE
                tvComments.visibility = View.VISIBLE
            }
            // update widgets
            val intent = Intent(activity, AppInfoWidget::class.java)
            intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            val context = context ?: return@invokeOnCompletion
            val comp = ComponentName(context, AppInfoWidget::class.java)
            val ids = AppWidgetManager.getInstance(activity).getAppWidgetIds(comp)
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            context.sendBroadcast(intent)
        }
    }
}
