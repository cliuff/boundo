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

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.ProgressBar
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.madness.collision.Democratic
import com.madness.collision.R
import com.madness.collision.main.MainViewModel
import com.madness.collision.settings.SettingsFunc
import com.madness.collision.util.*
import kotlinx.android.synthetic.main.activity_instant_qq.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import kotlin.math.roundToInt
import com.madness.collision.unit.qq_contacts.R as MyR

internal class QqFragment : Fragment(), Democratic {
    companion object{
        private const val REQUEST_LOAD_IMAGE = 10
        private const val ARG_CONTACT = "argContact"

        @JvmStatic
        fun newInstance(contact: QqContact) : QqFragment{
            val b = Bundle().apply {
                putParcelable(ARG_CONTACT, contact)
            }
            return QqFragment().apply { arguments = b }
        }
    }

    private var avatar: Bitmap? = null
    private var circularAvatar: Bitmap? = null
    private val mainViewModel: MainViewModel by activityViewModels()

    private var is4Update = false
    private lateinit var contact: QqContact

    override fun createOptions(context: Context, toolbar: Toolbar, iconColor: Int): Boolean {
        toolbar.setTitle(R.string.unit_qq_contacts)
        toolbar.inflateMenu(R.menu.toolbar_done)
        toolbar.menu.findItem(R.id.tbDone).icon.setTint(ThemeUtil.getColor(context, R.attr.colorActionPass))
        return true
    }

    override fun selectOption(item: MenuItem): Boolean {
        when (item.itemId){
            R.id.tbDone -> {
                val context = context ?: return false
                actionDone(context)
                return true
            }
        }
        return false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val context = context
        if (context != null) SettingsFunc.updateLanguage(context)
        return inflater.inflate(MyR.layout.activity_instant_qq, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val context = context ?: return

        democratize(mainViewModel)

        val extras: Bundle? = arguments
        contact = extras?.getParcelable(ARG_CONTACT) ?: QqContact("")
        is4Update = extras != null
        if (is4Update){
            instantQqId.isFocusable = false
            instantQqId.setTextColor(Color.GRAY)
            instantQqId.setText(contact.no)
            instantQqLabelLong.setText(contact.name)
            instantQqLabelLong.setSelectAllOnFocus(true)
            instantQqLabelShort.setText(contact.miniName)
            instantQqLabelShort.setSelectAllOnFocus(true)
        }
        contact.getProfilePhoto(context)?.let {
            avatar = it
            setCircularAvatar(context)
        } ?: setAvatarDefault(context)
        instantQqLabelShort.setOnEditorActionListener { _, actionId, event ->
            if ((event != null && event.keyCode == KeyEvent.KEYCODE_ENTER) || actionId == EditorInfo.IME_ACTION_DONE) {
                actionDone(context)
                return@setOnEditorActionListener true
            }
            false
        }

        instantQqProfile.setOnClickListener { actionGetImage() }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val context = context ?: return
        if (requestCode == REQUEST_LOAD_IMAGE && resultCode == AppCompatActivity.RESULT_OK && data != null){
            val selectedImage = data.data ?: return
            MemoryManager.clearSpace(activity)
            try {
                avatar = ImageUtil.getBitmap(context, selectedImage)
            } catch ( e: IOException) {
                e.printStackTrace()
                notify(MyR.string.launcher_qqcontacts_getavatar_toast_fail)
                return
            }
            avatar = X.toSquare(avatar!!).collisionBitmap

            setCircularAvatar(context)
        }
    }

    private fun setAvatarDefault(context: Context){
        val imgGallery = context.getDrawable(R.drawable.img_gallery) ?: return
        avatar = X.drawableToBitmap(imgGallery)
        circularAvatar = avatar
        instantQqProfile.setImageBitmap(avatar)
    }

    private fun setCircularAvatar(context: Context){
        circularAvatar = X.circularBitmap(avatar!!)
        val tg200dp = X.size(context, 200f, X.DP).roundToInt()
        instantQqProfile.setImageBitmap(X.toMax(circularAvatar!!, tg200dp))
    }

    private fun actionDone(context: Context){
        if (X.belowOff(X.N_MR1)) return
        val inId = instantQqId.text.toString()
        val inLabelLong = instantQqLabelLong.text.toString()
        val inLabelShort = instantQqLabelShort.text.toString()
        if (inId.isEmpty() || inLabelLong.isEmpty() || inLabelShort.isEmpty()) {
            notify(MyR.string.launcher_qqcontacts_toast_input_notcomplete)
            return
        }
        val popLoading = CollisionDialog.loading(context, ProgressBar(context))
        popLoading.show()
        GlobalScope.launch {
            val newContact = if (is4Update) contact else QqContact(inId)
            newContact.name = inLabelLong
            newContact.miniName = inLabelShort
            processContact(context, newContact)
        }.invokeOnCompletion {
            popLoading.dismiss()
            notifyBriefly(MyR.string.Launcher_QQContacts_Toast_Done_Text)
            GlobalScope.launch(Dispatchers.Main) {
                mainViewModel.popUpBackStack(true)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    private fun processContact(context: Context, newContact: QqContact){
        val shortcutManager: ShortcutManager? = context.getSystemService(ShortcutManager::class.java)
        if (shortcutManager == null){
            CollisionDialog.alert(context, R.string.text_error).show()
            return
        }
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse("mqqwpa://im/chat?chat_type=wpa&uin=${newContact.no}&version=1")
        val builder = ShortcutInfo.Builder(context, newContact.shortcutId)
                .setShortLabel(newContact.miniName).setLongLabel(newContact.name).setIntent(intent)
        avatar?.let {
            if (X.aboveOn(X.O)) {
                val blurred = X.blurBitmap(context, it, (it.width * 1.48).roundToInt(), (it.height * 1.48).roundToInt())
                val icon = Bitmap.createBitmap(blurred)
                Canvas(icon).drawBitmap(it, (it.width * 0.24).toFloat(), (it.height * 0.24).toFloat(), Paint(Paint.ANTI_ALIAS_FLAG))
                builder.setIcon(Icon.createWithAdaptiveBitmap(icon))
            } else {
                builder.setIcon(Icon.createWithBitmap(circularAvatar))
            }
        }
        Collections.singletonList(builder.build()).let {
            if (is4Update) shortcutManager.updateShortcuts(it) else shortcutManager.addDynamicShortcuts(it)
        }

        //save avatar image to storage
        avatar?.let {
            val path = newContact.getProfilePhotoPath(context)
            if (F.prepare4(path)){
                try {
                    it.compress(Bitmap.CompressFormat.WEBP, P.WEBP_COMPRESS_SPACE_FIRST, FileOutputStream(path))
                } catch ( e: FileNotFoundException) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun actionGetImage(){
        val getImage = Intent(Intent.ACTION_GET_CONTENT)
        getImage.type = "image/*"
        startActivityForResult(getImage, REQUEST_LOAD_IMAGE)
    }
}