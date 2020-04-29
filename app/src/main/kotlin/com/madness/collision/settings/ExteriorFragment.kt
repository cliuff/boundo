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

package com.madness.collision.settings

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.observe
import androidx.navigation.fragment.navArgs
import com.madness.collision.Democratic
import com.madness.collision.R
import com.madness.collision.main.MainActivity
import com.madness.collision.main.MainViewModel
import com.madness.collision.unit.themed_wallpaper.AccessTw
import com.madness.collision.unit.themed_wallpaper.ThemedWallpaperEasyAccess
import com.madness.collision.util.*
import kotlinx.android.synthetic.main.activity_exterior.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.*
import kotlin.math.min

class ExteriorFragment: Fragment(), Democratic, View.OnClickListener{
    companion object {
        private const val REQUEST_GET_IMAGE = 100
        private const val PERMISSION_EXTERNAL_STORAGE = 200
        private const val TAG = "Exterior"
        private const val ARG_MODE = "exteriorMode"
        private const val ARG_LAUNCH_MODE = "exteriorLaunchMode"
        /**
         * App light background selecting
         */
        const val MODE_LIGHT = 0
        /**
         * App dark background selecting
         */
        const val MODE_DARK = 1
        /**
         * Themed Wallpaper light wallpaper selecting
         */
        const val MODE_TW_LIGHT = 2
        /**
         * Themed Wallpaper dark wallpaper selecting
         */
        const val MODE_TW_DARK = 3
        const val LAUNCH_MODE_NON_NAV = "nonNav"

        @JvmStatic
        fun newInstance(mode: Int) : ExteriorFragment{
            val b = Bundle().apply {
                putInt(ARG_MODE, mode)
                putString(ARG_LAUNCH_MODE, LAUNCH_MODE_NON_NAV)
            }
            return ExteriorFragment().apply { arguments = b }
        }
    }

    private var mode: Int = MODE_LIGHT
    private var isTW : Boolean = false
    private var isDarkMode: Boolean = false
    private var image: Bitmap? = null
    private var blurred: Bitmap? = null
    private lateinit var forBlurry: Bitmap
    private lateinit var rs: GaussianBlur
    private lateinit var sb: SeekBar
    private val mainViewModel: MainViewModel by activityViewModels()
    private lateinit var backPath: String
    private var mImgGallery: Drawable? = null

    private fun getImgGallery(context: Context): Drawable?{
        if (mImgGallery == null) mImgGallery = context.getDrawable(R.drawable.img_gallery)
        return mImgGallery
    }

    override fun createOptions(context: Context, toolbar: Toolbar, iconColor: Int): Boolean {
        toolbar.setTitle(R.string.prefExteriorBackgrounds)
        toolbar.inflateMenu(R.menu.toolbar_exterior)
        toolbar.menu.findItem(R.id.exteriorTBClear).icon.setTint(ThemeUtil.getColor(context, R.attr.colorActionAlert))
        toolbar.menu.findItem(R.id.exteriorTBDone).icon.setTint(ThemeUtil.getColor(context, R.attr.colorActionPass))
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity?.let { SettingsFunc.updateLanguage(it) }
        return inflater.inflate(R.layout.activity_exterior, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val context = context ?: return
        democratize(mainViewModel)
        mainViewModel.contentWidthTop.observe(viewLifecycleOwner) {
            exteriorContainer.alterPadding(top = it)
        }

        sb = exteriorSeekBar
        rs = GaussianBlur(context)
        sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i : Int, b: Boolean) {
                exteriorBlurValue.text = String.format("%d/100", i)
                if (image == null) return
                val size = Point(exteriorBack.width, exteriorBack.height)
                val blurDegree = i / 4f
                if (blurDegree == 0f) {
                    blurred = image!!
                    GlobalScope.launch(Dispatchers.Main) {
                        exteriorBack.background = BitmapDrawable(resources, X.toTarget(blurred!!, size.x, size.y))
                    }
                }else {
                    blurred = rs.blur(forBlurry, blurDegree)
                    GlobalScope.launch(Dispatchers.Main) {
                        exteriorBack.background = BitmapDrawable(resources, X.toTarget(blurred!!, size.x, size.y))
                    }
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        exteriorImage.listenedBy(this)

        GlobalScope.launch {
            val args = arguments
            val isByNav = args?.getString(ARG_LAUNCH_MODE) != LAUNCH_MODE_NON_NAV
            val navArgs: ExteriorFragmentArgs by navArgs()
            mode = if (isByNav) navArgs.mode else (args?.getInt(ARG_MODE) ?: MODE_LIGHT)
            isTW = (mode == MODE_TW_LIGHT || mode == MODE_TW_DARK)
            isDarkMode = (mode == if (isTW) MODE_TW_DARK else MODE_DARK)
            backPath = when (mode) {
                MODE_LIGHT -> F.valFilePubExteriorPortrait(context)
                MODE_DARK -> F.valFilePubExteriorPortraitDark(context)
                MODE_TW_LIGHT -> F.valFilePubTwPortrait(context)
                MODE_TW_DARK -> F.valFilePubTwPortraitDark(context)
                else -> ""
            }

            val file = File(backPath)
            if (!file.exists()){
                setImage(context, null)
                return@launch
            }
            ImageUtil.getBitmap(file)?.let {
                setImage(context, it)
            } ?: setImage(context, null)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        rs.destroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val context = context ?: return
        if (requestCode == REQUEST_GET_IMAGE && resultCode == AppCompatActivity.RESULT_OK && data != null){
            val dataUri = data.data ?: return
            MemoryManager.clearSpace(activity)
            try {
                image = ImageUtil.getBitmap(context, dataUri)
            } catch ( e: IOException) {
                e.printStackTrace()
            }
            setImage(context, image!!)
        }
    }

    /**
     * Update UI and blurred image
     */
    private fun setImage(context: Context, image: Bitmap?){
        GlobalScope.launch {
            this@ExteriorFragment.image = image?.collisionBitmap
            // set default image
            val tg200dp = X.size(context, 200f, X.DP).toInt()
            if (image == null){
                blurred = null
                val imgGallery = getImgGallery(context)
                launch(Dispatchers.Main){
                    (exteriorImage.layoutParams as FrameLayout.LayoutParams).run {
                        width = tg200dp
                        height = tg200dp
                    }
                    exteriorImage.setImageDrawable(imgGallery)
                    exteriorCardImage.cardElevation = 0f
                    exteriorBack.background = null
                }
                return@launch
            }
            // prepare image
            val imgRes = this@ExteriorFragment.image!!
            forBlurry = X.toMin(imgRes, 100)
            val targetImage = X.toMax(imgRes, tg200dp)
            val elevation = X.size(context, 4f, X.DP)
            // prepare background
            val size = Point(exteriorBack.width, exteriorBack.height)
            val blurDegree = sb.progress / 4f
            val shouldBlur = blurDegree != 0f
            blurred = if (shouldBlur) rs.blur(forBlurry, blurDegree) else imgRes
            val targetBack = BitmapDrawable(resources, X.toTarget(blurred!!, size.x, size.y))
            // update UI
            launch(Dispatchers.Main){
                // set image
                (exteriorImage.layoutParams as FrameLayout.LayoutParams).run {
                    width = ViewGroup.LayoutParams.WRAP_CONTENT
                    height = ViewGroup.LayoutParams.WRAP_CONTENT
                }
                exteriorImage.setImageBitmap(targetImage)
                exteriorCardImage.cardElevation = elevation
                // set background
                exteriorBack.background = targetBack
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val context = context ?: return
        when (requestCode) {
            PERMISSION_EXTERNAL_STORAGE -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    actionDone(context)
                } else {
                    notifyBriefly(R.string.toast_permission_storage_denied)
                }
            }
        }
    }

    private fun actionDone(context: Context){
        /*
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (X.aboveOn(X.M))
                requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), PERMISSION_EXTERNAL_STORAGE)
            else
                Toast.makeText(context, R.string.toast_permission_storage_denied, Toast.LENGTH_SHORT).show()
            return
        }
*/
        val progressBar = ProgressBar(context)
        val dialog = CollisionDialog.loading(context, progressBar)
        dialog.show()
        GlobalScope.launch {
            val exteriorPath = F.valFilePubExterior(context)

            if (blurred == null){
                val background = File(backPath)
                F.prepare4(background) // delete if exist

                if (isTW){
                    if (!ThemedWallpaperEasyAccess.isDead && ThemedWallpaperEasyAccess.isDark == isDarkMode){
                        ThemedWallpaperEasyAccess.background = ColorDrawable(if (isDarkMode) Color.BLACK else Color.WHITE)
                        ThemedWallpaperEasyAccess.wallpaperTimestamp = System.currentTimeMillis()
                    }
                } else {
                    if (mainApplication.isDarkTheme == isDarkMode){
                        mainApplication.background = null
                        mainApplication.exterior = false
                    }
                }
                launch(Dispatchers.Main) {
                    dialog.dismiss()
                    if (isTW){
                        notifyBriefly(R.string.text_done)
                        AccessTw.getIsWallpaperChanged(this@ExteriorFragment).value = true
                        mainViewModel.popUpBackStack()
                    } else{
//                    findNavController().popBackStack(R.id.mainFragment, true)
                        mainViewModel.action.value = MainActivity.ACTION_EXTERIOR to null
                    }
                }
                return@launch
            }

            val size: Point = X.getPortraitRealResolution(context)
            var blurred = blurred!!
            // enlarge small image
            if (min(blurred.width, blurred.height) < size.x) blurred = X.toMin(blurred, size.x)
            // shrink large image
            val maxWidth = if (isTW) 2 * size.y else size.y
            val maxHeight = size.y
            if (blurred.height > maxHeight){
                val targetWidth = (blurred.width * maxHeight) / blurred.height
                blurred = Bitmap.createScaledBitmap(blurred, targetWidth, maxHeight, true)
            }
            if (blurred.width > maxWidth){
                val targetHeight = (blurred.height * maxWidth) / blurred.width
                blurred = Bitmap.createScaledBitmap(blurred, maxWidth, targetHeight, true)
            }
            if (isTW){
                if (!ThemedWallpaperEasyAccess.isDead && ThemedWallpaperEasyAccess.isDark == isDarkMode){
                    ThemedWallpaperEasyAccess.background = BitmapDrawable(resources, blurred)
                    ThemedWallpaperEasyAccess.wallpaperTimestamp = System.currentTimeMillis()
                }
            }else{
                if (mainApplication.isDarkTheme == isDarkMode){
                    mainApplication.run {
                        background = BitmapDrawable(resources, blurred)
                        exterior = true
                    }
                }
            }

            if (F.prepareDir(exteriorPath)) {
                try {
                    val stream = FileOutputStream(File(backPath))
                    blurred.compress(Bitmap.CompressFormat.WEBP, P.WEBP_COMPRESS_SPACE_FIRST, stream)
                } catch ( e: FileNotFoundException) {
                    e.printStackTrace()
                }
            }

            launch(Dispatchers.Main) {
                dialog.dismiss()
                if (isTW){
                    notifyBriefly(R.string.text_done)
                    AccessTw.getIsWallpaperChanged(this@ExteriorFragment).value = true
                    mainViewModel.popUpBackStack()
                } else{
//                    findNavController().popBackStack(R.id.mainFragment, true)
                    mainViewModel.action.value = MainActivity.ACTION_EXTERIOR to null
                }
            }
        }
    }

    override fun onClick(v: View?) {
        v ?: return
        if (v.id == R.id.exteriorImage){
            val getImage = Intent(Intent.ACTION_GET_CONTENT)
            getImage.type = "image/*"
            startActivityForResult(getImage, REQUEST_GET_IMAGE)
        }
    }
}