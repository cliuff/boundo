/*
 * Copyright 2021 Clifford Liu
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

package com.madness.collision.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.drawable.AnimatedVectorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.content.edit
import androidx.fragment.app.activityViewModels
import com.madness.collision.Democratic
import com.madness.collision.R
import com.madness.collision.main.MainViewModel
import com.madness.collision.util.*

internal class AdviceFragment : TaggedFragment(), Democratic, View.OnClickListener, NavNode {

    override val category: String = "Advice"
    override val id: String = "Advice"
    
    private lateinit var background: View
    private var count4DebugMode = 0
    private val mainViewModel: MainViewModel by activityViewModels()
    override val nodeDestinationId: Int = R.id.adviceFragment

    override fun createOptions(context: Context, toolbar: Toolbar, iconColor: Int): Boolean {
        mainViewModel.configNavigation(toolbar, iconColor)
        toolbar.setTitle(R.string.Main_TextView_Advice_Text)
        return true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_advice, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val view = view ?: return
        democratize(mainViewModel)

        background = view.findViewById(R.id.advice_display_background)

        val vLogo = view.findViewById<ImageView>(R.id.adviceLogo)
        (vLogo.drawable as AnimatedVectorDrawable).start()
        arrayOf(
                view.findViewById(R.id.adviceDerive),
                view.findViewById(R.id.adviceLicense),
                view.findViewById(R.id.adviceSourceCode),
                view.findViewById(R.id.adviceTranslation),
                vLogo as View
        ).forEach { it.setOnClickListener(this) }
    }

    override fun onClick(view: View) {
        val context = context ?: return
        when (view.id) {
            R.id.adviceDerive ->{
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clipData = clipboard.primaryClip
                if (clipData == null) {
                    notifyBriefly(R.string.text_no_content)
                    return
                }
                val builder = StringBuilder()
                for (i in 0 until clipData.itemCount) builder.append(clipData.getItemAt(i).htmlText)
                clipboard.setPrimaryClip(ClipData.newPlainText("text", builder.toString()))
                notifyBriefly(R.string.text_done)
            }
            R.id.adviceLicense -> {
                navigate(AdviceFragmentDirections.actionAdviceFragmentToOssActivity())
            }
            R.id.adviceSourceCode -> {
                val intent = Intent().apply {
                    action = Intent.ACTION_VIEW
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    data = Uri.parse(P.LINK_SOURCE_CODE)
                }
                startActivity(intent)
            }
            R.id.adviceTranslation -> {
                val intent = Intent().apply {
                    action = Intent.ACTION_VIEW
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    data = Uri.parse(P.LINK_TRANSLATION_PROGRAM)
                }
                startActivity(intent)
            }
            R.id.adviceLogo -> {
                count4DebugMode++
                if (count4DebugMode != 6) return
                count4DebugMode = 0
                val pref = context.getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE)
                mainApplication.debug = !mainApplication.debug
                pref.edit { putBoolean(P.ADVANCED, mainApplication.debug) }
                X.toast(context, getString(R.string.Advice_Switch_Debug_Text) + ": O" + if (mainApplication.debug) "n" else "ff", Toast.LENGTH_LONG)
            }
        }
    }
}