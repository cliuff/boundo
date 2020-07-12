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

package com.madness.collision.unit.no_media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Typeface
import android.os.Environment
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.madness.collision.R
import com.madness.collision.databinding.AdapterNmBinding
import com.madness.collision.diy.SandwichAdapter
import com.madness.collision.main.MainViewModel
import com.madness.collision.unit.no_media.data.BasicInfo
import com.madness.collision.unit.no_media.data.Dir
import com.madness.collision.util.ThemeUtil
import com.madness.collision.util.dartFuture
import java.io.File

internal class Adapter(
        context: Context,
        private val mainViewModel: MainViewModel,
        private val folders: List<Dir>,
        private val foldersMap: Map<String, List<BasicInfo>>,
        private var itemWidth: Int,
        private var itemHeight: Int
) : SandwichAdapter<Adapter.NMHolder>(context) {

    class NMHolder(binding: AdapterNmBinding) : RecyclerView.ViewHolder(binding.root) {
        val img: ImageView = binding.nmAdapterCover
        val tvDir: AppCompatTextView = binding.nmAdapterDir as AppCompatTextView
        val folder: ConstraintLayout = binding.nmAdapterFolder
    }

    override var spanCount: Int = 1
        set(value) {
            if (value > 0) field = value
        }
    override val listCount: Int
        get() = folders.size

    override fun onCreateBodyItemViewHolder(parent: ViewGroup, viewType: Int): NMHolder {
        return NMHolder(AdapterNmBinding.inflate(LayoutInflater.from(context), parent, false))
    }

    override fun onMakeBody(holder: NMHolder, index: Int) {
        holder.folder.maxHeight = itemHeight

        val folder = folders[index]
        val dirLimit = 50
//        the root of the primary shared storage
//        used below Android 10
        val externalRoot = Environment.getExternalStorageDirectory()?.path ?: ""
        val dir = folder.getCleanPath(externalRoot).run {
            if (this.length > dirLimit) "..." + this.substring(this.length - dirLimit, this.length)
            else this
        }
        val builder = SpannableString(dir)
        val indexStart = dir.lastIndexOf(File.separator) + 1
        val indexEnd = dir.length
        builder.setSpan(StyleSpan(Typeface.BOLD), indexStart, indexEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        val colorAccent = ThemeUtil.getColor(context, R.attr.colorAccent)
        builder.setSpan(ForegroundColorSpan(colorAccent), indexStart, indexEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        holder.tvDir.dartFuture(builder)
        if (folder.images.isNotEmpty()) {
            Glide.with(context).load(folder.images[0].path)
                    .apply(RequestOptions().placeholder(R.drawable.img_gallery)
                            .centerCrop().override(itemWidth, itemHeight))
                    .into(holder.img)
        } else {
            holder.img.setImageBitmap(Bitmap.createBitmap(itemWidth, itemHeight, Bitmap.Config.ARGB_8888))
        }
        holder.folder.setOnClickListener {
            val files = foldersMap[folder.path] ?: emptyList()
            NmItemFragment.newInstance(folder.path, files, spanCount, itemWidth, itemHeight).let {
                mainViewModel.displayFragment(it)
            }
        }
    }

}
