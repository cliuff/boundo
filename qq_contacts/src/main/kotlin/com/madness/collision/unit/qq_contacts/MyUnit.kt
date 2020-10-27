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

package com.madness.collision.unit.qq_contacts

import android.annotation.TargetApi
import android.content.Context
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.Toolbar
import com.google.android.material.card.MaterialCardView
import com.madness.collision.R
import com.madness.collision.databinding.InstantQqItemBinding
import com.madness.collision.instant.Instant
import com.madness.collision.settings.SettingsFunc
import com.madness.collision.unit.Unit
import com.madness.collision.util.*
import kotlinx.android.synthetic.main.activity_instant_qq_manager.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.roundToInt
import com.madness.collision.unit.qq_contacts.R as MyR

class MyUnit : Unit() {

    override val id: String = "QC"

    companion object {
        private const val KEY_CONTACT = MyR.bool.instantQqItemContact
        private const val KEY_SELECTION = MyR.bool.instantQqItemIsSelected
    }

    private var selectionCount = 0
    private lateinit var instant: Instant

    override fun createOptions(context: Context, toolbar: Toolbar, iconColor: Int): Boolean {
        toolbar.setTitle(R.string.unit_qq_contacts)
        inflateAndTint(MyR.menu.toolbar_instant_qq_manager, toolbar, iconColor)
        return true
    }

    override fun selectOption(item: MenuItem): Boolean {
        when (item.itemId){
            MyR.id.instantQqManagerTbAdd -> {
                QqFragment().let { mainViewModel.displayFragment(it) }
                return true
            }
            MyR.id.instantQqManagerTbRemove -> {
                val context = context ?: return false
                if (instantQqList.childCount == 0){
                    notifyBriefly(R.string.text_no_content)
                    return true
                }
                if (selectionCount < 1){
                    CollisionDialog(context, R.string.text_cancel, R.string.text_OK, true).apply {
                        setListener({ dismiss() }, {
                            dismiss()
                            GlobalScope.launch { removeAll(context) }
                        })
                        setTitleCollision(0, 0, 0)
                        setContent(MyR.string.qc_manager_remove_all)
                    }.show()
                    return true
                }
                val list = mutableListOf<View>()
                val contacts = mutableListOf<QqContact>()
                for(i in 0 until instantQqList.childCount){
                    val v = instantQqList.getChildAt(i)
                    val isSelected = v.getTag(KEY_SELECTION) as Boolean? ?: false
                    if (!isSelected) continue
                    list.add(v)
                }
                list.forEach { view ->
                    contacts.add(view.getTag(KEY_CONTACT) as QqContact)
                    instantQqList.post { instantQqList.removeView(view) }
                    selectionCount --
                }
                GlobalScope.launch { removeData(context, *contacts.toTypedArray()) }
                return true
            }
        }
        return false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity?.let { SettingsFunc.updateLanguage(it) }
        return inflater.inflate(MyR.layout.activity_instant_qq_manager, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val context = context ?: return
        democratize()
        mainViewModel.contentWidthTop.observe(viewLifecycleOwner) {
            instantQqListContainer.alterPadding(top = it)
        }
        if (X.aboveOn(X.N_MR1)) {
            val shortcutManager: ShortcutManager = context.getSystemService(ShortcutManager::class.java) ?: return
            instant = Instant(context, shortcutManager)
            updateList(context)
        } else {
            mainViewModel.popUpBackStack(true)
        }
    }

    @TargetApi(Build.VERSION_CODES.N_MR1)
    private fun removeData(context: Context, vararg contacts: QqContact){
        contacts.forEach {
            instant.removeDynamicShortcuts(it.shortcutId)
            File(it.getProfilePhotoPath(context)).run {
                if (this.exists()) this.delete()
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.N_MR1)
    private fun removeAll(context: Context){
        instantQqList.post { instantQqList.removeAllViews() }
        instant.dynamicShortcuts.filter {
            it.id.startsWith(QqContact.SHORTCUT_ID_PREFIX)
        }.map { it.id }.let {
            instant.removeDynamicShortcuts(*it.toTypedArray())
        }
        X.deleteFolder(File(QqContact.getProfilePhotoFolderPath(context)))
    }

    @RequiresApi(api = Build.VERSION_CODES.N_MR1)
    private fun updateList(context: Context) {
        selectionCount = 0
        instantQqList.removeAllViews()
        val dynamicShortcuts = instant.dynamicShortcuts
        if (dynamicShortcuts.isEmpty()) return
        val color = ThemeUtil.getColor(context, R.attr.colorAPrimary)
        val colorSelected = ThemeUtil.getBackColor(color, 0.3f)
        val colorNormal = Color.TRANSPARENT
        val onClickListener = View.OnClickListener {
            val isSelected = it.getTag(KEY_SELECTION) as Boolean? ?: false
            val isSelecting = selectionCount > 0
            when {
                isSelected -> {
                    selectionCount --
                    (it as MaterialCardView).setCardBackgroundColor(colorNormal)
                    it.setTag(KEY_SELECTION, false)
                }
                isSelecting -> {
                    selectionCount ++
                    (it as MaterialCardView).setCardBackgroundColor(colorSelected)
                    it.setTag(KEY_SELECTION, true)
                }
                else -> {
                    val f = QqFragment.newInstance(it.getTag(KEY_CONTACT) as QqContact)
                    mainViewModel.displayFragment(f)
                }
            }
        }
        val onLongClickListener = View.OnLongClickListener {
            val isSelected = it.getTag(KEY_SELECTION) as Boolean? ?: false
            if (isSelected) return@OnLongClickListener false
            selectionCount ++
            (it as MaterialCardView).setCardBackgroundColor(colorSelected)
            it.setTag(KEY_SELECTION, true)
            true
        }
        val profilePhotoWidth = X.size(context, 50f, X.DP).roundToInt()
        for (s in dynamicShortcuts) {
            if (!s.id.startsWith(QqContact.SHORTCUT_ID_PREFIX)) continue
            val mViews = InstantQqItemBinding.inflate(layoutInflater, instantQqList, true)
            val card = mViews.instantQqItem
            val textView = mViews.instantQqItemText
            val contact = QqContact.fromShortcut(s)
            textView.text = contact.name
            val profilePhoto = contact.getProfilePhoto(context)
            val round = if (profilePhoto != null) X.circularBitmap(X.toTarget(profilePhoto, profilePhotoWidth, profilePhotoWidth))
            else Bitmap.createBitmap(profilePhotoWidth, profilePhotoWidth, Bitmap.Config.ARGB_8888)
            val drawable = BitmapDrawable(resources, round)
            drawable.setBounds(0, 0, profilePhotoWidth, profilePhotoWidth)
            textView.setCompoundDrawablesRelative(drawable, null, null, null)
            card.setTag(KEY_CONTACT, contact)
            card.setOnClickListener(onClickListener)
            card.setOnLongClickListener(onLongClickListener)
        }
    }

}
