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

package com.madness.collision.util

import android.app.Activity
import android.app.AppOpsManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.*
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.util.TypedValue
import android.view.Display
import android.view.Surface
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.util.component1
import androidx.core.util.component2
import androidx.palette.graphics.Palette
import com.madness.collision.misc.MiscApp
import com.madness.collision.unit.api_viewing.AccessAV
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.*
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max
import kotlin.math.min
import com.madness.collision.R as MyRes

object X {
    fun deleteFolder( folder: File) = folder.deleteRecursively()

    fun listFiles( folder: File,  mimeType: String,  list: MutableList<String>){
        val ms = arrayOf(mimeType)
        listFiles(folder, ms, list)
    }

    fun listFiles( folderDir: String,  mimeType: String,  list: MutableList<String>){
        val ms = arrayOf(mimeType)
        listFiles(folderDir, ms, list)
    }

    fun listFiles( folderDir: String, mimeTypes: Array<String>, list: MutableList<String>){
        listFiles(File(folderDir), mimeTypes, list)
    }

    fun listFiles( folder: File, mimeTypes: Array<String>,  list: MutableList<String>){
        if (!folder.exists() || !folder.canRead() || !folder.isDirectory) {
            return
        }
        for (newFile in folder.listFiles() ?: emptyArray()){
            if (newFile.isFile) {
                for (mimeType in mimeTypes){/*
                    if (mimeType.equals(getMimeType(newFile))) {
                        list.add(newFile.getPath())
                        break
                    }*/
                    if (newFile.name.endsWith(mimeType)) {
                        list.add(newFile.path)
                        break
                    }
                }
            }else if (newFile.isDirectory) {
                listFiles(newFile, mimeTypes, list)
            }
        }
    }

    fun listFiles4API( handler: Handler, folderDir:  String){
        listFiles4API(handler, File(folderDir))
    }

    fun listFiles4API( handler: Handler,  folder: File){
        if (!folder.exists() || !folder.canRead() || !folder.isDirectory) return
        for (newFile in folder.listFiles() ?: emptyArray()) {
            try {
                if (newFile.isFile && newFile.name.endsWith(".apk")) {
                    handler.obtainMessage(AccessAV.HANDLE_DISPLAY_APK, newFile.path).sendToTarget()
                } else if (newFile.isDirectory) {
                    listFiles4API(handler, newFile)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        /*
        if (aboveOn(O)){
            try {
                Files.find(
                        Paths.get(folder.path),
                        Integer.MAX_VALUE,
                        BiPredicate { path, basicFileAttributes -> basicFileAttributes.isRegularFile && path.toString().endsWith(".apk") }
                ).forEach { path ->
                    // it is a Path object rather than String
                    handler.obtainMessage(ApiFragment.HANDLE_DISPLAY_APK, path.toFile().path).sendToTarget()
                }
            } catch ( e: IOException) {
                e.printStackTrace()
            }
        }else {
        }*/
    }

    fun copyFileLessTwoGB( src: File,  dst: File) {
        val inStream = FileInputStream(src)
        val outStream = FileOutputStream(dst)
        val inChannel = inStream.channel
        val outChannel = outStream.channel
        inChannel.transferTo(0, inChannel.size(), outChannel)
        inChannel.close()
        outChannel.close()
        inStream.close()
        outStream.close()
    }

    fun getStatusBarHeight(context: Context): Int {
        val result = AtomicInteger()
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) result.set(context.resources.getDimensionPixelSize(resourceId))
        if (result.get() == 0) result.set(size(context, 30f, DP).toInt())
        return result.get()
    }

    fun setBackground(context: Context,  background: View){
        val point = getPortraitRealResolution(context)
        val isLandscape = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val width = if (isLandscape) point.y else point.x
        val height = if (isLandscape) point.x else point.y
        /*
                ObjectAnimator.ofObject(background, "background", (fraction, startValue, endValue) -> {
                    Rect rect = new Rect(0, 0, background.getWidth(), background.getHeight())
                    Drawable drawable
                    if (startValue == null){
                        drawable = (Drawable) endValue
                        drawable = drawable.mutate()
                        drawable.setAlpha((int)(255 * fraction))
                        drawable.setBounds(rect)
                        return drawable
                    }else {
                        if (fraction < 0.5){
                            drawable = (Drawable) startValue
                            drawable = drawable.mutate()
                            drawable.setAlpha((int)(255 * (0.5 - fraction)))
                            drawable.setBounds(rect)
                            return drawable
                        }else {
                            drawable = (Drawable) endValue
                            drawable = drawable.mutate()
                            drawable.setAlpha((int)(255 * (fraction - 0.5)))
                            drawable.setBounds(rect)
                            return drawable
                        }
                    }
                },MainApplication.getInstance().background, MainApplication.getInstance().background);*/
        setSplitBackground(context, background, width, height)
    }

    fun setSplitBackground( context: Context,  background: View, width: Int, height: Int){
        if (mainApplication.background != null) {
            background.background = BitmapDrawable(context.resources, BackgroundUtil.getBackground(mainApplication.background!!, width, height))
        }
    }

    fun setSplitBackground( context: Context,  background: View,  size: Point){
        setSplitBackground(context, background, size.x, size.y)
    }

    fun iconDrawable2Bitmap( context: Context,  drawable: Drawable): Bitmap{
        if (aboveOn(O) && drawable is AdaptiveIconDrawable) return GraphicsUtil.drawAIRound(context, drawable)
        return drawableToBitmap(drawable)
    }

    /**
     * normal drawable that are not AdaptiveIconDrawable
     * @param drawable drawable res
     * @return bitmap
     */
    fun drawableToBitmap ( drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) return drawable.bitmap.collisionBitmap

        val intrinsicWidth = drawable.intrinsicWidth
        val intrinsicHeight = drawable.intrinsicHeight
        val bitmap: Bitmap = if (intrinsicWidth <= 0 || intrinsicHeight <= 0)
        // Single color bitmap will be created of 1x1 pixel, update: it might as well be gradient drawable apart from single color drawable
            Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888)
        else
            Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }


    /**
     * clip the image to target
     * @param src image to be processed
     * @param targetWidth target width
     * @param targetHeight target height
     * @return product
     */
    fun toTarget(src: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap{
        if (targetWidth == 0 || targetHeight == 0) return src
        val originalWidth = src.width
        val originalHeight = src.height
        if (originalHeight == targetHeight && originalWidth == targetWidth) return src
        var image: Bitmap
        val cropWidth = originalWidth * targetHeight / originalHeight >= targetWidth
        if (cropWidth){
            val cropLength = (originalWidth - originalHeight * targetWidth / targetHeight) / 2
            image = Bitmap.createBitmap(src, cropLength, 0, originalWidth - 2 * cropLength, originalHeight)
            image = Bitmap.createScaledBitmap(image, targetWidth, targetHeight, true)
        }else {
            val cropLength = (originalHeight - originalWidth * targetHeight / targetWidth) / 2
            image = Bitmap.createBitmap(src, 0, cropLength, originalWidth, originalHeight - 2 * cropLength)
            image = Bitmap.createScaledBitmap(image, targetWidth, targetHeight, true)
        }
        return image
    }

    fun toMax( image: Bitmap, maximumLength: Int): Bitmap{
        val (targetWidth, targetHeight) = image.size.doOptimal(maximumLength)
        return Bitmap.createScaledBitmap(image, targetWidth, targetHeight, true)
    }

    fun toMin( image: Bitmap, minimumLength: Int): Bitmap{
        val (targetWidth, targetHeight) = image.size.doMinimum(minimumLength)
        return Bitmap.createScaledBitmap(image, targetWidth, targetHeight, true)
    }

    fun toSquare( image: Bitmap): Bitmap{
        val lengthHorizontal = image.width
        val lengthVertical = image.height
        if (lengthHorizontal == lengthVertical) return image
        return when {
            lengthHorizontal > lengthVertical -> {
                val translateX = (lengthHorizontal - lengthVertical)/2
                val translateY = 0
                Bitmap.createBitmap(image, translateX, translateY, lengthVertical, lengthVertical)
            }
            lengthHorizontal < lengthVertical -> {
                val translateX = 0
                val translateY = (lengthVertical - lengthHorizontal) / 2
                Bitmap.createBitmap(image, translateX, translateY, lengthHorizontal, lengthHorizontal)
            }
            else -> image
        }
    }

    fun getColor( context: Context, colorRes: Int): Int{
        return ContextCompat.getColor(context, colorRes)
    }

    fun getColorHex( context: Context, colorRes: Int): String{
        return Integer.toHexString(getColor(context, colorRes) and 0x00ffffff)
    }

    fun getPortraitRealResolution(context: Context): Point {
        val display = SystemUtil.getDisplay(context) ?: return Point()
        val size = Point()
        // subject to device rotation regardless of ORIENTATION value
        display.getRealSize(size)
        val rotation = display.rotation
        return if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) Point(size.y, size.x) else size
    }

    fun getCurrentAppResolution(context: Context): Point {
        return if (aboveOn(R)) {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager? ?: return Point()
            val metrics = windowManager.currentWindowMetrics
            val bounds = metrics.bounds
            Point(bounds.width(), bounds.height())
        } else {
            val display = SystemUtil.getDisplay(context) ?: return Point()
            val size = Point()
            display.getSizeLegacy(size)
            size
        }
    }

    @Suppress("deprecation")
    private fun Display.getSizeLegacy(size: Point) {
        getSize(size)
    }

    /**
     * get circular bitmap
     * @param source
     * original (non)rectangular bitmap
     * @return
     * circular bitmap
     */
    fun circularBitmap( source: Bitmap): Bitmap{
        val width = source.width
        val height = source.height
        val radius = min(width, height) * 0.5f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val bitmap = source.collisionBitmap
        //画布设置遮罩效果
        paint.shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        //处理图像数据
        val circular = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(circular)
        canvas.drawCircle(width * 0.5f, height * 0.5f, radius, paint)
        return circular
    }

    const val DP = TypedValue.COMPLEX_UNIT_DIP
    const val SP = TypedValue.COMPLEX_UNIT_SP
    fun size( context: Context, value: Float, type: Int): Float = TypedValue.applyDimension(type, value, context.resources.displayMetrics)

    fun colorAgainstBackground( fore: View,  back: View, offsetX: Int = 0, offsetY: Int = 0): Int{
        return extractBackColor(fore, back, offsetX, offsetY)[0]
    }

    fun extractBackColor( fore: View,  back: View, offsetX: Int = 0, offsetY: Int = 0): IntArray{
        val colors = IntArray(2)
        if (back.background == null){
            colors[0] = Color.BLACK
            return colors
        }
        val prefWidth: Int
        val prefHeight: Int
        val isForeInflated = fore.width > 0
        if (!isForeInflated){
            fore.measure()
            prefWidth = fore.measuredWidth
            prefHeight = fore.measuredHeight
        }else {
            prefWidth = fore.width
            prefHeight = fore.height
        }
        val drawable = back.background
        if (drawable == null){
            colors[0] = Color.BLACK
            return colors
        }
        val foreBitmap: Bitmap
        if (drawable is BitmapDrawable && drawable.bitmap != null) // Single color bitmap will be created of 1x1 pixel
        {
            foreBitmap = drawable.bitmap.collisionBitmap
        } else{
            val intrinsicWidth = drawable.intrinsicWidth
            val intrinsicHeight = drawable.intrinsicHeight
            foreBitmap = if (intrinsicWidth <= 0 || intrinsicHeight <= 0) {
                Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888) // Single color bitmap will be created of 1x1 pixel
            } else {
                Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888)
            }
            val canvas = Canvas(foreBitmap)
            drawable.draw(canvas)
        }
        var bitmap: Bitmap
        if (foreBitmap.width == 1 && foreBitmap.height == 1){
            bitmap = foreBitmap
        } else {
            bitmap = Bitmap.createBitmap(prefWidth, prefHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawBitmap(foreBitmap, Rect(offsetX, offsetY, prefWidth, prefHeight), Rect(0, 0, prefWidth, prefHeight), null)
        }

        val n = (max(bitmap.width, bitmap.height) / 800) + 1
        bitmap = GraphicsUtil.clipDown2(bitmap, n)
        var backColor: Int
        if (bitmap.width == 1 && bitmap.height == 1){
            backColor = bitmap.getPixel(0, 0)
        }else {
            val paint = Paint(Color.WHITE)
            paint.colorFilter = LightingColorFilter(0xFAFAFA, 0x050505)
            Canvas(bitmap).drawBitmap(bitmap, 0f, 0f, paint)
            val palette = try {
                Palette.from(bitmap).generate()
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
            // below: likely to get white for blurred images
            backColor = palette?.getDominantColor(Color.WHITE) ?: Color.WHITE
            backColor = ColorUtils.blendARGB(Color.BLACK, backColor, 0.98f)
        }
        val contrast1 = ColorUtils.calculateContrast(Color.WHITE, backColor)
        val contrast2 = ColorUtils.calculateContrast(Color.BLACK, backColor)
        colors[0] = if (contrast1 > contrast2) Color.WHITE else Color.BLACK
        colors[1] = backColor
        return colors
    }

    fun saveImage( image: Bitmap,  path: String): Boolean{
        val file = File(path)
        if (!com.madness.collision.util.F.prepare4(file)) return false
        return savePNG(image, path)
    }

    fun savePNG(image: Bitmap,  path: String): Boolean{
        val file = File(path)
        return try {
            val stream = FileOutputStream(file)
            image.compress(Bitmap.CompressFormat.PNG, com.madness.collision.util.P.WEBP_COMPRESS_QUALITY_FIRST, stream)
            true
        } catch ( e: FileNotFoundException) {
            e.printStackTrace()
            false
        }
    }

    fun curvedCard( card: CardView){
        card.measure()
        card.radius = (card.measuredHeight / 2).toFloat()
    }

    fun blurBitmap( context: Context,  src: Bitmap): Bitmap{
        return blur(context, src, src.width, src.height)
    }

    fun blurBitmap(context: Context,  src: Bitmap, width: Int, height: Int): Bitmap{
        return blur(context, src, width, height)
    }

    private fun blur(context: Context,  src: Bitmap, width: Int, height: Int): Bitmap{
        val radius = 20f //range from 0 to 20, the larger the blurrier
        val scale2 = 50 //scale to this size in advance, the smaller the blurrier, is 100 in exterior activity
        val blurred = GaussianBlur(context).blurOnce(toMin(src, scale2), radius)
        return toTarget(blurred, width, height)
    }

    fun copyText2Clipboard( context: Context,  content: CharSequence, toastId: Int){
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager? ?: return
        clipboard.setPrimaryClip(ClipData.newPlainText("link", content))
        toast(context, toastId, Toast.LENGTH_LONG)
    }

    const val DEV = Build.VERSION_CODES.CUR_DEVELOPMENT
    const val R = Build.VERSION_CODES.R
    const val Q = Build.VERSION_CODES.Q
    const val P = Build.VERSION_CODES.P
    const val O_MR1 = Build.VERSION_CODES.O_MR1
    const val O = Build.VERSION_CODES.O
    const val N_MR1 = Build.VERSION_CODES.N_MR1
    const val N = Build.VERSION_CODES.N
    const val M = Build.VERSION_CODES.M
    const val L_MR1 = Build.VERSION_CODES.LOLLIPOP_MR1
    const val L = Build.VERSION_CODES.LOLLIPOP
    const val K_WATCH = Build.VERSION_CODES.KITKAT_WATCH
    const val K = Build.VERSION_CODES.KITKAT
    const val J_MR2 = Build.VERSION_CODES.JELLY_BEAN_MR2
    const val J_MR1 = Build.VERSION_CODES.JELLY_BEAN_MR1
    const val J = Build.VERSION_CODES.JELLY_BEAN
    const val I_MR1 = Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1
    const val I = Build.VERSION_CODES.ICE_CREAM_SANDWICH
    const val H_MR2 = Build.VERSION_CODES.HONEYCOMB_MR2
    const val H_MR1 = Build.VERSION_CODES.HONEYCOMB_MR1
    const val H = Build.VERSION_CODES.HONEYCOMB
    const val G_MR1 = Build.VERSION_CODES.GINGERBREAD_MR1
    const val G = Build.VERSION_CODES.GINGERBREAD
    const val F = Build.VERSION_CODES.FROYO
    const val E_MR1 = Build.VERSION_CODES.ECLAIR_MR1
    const val E_0_1 = Build.VERSION_CODES.ECLAIR_0_1
    const val E = Build.VERSION_CODES.ECLAIR
    const val D = Build.VERSION_CODES.DONUT
    const val C = Build.VERSION_CODES.CUPCAKE
    const val B = Build.VERSION_CODES.BASE_1_1
    const val A = Build.VERSION_CODES.BASE

    fun aboveOff(apiLevel: Int): Boolean = Build.VERSION.SDK_INT > apiLevel

    fun aboveOn(apiLevel: Int): Boolean = Build.VERSION.SDK_INT >= apiLevel

    fun belowOff(apiLevel: Int): Boolean = Build.VERSION.SDK_INT < apiLevel

    fun belowOn(apiLevel: Int): Boolean = Build.VERSION.SDK_INT <= apiLevel

    fun makeGone(vararg views: View){
        for (view in views) if (view.visibility != View.GONE) view.visibility = View.GONE
    }

    fun toast( context: Context, messageRes: Int, duration: Int){
        toast(context, context.getString(messageRes), duration)
    }

    fun toast( context: Context,  message: CharSequence, duration: Int){
        GlobalScope.launch(Dispatchers.Main){
            if (!mainApplication.notificationAvailable) {
                if (context is Activity) popRequestNotification(context)
                else popNotifyNotification(context)
            }
            Toast.makeText(context, message, duration).show()
        }
    }

    fun popRequestNotification(activity: Activity){
        CollisionDialog(activity, MyRes.string.textGrantPermission).run {
            setTitleCollision(0, 0, 0)
            setContent(MyRes.string.textAsk4Notification)
            show()
            val packageName = activity.packageName
            setListener{
                dismiss()
                Intent().run {
                    action = "android.settings.APP_NOTIFICATION_SETTINGS"
                    if (aboveOn(O)) putExtra("android.provider.extra.APP_PACKAGE", packageName)
                    else {
                        putExtra("app_package", packageName)
                        putExtra("app_uid", activity.applicationInfo.uid)
                    }
                    activity.startActivity(this)
                }
            }
        }
    }

    fun popNotifyNotification(context: Context){
        CollisionDialog.alert(context, MyRes.string.textAsk4Notification).show()
    }

    fun write(content: String, path: String): Boolean{
        val ushContent = content.toByteArray(StandardCharsets.UTF_8)
        val file = File(path)
        if (!com.madness.collision.util.F.prepare4(file)) return false
        try {
            FileOutputStream(file).use { it.write(ushContent) }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return true
    }

    fun canAccessUsageStats(context: Context): Boolean {
        val applicationInfo = MiscApp.getApplicationInfo(context, packageName = context.packageName) ?: return false
        val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val re = if (aboveOn(Q)) appOpsManager.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, applicationInfo.uid, applicationInfo.packageName)
        else appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, applicationInfo.uid, applicationInfo.packageName)
        return re == AppOpsManager.MODE_ALLOWED
    }
}
