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

import android.content.ContentUris
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.observe
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.madness.collision.Democratic
import com.madness.collision.R
import com.madness.collision.main.MainViewModel
import com.madness.collision.settings.SettingsFunc
import com.madness.collision.unit.no_media.data.BasicInfo
import com.madness.collision.unit.no_media.data.Dir
import com.madness.collision.unit.no_media.data.Media
import com.madness.collision.util.*
import kotlinx.android.synthetic.main.nm_item.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.roundToInt
import com.madness.collision.unit.no_media.R as MyR

internal class NmItemFragment: Fragment(), Democratic, View.OnClickListener {

    companion object{
        const val EXTRA_PATH = "path"
        const val EXTRA_FILES = "files"
        const val EXTRA_SPAN_COUNT = "spanCount"
        const val EXTRA_WIDTH = "width"
        const val EXTRA_HEIGHT = "height"

        @JvmStatic
        fun newInstance(
                path: String, files: List<BasicInfo>, spanCount: Int, itemWidth: Int, itemHeight: Int
        ) = NmItemFragment().apply {
            arguments = Bundle().apply {
                putParcelableArray(EXTRA_FILES, files.toTypedArray())
                putString(EXTRA_PATH, path)
                putInt(EXTRA_WIDTH, itemWidth)
                putInt(EXTRA_HEIGHT, itemHeight)
                putInt(EXTRA_SPAN_COUNT, spanCount)
            }
        }
    }

    private lateinit var folder: Dir
    private var nm = false
        set(value) {
            field = value
            nmItemFab.isActivated = !value
        }

    private lateinit var manager: RecyclerView.LayoutManager

    private val mainViewModel: MainViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val context = context ?: return null
        SettingsFunc.updateLanguage(context)
        return inflater.inflate(MyR.layout.nm_item, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val context = context ?: return

        democratize(mainViewModel)
        mainViewModel.contentWidthBottom.observe(viewLifecycleOwner){
            (nmItemPath.layoutParams as ConstraintLayout.LayoutParams).run {
                this.bottomMargin = it + X.size(context, 10f, X.DP).roundToInt()
            }
        }

        val bundle: Bundle? = arguments
        if (bundle != null) {
            val spanCount = bundle.getInt(EXTRA_SPAN_COUNT)
            val itemWidth = bundle.getInt(EXTRA_WIDTH)
            val itemHeight = bundle.getInt(EXTRA_HEIGHT)

            val path = bundle.getString(EXTRA_PATH) ?: ""
            val list: MutableList<BasicInfo> = mutableListOf()
            bundle.getParcelableArray(EXTRA_FILES)?.forEach { if (it is BasicInfo) list.add(it) }
            folder = Dir(context, path, list, false)
            manager = GridLayoutManager(context, spanCount)
            nmItemRv.layoutManager = manager
            nmItemRv.adapter = ItemAdapter(context, folder.images, itemWidth, itemHeight).apply {
                this.spanCount = spanCount
                topCover = mainViewModel.contentWidthTop.value ?: 0
                bottomCover = mainViewModel.contentWidthBottom.value ?: 0
            }
            nmItemPath.text = folder.path
            nmItemProgressBar.visibility = View.GONE
            nm = folder.nm
            nmItemFab.setOnClickListener(this)
        } else {
            nmItemProgressBar.visibility = View.GONE
            notifyBriefly(R.string.text_error)
        }
    }

    override fun onClick(view: View) {
        val context = context ?: return
        if (view.id == MyR.id.nmItemFab) {
            // todo hidden paths list
            val folderNM = folder.nm
            if (!folderNM){
                val popConfirm = CollisionDialog(context, R.string.text_cancel, R.string.text_OK, true)
                popConfirm.setContent(MyR.string.nm_item_action_warning)
                popConfirm.setTitleCollision(0, 0, 0)
                popConfirm.show()
                popConfirm.setListener({
                    popConfirm.dismiss()
                }, {
                    popConfirm.dismiss()
                    try {
                        if (folder.nmFile.createNewFile()) {
                            nm = true
                            GlobalScope.launch { hideFolder(context, folder) }
                        } else throw Exception()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                })
            }else{
                notifyBriefly(R.string.text_error)
            }/*
            if (folderNM && folder.nmFile.delete()) {
                nm = false
                GlobalScope.launch { showFolder(context, folder) }
            } else {
                try {
                    if (!folderNM && folder.nmFile.createNewFile()) {
                        nm = true
                        GlobalScope.launch { hideFolder(context, folder) }
                    } else throw Exception()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }*/
        }
    }

    private fun showFolder(context: Context, folder: Dir) {
        MediaScannerConnection.scanFile(context, arrayOf(folder.path), null, null)
//        folder.images.forEach { insertFiles(it) }
        // todo restore hidden files
    }

    private fun hideFolder(context: Context, folder: Dir) {
        val fOri = File(folder.path)
        val pathToHide = folder.asDirectory + "hiddeneddih"
        val fToHide = File(pathToHide)
        fOri.renameTo(fToHide)
        folder.images.forEach { deleteFiles(context, it) }
        fToHide.renameTo(fOri)
    }

    private fun deleteImages(context: Context, vararg paths: String) {
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val selection = MediaStore.Images.Media.DATA + " = ?"
        val contentResolver = context.contentResolver
        val c = contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, selection, paths, null)
        if (c == null || !c.moveToFirst()) return
        do {
            // Deleting the item via the content provider will also remove the file
            val id = c.getLong(c.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
            val contUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val uri = ContentUris.withAppendedId(contUri, id)
            contentResolver.delete(uri, null, null)
        } while (c.moveToNext())
        c.close()
    }

    private fun insertFiles(context: Context, vararg files: Media) {
        files.forEach { file ->
            if (!file.isVideo) MediaStore.Images.Media.insertImage(context.contentResolver, file.path, file.name, "")
        }
    }

    private fun deleteFiles(context: Context, vararg files: Media) {
        files.forEach { file ->
            val contUri = if (file.isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val uri = ContentUris.withAppendedId(contUri, file.id)
            context.contentResolver.delete(uri, null, null)
        }
    }
}