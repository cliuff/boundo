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

package com.madness.collision.unit.image_modifying

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.heifwriter.HeifWriter
import com.madness.collision.R
import com.madness.collision.databinding.UnitImBinding
import com.madness.collision.unit.Unit
import com.madness.collision.util.*
import com.madness.collision.util.ui.appLocale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.*
import kotlin.math.roundToInt

class MyUnit: Unit(){

    override val id: String = "IM"

    companion object {
        const val REQUEST_GET_IMAGE = 100
        const val permission_REQUEST_EXTERNAL_STORAGE = 200
    }

    private var sampleImage: Bitmap? = null
    private var imageGetter: (() -> Pair<String, Bitmap>?)? = null
    private var previewSize = 0
    private lateinit var viewBinding: UnitImBinding

    override fun createOptions(context: Context, toolbar: Toolbar, iconColor: Int): Boolean {
        configNavigation(toolbar, iconColor)
        toolbar.setTitle(R.string.developertools_cropimage)
        toolbar.inflateMenu(R.menu.toolbar_im)
        toolbar.menu.findItem(R.id.imToolbarDone).icon?.setTint(ThemeUtil.getColor(context, R.attr.colorActionPass))
        return true
    }

    override fun selectOption(item: MenuItem): Boolean {
        when (item.itemId){
            R.id.imToolbarDone -> {
                val context = context ?: return false
                actionDone(context)
                return true
            }
        }
        return false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewBinding = UnitImBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val context = context ?: return
        democratize()
        mainViewModel.contentWidthTop.observe(viewLifecycleOwner) {
            viewBinding.imageContainer.alterPadding(top = it)
        }
        viewBinding.imagePreview.setOnClickListener{
            val getImage = Intent(Intent.ACTION_GET_CONTENT)
            getImage.type = "image/*"
            startActivityForResult(getImage, REQUEST_GET_IMAGE)
        }
        previewSize = X.size(context, 200f, X.DP).roundToInt()
        setDefaultImage(context)
        val formatItems: Array<String> = if (X.aboveOn(X.P)) {
            arrayOf("png", "jpg", "webp", "heif")
        } else {
            arrayOf("png", "jpg", "webp")
        }
        viewBinding.toolsImageFormat.setText(formatItems[0])
        viewBinding.toolsImageFormat.dropDownBackground.setTint(ThemeUtil.getColor(context, R.attr.colorASurface))
        viewBinding.toolsImageFormat.setAdapter(ArrayAdapter(context, R.layout.pop_list_item, formatItems))
        val appLocale = appLocale
        val initialFraction = String.format(appLocale, "%d/%d", 0, 100)
        (viewBinding.imageBlurValue as AppCompatTextView).text = initialFraction
        (viewBinding.imageCompressValue as AppCompatTextView).text = initialFraction
        val onSeek = object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                val v = when(p0?.id){
                    R.id.imageBlur -> viewBinding.imageBlurValue
                    R.id.imageCompress -> viewBinding.imageCompressValue
                    else -> null
                } as AppCompatTextView? ?: return
                v.text = String.format(appLocale, "%d/%d", p1, 100)
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        }
        viewBinding.imageBlur.setOnSeekBarChangeListener(onSeek)
        viewBinding.imageCompress.setOnSeekBarChangeListener(onSeek)
    }

    private fun setDefaultImage(context: Context) {
        (viewBinding.imagePreview.layoutParams as FrameLayout.LayoutParams).run {
            width = previewSize
            height = previewSize
        }
        viewBinding.imagePreview.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.img_gallery))
        viewBinding.imageCard.cardElevation = 0f
        viewBinding.imageEditWidth.setText("")
        viewBinding.imageEditHeight.setText("")
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            permission_REQUEST_EXTERNAL_STORAGE -> {
                if (grantResults.isEmpty()) return
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    val context = context ?: return
                    actionDone(context)
                } else {
                    notifyBriefly(R.string.toast_permission_storage_denied)
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val context = context ?: return
        val activity = activity ?: return
        if (requestCode == REQUEST_GET_IMAGE && resultCode == Activity.RESULT_OK && data != null) {
            GlobalScope.launch {
                val dataUri = data.data ?: return@launch
                MemoryManager.clearSpace(activity)
                sampleImage = null
                launch(Dispatchers.Main) {
                    viewBinding.imagePreview.setImageDrawable(null)
                }
                try {
                    sampleImage = ImageUtil.getSampledBitmap(context, dataUri, previewSize, previewSize)
                } catch (e: OutOfMemoryError) {
                    e.printStackTrace()
                    notifyBriefly(R.string.text_error)
                    return@launch
                } catch (e: Exception) {
                    e.printStackTrace()
                    notifyBriefly(R.string.text_error)
                    return@launch
                }
                imageGetter = {
                    val imageName: String
                    var nameCursor: Cursor? = null
                    if (data.data != null) {
                        nameCursor = context.contentResolver.query(data.data!!, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                    }
                    if (nameCursor != null) {
                        nameCursor.moveToFirst()
                        imageName = nameCursor.getString(0)
                        nameCursor.close()
                    } else {
                        imageName = ""
                    }
                    try {
                        ImageUtil.getBitmap(context, dataUri)?.run {
                            imageName to this
                        }
                    } catch (e: OutOfMemoryError) {
                        e.printStackTrace()
                        notifyBriefly(R.string.text_error)
                        null
                    } catch (e: Exception) {
                        e.printStackTrace()
                        notifyBriefly(R.string.text_error)
                        null
                    }
                }

                showImage(context)
            }
        }
    }

    private fun showImage(context: Context) {
        val image = sampleImage ?: return
        GlobalScope.launch {
            val targetImage = X.toMax(image, previewSize)
            val elevation = X.size(context, 4f, X.DP)
            launch(Dispatchers.Main) {
                (viewBinding.imagePreview.layoutParams as FrameLayout.LayoutParams).run {
                    width = ViewGroup.LayoutParams.WRAP_CONTENT
                    height = ViewGroup.LayoutParams.WRAP_CONTENT
                }
                viewBinding.imagePreview.setImageBitmap(targetImage)
                viewBinding.imageCard.cardElevation = elevation
                viewBinding.imageEditWidth.setText(image.width.toString())
                viewBinding.imageEditHeight.setText(image.height.toString())
            }
        }
    }

    private fun actionDone(context: Context){
        if (X.belowOff(X.Q)){
            val permissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            if (PermissionUtil.check(context, permissions).isNotEmpty()) {
                if (X.aboveOn(X.M)) {
                    requestPermissions(permissions, permission_REQUEST_EXTERNAL_STORAGE)
                } else {
                    notifyBriefly(R.string.toast_permission_storage_denied)
                }
                return
            }
        }

        val popLoading = CollisionDialog.loading(context, ProgressBar(context))
        popLoading.show()
        GlobalScope.launch {
            sampleImage = null
            launch(Dispatchers.Main) {
                viewBinding.imagePreview.setImageDrawable(null)
            }
            val (imageName, image) = imageGetter?.invoke() ?: "" to null
            val isSuccessful = if (image != null) {
                try {
                    processImage(context, image).run { saveImage(context, this, imageName) }
                } catch (e: OutOfMemoryError) {
                    e.printStackTrace()
                    notifyBriefly(R.string.text_error)
                    false
                } catch (e: Exception) {
                    e.printStackTrace()
                    notifyBriefly(R.string.text_error)
                    false
                }
            } else {
                false
            }
            if (isSuccessful) {
                notify(R.string.im_toast_finish)
            } else {
                notifyBriefly(R.string.text_error)
            }
            launch(Dispatchers.Main) {
                setDefaultImage(context)
            }
        }.invokeOnCompletion {
            popLoading.dismiss()
        }
    }

    private fun processImage(context: Context, targetImage: Bitmap): Bitmap {
        var image = targetImage
        val tX: Int = if (!viewBinding.imageEditWidth.text.isNullOrEmpty())
            viewBinding.imageEditWidth.text!!.toString().toInt().let {
                if (it < 8000) it
                else {
                    X.toast(context, "limit width 8000", Toast.LENGTH_SHORT)
                    image.width
                }
            }
        else {
            image.width
        }
        val tH: Int = if (!viewBinding.imageEditHeight.text.isNullOrEmpty())
            viewBinding.imageEditHeight.text!!.toString().toInt().let {
                if (it < 8000) it
                else {
                    X.toast(context, "limit height 8000", Toast.LENGTH_SHORT)
                    image.height
                }
            }
        else {
            image.height
        }
        image = X.toTarget(image, tX, tH)
        val blurDegree = viewBinding.imageBlur.progress / 4f
        if (blurDegree != 0f) {
            val blurred = X.toMin(image.toMutable(), 100).let {
                GaussianBlur(context).blurOnce(it, blurDegree)
            }
            image = X.toTarget(blurred, image.width, image.height)
        }
        //                image = X.toTarget(GaussianBlur(context).blurOnce(X.toMin(image.collisionBitmap, 100), blurDegree), image.width, image.height)
        return image
    }

    private fun saveImage(context: Context, image: Bitmap, imageName: String): Boolean{
        val formatExtension: String = viewBinding.toolsImageFormat.text.toString()
        var isHeif = false
        var name = imageName.substring(0, imageName.lastIndexOf("."))
        val formatMimeType: String
        var format: Bitmap.CompressFormat? = null
        when (formatExtension) {
            "jpg" -> {
                formatMimeType = "image/jpeg"
                format = Bitmap.CompressFormat.JPEG
            }
            "webp" -> {
                formatMimeType = "image/webp"
                format = Bitmap.CompressFormat.WEBP
            }
            "heif" -> {
                formatMimeType = "image/heif"
                isHeif = true
            }
            else -> {
                formatMimeType = "image/png"
                format = Bitmap.CompressFormat.PNG
            }
        }
        name += "-modified.$formatExtension"
        var imagePath = ""
        val uri = if (X.aboveOn(X.Q)) getImageUri4Q(context, name, formatMimeType) else {
            imagePath = F.createPath(F.externalRoot(Environment.DIRECTORY_PICTURES), name)
            File(imagePath).getProviderUri(context)
        }
        val fd = if (uri == null) null else try {
            context.contentResolver.openFileDescriptor(uri, "w")?.fileDescriptor
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            null
        }
        val compressQuality = viewBinding.imageCompress.progress
        val isSucceful = fd?.let {
            if (isHeif) {
                if (X.aboveOn(X.P)) saveAsHeif(image, fd, compressQuality) else false
            } else saveAsNonHeif(image, fd, compressQuality, format!!, formatMimeType, imagePath)
        } ?: false
        if (!isSucceful && X.aboveOn(X.Q) && uri != null) {
            context.contentResolver.delete(uri, null, null)
        }
        return isSucceful
    }

    @RequiresApi(value = Build.VERSION_CODES.Q)
    private fun getImageUri4Q(context: Context, name: String, formatMimeType: String): Uri?{
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, formatMimeType)
        }
        val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        return context.contentResolver.insert(collection, values)
    }

    private fun saveAsNonHeif(image: Bitmap, fd: FileDescriptor, compressQuality: Int, format: Bitmap.CompressFormat, formatMimeType: String, imagePath: String): Boolean{
        try {
            FileOutputStream(fd).use {
                image.compress(format, compressQuality, it)
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            return false
        }
        if (X.belowOff(X.Q)) MediaScannerConnection.scanFile(activity, arrayOf(imagePath), arrayOf(formatMimeType), null)
        return true
    }

    private fun saveAsHeif(bitmap: Bitmap, fd: FileDescriptor, compressQuality: Int): Boolean{
        try {
            HeifWriter.Builder(fd, bitmap.width, bitmap.height, HeifWriter.INPUT_MODE_BITMAP)
                    .setQuality(compressQuality).build().run {
                        start()
                        addBitmap(bitmap)
                        stop(0)
                        close()
                    }
        }catch (e: Exception){
            e.printStackTrace()
            return false
        }
        return true
    }
}
