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

package com.madness.collision.versatile

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.service.quicksettings.Tile
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.app.ActivityCompat
import com.madness.collision.R
import com.madness.collision.databinding.ActivityScannerBinding
import com.madness.collision.qs.TileCommon
import com.madness.collision.qs.TileServiceBarcodeScanner
import com.madness.collision.qs.TileServiceBarcodeScannerMm
import com.madness.collision.util.*
import com.madness.collision.util.ui.appLocale
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.*

internal class BarcodeScannerActivity: AppCompatActivity() {
    companion object {
        private const val permission_REQUEST_EXTERNAL_STORAGE = 1
        const val EXTRA_MODE = "ScannerMode"
        const val MODE_ALIPAY = 1
        const val MODE_WECHAT = 2
        private const val REQUEST_SCREEN_CAPTURING = 1
    }

    private var mode = MODE_ALIPAY
    private var dirCapture = ""
    private lateinit var viewBinding: ActivityScannerBinding

    private fun processCapture(bitmap: Bitmap){
        // write bitmap to a file
        val captureAffix = if (mode == MODE_WECHAT) {
            SimpleDateFormat(" yyyy-MM-dd HH:mm:ss", appLocale).format(Calendar.getInstance().time)
        } else ""
        val fileName = "capture$captureAffix.webp"
        val file : File?
        val uri: Uri?
        val isViaMediaStore = (mode == MODE_WECHAT) && X.aboveOn(X.Q)
        uri = if (isViaMediaStore) {
            file = null
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/webp")
            }
            if (X.aboveOn(X.Q)) {
                val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                contentResolver.insert(collection, values)
            } else null
        } else {
            file = File(F.createPath(dirCapture, fileName))
            file.getProviderUri(this@BarcodeScannerActivity)
        }
        uri ?: return

        try {
            if (isViaMediaStore) contentResolver.openOutputStream(uri) else FileOutputStream(file!!)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            null
        }?.use { fos ->
            val format = if (X.aboveOn(X.R)) Bitmap.CompressFormat.WEBP_LOSSY else webpLegacy
            bitmap.compress(format, P.WEBP_COMPRESS_SPACE_FIRST, fos)
            try {
                transfer(uri, file)
            } catch (e: Exception) {
                e.printStackTrace()
                notify(R.string.text_app_not_installed)
            } finally {
                bitmap.recycle()
            }
        }
    }

    @Suppress("deprecation")
    private val webpLegacy = Bitmap.CompressFormat.WEBP

    @SuppressLint("WrongConstant")
    private fun getTransferIntent(uri: Uri? = null): Intent{
        return when(mode){
            MODE_WECHAT -> {
                X.toast(this, getString(R.string.textSelectFromGallery), Toast.LENGTH_LONG)
                Intent().apply {
                    action = Intent.ACTION_VIEW
                    flags = 335544320
                    putExtra("LauncherUI.From.Scaner.Shortcut", true)
                    component = ComponentName("com.tencent.mm", "com.tencent.mm.ui.LauncherUI")
                }
            }
            else -> {
                Intent().apply {
                    action = Intent.ACTION_SEND
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                    type = "image/*"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    component = ComponentName("com.eg.android.AlipayGphone", "com.alipay.mobile.quinox.splash.ShareScanQRDispenseActivity")
                }
            }
        }
    }

    private fun transfer(uri: Uri? = null, file: File? = null){
        if (file != null && X.belowOff(X.Q)) MediaScannerConnection.scanFile(this, arrayOf(file.path), arrayOf("image/webp"), null)
        startActivity(getTransferIntent(uri))
    }

    override fun onCreate( savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pref = getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE)
        val themeId = ThemeUtil.updateTheme(this, pref, false)
        val themedContext = ContextThemeWrapper(this, themeId)
        viewBinding = ActivityScannerBinding.inflate(LayoutInflater.from(themedContext))
        setContentView(viewBinding.root)
        val colorSurface = ThemeUtil.getColor(themedContext, R.attr.colorASurface)
        viewBinding.scannerContent.background.setTint(colorSurface and 0xf0ffffff.toInt())
        val intent = intent
        if (intent == null){
            finish()
            return
        }
        mode = intent.getIntExtra(EXTRA_MODE, MODE_ALIPAY)
        dirCapture = when(mode){
            MODE_WECHAT -> if (X.aboveOn(X.Q)) "" else picDirLegacy()
            else -> F.createPath(F.cachePublicPath(this), Environment.DIRECTORY_PICTURES, "Captures")
        }
        val isViaMediaStore = mode == MODE_WECHAT && X.aboveOn(X.Q)
        if (!isViaMediaStore && !F.prepareDir(dirCapture)) {
            finish()
            return
        }
        if (mode == MODE_WECHAT && X.belowOff(X.Q)){
            val permissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            if (PermissionUtil.check(this, permissions).isNotEmpty()){
//            updateTileState()
                if (X.aboveOn(X.M)) {
                    ActivityCompat.requestPermissions(this, permissions, permission_REQUEST_EXTERNAL_STORAGE)
                } else {
                    notifyBriefly(R.string.toast_permission_storage_denied)
                }
            } else startProjection()
        } else startProjection()
    }

    @Suppress("deprecation")
    private fun picDirLegacy(): String {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)?.path ?: ""
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            permission_REQUEST_EXTERNAL_STORAGE -> {
                if (grantResults.isEmpty()) return
                when (grantResults[0]) {
                    PackageManager.PERMISSION_GRANTED -> startProjection()
                    else -> notifyBriefly(R.string.toast_permission_storage_denied)
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_SCREEN_CAPTURING || data == null) {
            finish()
            return
        }
        val intent = Intent(this, ScreenCapturingService::class.java).apply {
            putExtra(ScreenCapturingService.ARG_RESULT_CODE, resultCode)
            putExtra(ScreenCapturingService.ARG_DATA, data)
        }
        ScreenCapturingService.uiContext = WeakReference(this)
        startService(intent)
        GlobalScope.launch {
            val time = System.currentTimeMillis()
            var bmp: Bitmap? = null
            while (bmp == null) {
                if (System.currentTimeMillis() - time > 10000) break
                bmp = ScreenCapturingService.capture
                delay(200)
            }
            val capture = bmp ?: return@launch
            processCapture(capture)
            ScreenCapturingService.capture = null
            finish()
        }
    }

    private fun startProjection() {
        val mProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(mProjectionManager.createScreenCaptureIntent(), REQUEST_SCREEN_CAPTURING)
    }

    private fun updateTileState(){
        if (X.aboveOn(X.N)){
            // update tile
            val tileService: TileCommon = when(mode){
                MODE_WECHAT -> TileServiceBarcodeScannerMm.INSTANCE.get()!!
                else -> TileServiceBarcodeScanner.INSTANCE.get()!!
            }
            tileService.qsTile.run {
                icon = tileService.iconIdle
                state = Tile.STATE_INACTIVE
                updateTile()
            }
        }
    }
}
