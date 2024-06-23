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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.SeekBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.madness.collision.Democratic
import com.madness.collision.R
import com.madness.collision.databinding.ActivityExteriorBinding
import com.madness.collision.main.MainViewModel
import com.madness.collision.unit.themed_wallpaper.AccessTw
import com.madness.collision.unit.themed_wallpaper.ThemedWallpaperEasyAccess
import com.madness.collision.util.*
import com.madness.collision.util.os.OsUtils
import com.madness.collision.util.ui.appLocale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import kotlin.math.min
import kotlin.math.roundToInt

class ExteriorFragment: TaggedFragment(), Democratic {

    override val category: String = "Exterior"
    override val id: String = "Exterior"

    companion object {
        const val ARG_MODE = "exteriorMode"
        /**
         * Themed Wallpaper light wallpaper selecting
         */
        const val MODE_TW_LIGHT = 2
        /**
         * Themed Wallpaper dark wallpaper selecting
         */
        const val MODE_TW_DARK = 3

        @JvmStatic
        fun newInstance(mode: Int) : ExteriorFragment{
            val b = Bundle().apply { putInt(ARG_MODE, mode) }
            return ExteriorFragment().apply { arguments = b }
        }
    }

    private var mode: Int = MODE_TW_LIGHT
    private var isDarkMode: Boolean = false
    private var imageUri: Uri? = null
    private var backPreview: Bitmap? = null
    private var blurred: Bitmap? = null
    private var forBlurry: Bitmap? = null
    private lateinit var rs: GaussianBlur
    private lateinit var sb: SeekBar
    private val mainViewModel: MainViewModel by activityViewModels()
    private lateinit var backPath: String
    private var mImgGallery: Drawable? = null
    private lateinit var viewBinding: ActivityExteriorBinding

    private fun getImgGallery(context: Context): Drawable? {
        if (mImgGallery == null) mImgGallery = ContextCompat.getDrawable(context, R.drawable.img_gallery)
        return mImgGallery
    }

    override fun createOptions(context: Context, toolbar: Toolbar, iconColor: Int): Boolean {
        mainViewModel.configNavigation(toolbar, iconColor)
        toolbar.setTitle(R.string.prefExteriorBackgrounds)
        toolbar.inflateMenu(R.menu.toolbar_exterior)
        toolbar.menu.findItem(R.id.exteriorTBClear).icon?.setTint(ThemeUtil.getColor(context, R.attr.colorActionAlert))
        toolbar.menu.findItem(R.id.exteriorTBDone).icon?.setTint(ThemeUtil.getColor(context, R.attr.colorActionPass))
        return true
    }

    override fun selectOption(item: MenuItem): Boolean {
        when (item.itemId){
            R.id.exteriorTBClear -> {
                val context = context ?: return false
                setImage(context, null)
                return true
            }
            R.id.exteriorTBDone -> {
                val context = context ?: return false
                actionDone(context)
                return true
            }
        }
        return false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewBinding = ActivityExteriorBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val context = context ?: return
        democratize(mainViewModel)
        mainViewModel.contentWidthTop.observe(viewLifecycleOwner) {
            viewBinding.exteriorContainer?.alterPadding(top = it)
        }

        sb = viewBinding.exteriorSeekBar
        rs = GaussianBlur(context)
        val appLocale = appLocale
        val initialFraction = String.format(appLocale, "%d/%d", 0, 100)
        (viewBinding.exteriorBlurValue as AppCompatTextView).text = initialFraction
        sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i : Int, b: Boolean) {
                viewBinding.exteriorBlurValue.text = String.format(appLocale, "%d/%d", i, 100)
                updateBlur(i)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        viewBinding.exteriorImage.setOnClickListener {
            getImageLauncher.launch("image/*")
        }

        lifecycleScope.launch(Dispatchers.Default) {
            val args = arguments
            mode = args?.getInt(ARG_MODE) ?: MODE_TW_LIGHT
            isDarkMode = (mode == MODE_TW_DARK)
            backPath = when (mode) {
                MODE_TW_LIGHT -> F.valFilePubTwPortrait(context)
                MODE_TW_DARK -> F.valFilePubTwPortraitDark(context)
                else -> ""
            }

            val file = File(backPath)
            if (!file.exists()){
                setImage(context, null)
                return@launch
            }
            clearRef()
            clearViewRes()
            val (imagePreview, backPreview) = loadSamples(context, file = file)
            this@ExteriorFragment.backPreview = backPreview
            setImage(context, imagePreview)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        rs.destroy()
    }

    // This must be called unconditionally, as part of initialization path,
    // typically as a field initializer of an Activity or Fragment.
    private val getImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) {
        val context = context ?: return@registerForActivityResult
        val dataUri = it ?: return@registerForActivityResult
        lifecycleScope.launch(Dispatchers.Default) {
            clearRef()
            clearViewRes()
            val (imagePreview, backPreview) = loadSamples(context, dataUri)
            this@ExteriorFragment.backPreview = backPreview
            imageUri = dataUri
            setImage(context, imagePreview)
        }
    }

    private fun loadSamples(context: Context, uri: Uri? = null, file: File? = null): Pair<Bitmap?, Bitmap?> {
        val previewSize = X.size(context, 200f, X.DP).roundToInt()
        var imagePreview: Bitmap? = null
        var backPreview: Bitmap? = null
        try {
            if (uri != null) {
                imagePreview = ImageUtil.getSampledBitmap(context, uri, previewSize, previewSize)
                backPreview = ImageUtil.getSampledBitmap(context, uri, viewBinding.exteriorBack.width, viewBinding.exteriorBack.height)
            } else if (file != null) {
                imagePreview = ImageUtil.getSampledBitmap(file, previewSize, previewSize)
                backPreview = ImageUtil.getSampledBitmap(file, viewBinding.exteriorBack.width, viewBinding.exteriorBack.height)
            }
        } catch (e: OutOfMemoryError) {
            e.printStackTrace()
            notifyBriefly(R.string.text_error)
        } catch (e: Exception) {
            e.printStackTrace()
            notifyBriefly(R.string.text_error)
        }
        return imagePreview to backPreview
    }

    private fun clearRef() {
        blurred = null
        forBlurry = null
        backPreview = null
        imageUri = null
    }

    private fun clearViewRes() {
        lifecycleScope.launch(Dispatchers.Main) {
            viewBinding.exteriorBack.background = null
            viewBinding.exteriorImage.setImageDrawable(null)
        }
    }

    /**
     * Update UI and blurred image
     */
    private fun setImage(context: Context, image: Bitmap?, previewSize: Int = X.size(context, 200f, X.DP).roundToInt()){
        lifecycleScope.launch(Dispatchers.Default) {
            // set default image
            if (image == null){
                clearRef()
                clearViewRes()
                val imgGallery = getImgGallery(context)
                launch(Dispatchers.Main){
                    (viewBinding.exteriorImage.layoutParams as FrameLayout.LayoutParams).run {
                        width = previewSize
                        height = previewSize
                    }
                    viewBinding.exteriorImage.setImageDrawable(imgGallery)
                    viewBinding.exteriorCardImage.cardElevation = 0f
                    viewBinding.exteriorBack.background = null
                }
                return@launch
            }
            // prepare image
            forBlurry = X.toMin(image, 100)
            val previewImage = X.toMax(image, previewSize)
            val elevation = X.size(context, 4f, X.DP)
            // update UI
            launch(Dispatchers.Main){
                // set image
                (viewBinding.exteriorImage.layoutParams as FrameLayout.LayoutParams).run {
                    width = ViewGroup.LayoutParams.WRAP_CONTENT
                    height = ViewGroup.LayoutParams.WRAP_CONTENT
                }
                viewBinding.exteriorImage.setImageBitmap(previewImage)
                viewBinding.exteriorCardImage.cardElevation = elevation
            }
            updateBlur(sb.progress)
        }
    }

    private fun updateBlur(progress: Int) {
        if (backPreview == null) return
        lifecycleScope.launch(Dispatchers.Default) {
            val size = Point(viewBinding.exteriorBack.width, viewBinding.exteriorBack.height)
            val blurDegree = progress / 4f
            val shouldBlur = blurDegree != 0f
            blurred = if (shouldBlur) rs.blur(forBlurry!!, blurDegree) else backPreview
            blurred = X.toTarget(blurred!!, size.x, size.y)
            val targetBack = BitmapDrawable(resources, blurred)
            launch(Dispatchers.Main){
                viewBinding.exteriorBack.background = targetBack
            }
        }
    }

    private fun actionDone(context: Context) {
        val progressBar = ProgressBar(context)
        val dialog = CollisionDialog.loading(context, progressBar)
        dialog.show()
        lifecycleScope.launch(Dispatchers.Default) {
            try {
                processImage(context)
            } catch (e: OutOfMemoryError) {
                e.printStackTrace()
                notifyBriefly(R.string.text_error)
            } catch (e: Exception) {
                e.printStackTrace()
                notifyBriefly(R.string.text_error)
            }
            launch(Dispatchers.Main) {
                dialog.dismiss()
                notifyBriefly(R.string.text_done, true)
                AccessTw.updateChangeTimestamp()
                mainViewModel.popUpBackStack()
            }
        }
    }

    private fun processImage(context: Context){
        val exteriorPath = F.valFilePubExterior(context)

        if (imageUri == null){
            val background = File(backPath)
            F.prepare4(background) // delete if exist

            if (!ThemedWallpaperEasyAccess.isDead && ThemedWallpaperEasyAccess.isDark == isDarkMode){
                ThemedWallpaperEasyAccess.background = ColorDrawable(if (isDarkMode) Color.BLACK else Color.WHITE)
                ThemedWallpaperEasyAccess.wallpaperTimestamp = System.currentTimeMillis()
            }
            return
        }

        val size: Point = getPortraitRealResolution(context)
        val blurDegree = sb.progress / 4f
        val shouldBlur = blurDegree != 0f
        var blurred1 = if (shouldBlur) rs.blur(forBlurry!!, blurDegree) else null
        val uri = imageUri!!
        clearRef()
        clearViewRes()
        if (!shouldBlur) {
            blurred1 = ImageUtil.getBitmap(context, uri)
        }
        var blurred = blurred1 ?: return
        // enlarge small image
        if (min(blurred.width, blurred.height) < size.x) blurred = X.toMin(blurred, size.x)
        // shrink large image
        val maxWidth = 2 * size.y
        val maxHeight = size.y
        if (blurred.height > maxHeight){
            val targetWidth = (blurred.width * maxHeight) / blurred.height
            blurred = Bitmap.createScaledBitmap(blurred, targetWidth, maxHeight, true)
        }
        if (blurred.width > maxWidth){
            val targetHeight = (blurred.height * maxWidth) / blurred.width
            blurred = Bitmap.createScaledBitmap(blurred, maxWidth, targetHeight, true)
        }
        if (!ThemedWallpaperEasyAccess.isDead && ThemedWallpaperEasyAccess.isDark == isDarkMode){
            ThemedWallpaperEasyAccess.background = BitmapDrawable(resources, blurred)
            ThemedWallpaperEasyAccess.wallpaperTimestamp = System.currentTimeMillis()
        }

        if (F.prepareDir(exteriorPath)) {
            val format = if (OsUtils.satisfy(OsUtils.R)) {
                Bitmap.CompressFormat.WEBP_LOSSY
            } else {
                @Suppress("DEPRECATION")
                Bitmap.CompressFormat.WEBP
            }
            try {
                val stream = FileOutputStream(File(backPath))
                blurred.compress(format, P.WEBP_COMPRESS_SPACE_FIRST, stream)
            } catch ( e: FileNotFoundException) {
                e.printStackTrace()
            }
        }

    }

    private fun getPortraitRealResolution(context: Context): Point {
        val display = SystemUtil.getDisplay(context) ?: return Point()
        val size = Point()
        // subject to device rotation regardless of ORIENTATION value
        display.getRealSize(size)
        val rotation = display.rotation
        return if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) Point(size.y, size.x) else size
    }
}