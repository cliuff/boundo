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

package com.madness.collision.unit.api_viewing.tag.inflater

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.get
import androidx.core.view.size
import androidx.core.view.updatePaddingRelative
import coil.Coil
import coil.request.ImageRequest
import com.madness.collision.R
import com.madness.collision.chief.chiefPkgMan
import com.madness.collision.misc.PackageCompat
import com.madness.collision.unit.api_viewing.databinding.AdapterAvTagBinding
import com.madness.collision.util.ThemeUtil
import com.madness.collision.util.X
import com.madness.collision.util.ui.AppIconPackageInfo
import kotlinx.coroutines.runBlocking
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

internal object AppTagInflater {
    private val _tagIcons: MutableMap<String, Bitmap> = mutableMapOf()
    val tagIcons: Map<String, Bitmap>
        get() = _tagIcons
    private var colorBackground: Int? = null

    fun clearCache() {
        synchronized(_tagIcons) {
            _tagIcons.clear()
        }
    }

    fun clearContext() {
        colorBackground = null
    }

    private fun getTagColor(context: Context): Int {
        return colorBackground ?: ThemeUtil.getColor(context, R.attr.colorAOnBackground).let {
            ColorUtils.setAlphaComponent(it, 14)
        }.also { colorBackground = it }
    }

    private fun makeTagIcon(context: Context, resId: Int): Bitmap? {
        val drawable = try {
            ContextCompat.getDrawable(context, resId)
        } catch (e: Resources.NotFoundException) {
            e.printStackTrace()
            null
        } ?: return null
        return makeTagIcon(context, drawable)
    }

    private fun makeTagIcon(context: Context, drawable: Drawable): Bitmap? {
        val width = X.size(context, 12f, X.DP).roundToInt()
        val bitmap = Bitmap.createBitmap(width, width, Bitmap.Config.ARGB_8888)
        drawable.setBounds(0, 0, width, width)
        drawable.draw(Canvas(bitmap))
        return bitmap
    }

    fun ensureTagIcon(context: Context, key: String, resId: Int): Bitmap? {
        val icon = tagIcons[key]
        if (icon != null) return icon
        val re = makeTagIcon(context, resId)
        if (re != null) synchronized(_tagIcons) {
            _tagIcons[key] = re
        }
        return re
    }

    fun ensureTagIcon(context: Context, key: String, drawable: Drawable): Bitmap? {
        val icon = tagIcons[key]
        if (icon != null) return icon
        val re = makeTagIcon(context, drawable)
        if (re != null) synchronized(_tagIcons) {
            _tagIcons[key] = re
        }
        return re
    }

    fun ensureTagIcon(context: Context, pkgName: String): Bitmap? {
        val icon = tagIcons[pkgName]
        if (icon != null) return icon
        val pkgInfo = try {
            PackageCompat.getInstalledPackage(chiefPkgMan, pkgName) ?: return null
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            return null
        }
        val req = ImageRequest.Builder(context).data(AppIconPackageInfo(pkgInfo)).build()
        val iconDrawable = runBlocking { Coil.imageLoader(context).execute(req).drawable }
        return if (iconDrawable != null) ensureTagIcon(context, pkgName, iconDrawable) else null
    }

    class TagInfo(val nameResId: Int? = null, val name: String? = null, val icon: Icon? = null, val rank: String) {
        class Icon(
            val bitmap: Bitmap? = null,
            val text: CharSequence? = null,
            val isExternal: Boolean = false,  // not built-in
        )
    }

    class TagRank(val name: String?, val rank: String)

    fun inflateTag(context: Context, parent: ViewGroup, tagInfo: TagInfo) {
        val name = tagInfo.name ?: tagInfo.nameResId?.let { context.getString(it) }
        var tagPosition = 0
        for (i in 0 until parent.size) {
            val child = parent[i]
            if (child !is LinearLayout) continue
            val rank = child.tag as TagRank
            if (rank.name == name) return
            // get appropriate index
            if (tagInfo.rank > rank.rank) tagPosition = max(tagPosition, i + 1)
            else if (tagInfo.rank < rank.rank) tagPosition = min(tagPosition, i)
        }
        // reset to last index (equals to size)
        if (tagPosition !in 0..parent.size) tagPosition = parent.size

        val inflater = LayoutInflater.from(context)
        AdapterAvTagBinding.inflate(inflater, parent, false).apply {
            avAdapterInfoTag.tag = TagRank(name, tagInfo.rank)
            avAdapterInfoTag.background.setTint(getTagColor(context))
            val icon = tagInfo.icon
            when {
                icon?.bitmap != null -> {
                    // Built-in icons are designed as 54/72 (content/size).
                    // So add extra 1/8 of icon view size as padding to external icon.
                    if (icon.isExternal) {
                        val extraPadding = X.size(context, 12/8f, X.DP).roundToInt()
                        avAdapterInfoTagIcon.updatePaddingRelative(top = extraPadding, bottom = extraPadding)
                    }
                    avAdapterInfoTagIcon.setImageBitmap(icon.bitmap)
                    avAdapterInfoTagName.visibility = View.GONE
                }
                icon?.text != null -> {
                    avAdapterInfoTagName.text = icon.text
                    avAdapterInfoTagIcon.visibility = View.GONE
                }
                else -> {
                    avAdapterInfoTagName.text = name
                    avAdapterInfoTagIcon.visibility = View.GONE
                }
            }
        }.let { parent.addView(it.root, tagPosition) }
    }
}