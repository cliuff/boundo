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
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.madness.collision.R
import com.madness.collision.databinding.FileActionsBinding
import java.io.File

class FilePop: BottomSheetDialogFragment(){
    companion object{
        const val TAG = "FilePop"
        private const val ARG_INTENT = "argIntent"

        fun newInstance(intent: Intent) = FilePop().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_INTENT, intent)
            }
        }
        
        const val ACTION_FILE = "byFile"
        const val ACTION_URI = "byUri"
        const val REQUEST_SAVE_FILE = 0

        fun by(context: Context, fileUri: Uri, fileType: String, title: String, imageUri: Uri? = null, imageLabel: String = ""): FilePop{
            val intent = Intent().apply {
                setDataAndType(fileUri, fileType)
                putExtra(Intent.EXTRA_TITLE, title)
                if (imageUri != null) clipData = ClipData.newUri(context.contentResolver, imageLabel, imageUri)
            }
            return newInstance(intent)
        }

        fun by(context: Context, fileUri: Uri, fileType: String, titleId: Int, imageUri: Uri? = null, imageLabel: String = ""): FilePop{
            return by(context, fileUri, fileType, context.getString(titleId), imageUri, imageLabel)
        }

        fun by(context: Context, file: File, fileType: String, title: String, imageUri: Uri? = null, imageLabel: String = ""): FilePop{
            return by(context, file.getProviderUri(context), fileType, title, imageUri, imageLabel)
        }

        fun by(context: Context, file: File, fileType: String, titleId: Int, imageUri: Uri? = null, imageLabel: String = ""): FilePop{
            return by(context, file, fileType, context.getString(titleId), imageUri, imageLabel)
        }
    }

    private lateinit var mContext: Context
    private lateinit var fileUri: Uri
    private lateinit var file: File
    private var fileType = "*/*"
    private var title = ""
    private var imageUri: Uri? = null
    private var imageLabel = ""

    private lateinit var mViews: FileActionsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AppTheme_Pop)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mViews = FileActionsBinding.inflate(inflater, container, false)
        return mViews.root
    }

    override fun onStart() {
        super.onStart()
        val context = context ?: return
        val rootView = view ?: return
        BottomSheetBehavior.from(rootView.parent as View).configure(context)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        mContext = context ?: return

        dialog?.window?.let {
            val transparentNavBar = mainApplication.insetBottom == 0
            val isDarkNav = if (transparentNavBar) false else mainApplication.isPaleTheme
            SystemUtil.applyEdge2Edge(it)
            SystemUtil.applyStatusBarColor(mContext, it, false, isTransparentBar = true)
            val colorSurface = ThemeUtil.getColor(mContext, R.attr.colorASurface)
            val color = if (isDarkNav && X.belowOff(X.O)) ColorUtil.darkenAs(colorSurface, 0.9f) else colorSurface
            SystemUtil.applyNavBarColor(mContext, it, isDarkNav, transparentNavBar, color = color)
        }

//        val marginBottom = X.size(mContext, 15f, X.DP).roundToInt()
//        if (mainApplication.insetBottom < marginBottom) mViews.sdkInfoGuidelineBottom.setGuidelineEnd(marginBottom - mainApplication.insetBottom)

        val intent = arguments?.getParcelable(ARG_INTENT) ?: Intent()
        when {
            intent.action == ACTION_FILE -> {
                file = intent.getSerializableExtra(Intent.EXTRA_STREAM) as File
                fileUri = file.getProviderUri(mContext)
            }
            intent.data == null -> {
                dismiss()
                return
            }
            else -> fileUri = intent.data!!
        }
        intent.type?.let { fileType = it }
        intent.getStringExtra(Intent.EXTRA_TITLE)?.let { title = it }
        intent.clipData?.run {
            val clipItem = getItemAt(0)
            imageUri = clipItem.uri
            imageLabel = description.label.toString()
        }
        loadData()
        mViews.fileActionsOpen.setOnClickListener { open() }
        mViews.fileActionsShare.setOnClickListener { share() }
        mViews.fileActionsSave.setOnClickListener { save() }
    }

    private fun loadData() {
        val matchRe = "(.+)/(.+)".toRegex().find(fileType)
        val displayFileType = if (matchRe != null) {
            val (cat, type) = matchRe.destructured
            when (cat) {
                "image" -> when (type) {
                    "png" -> "PNG"
                    "jpeg" -> "JPEG"
                    "webp" -> "WEBP"
                    "heif" -> "HEIF"
                    "*" -> "IMG"
                    else -> fileType
                }
                "text" -> when (type) {
                    "csv" -> "CSV"
                    "html" -> "HTML"
                    "calendar" -> "ICS"
                    "plain" -> "TXT"
                    else -> "TXT"
                }
                "application" -> when (type) {
                    "vnd.android.package-archive" -> "APK"
                    else -> fileType
                }
                "resource" -> when (type) {
                    "folder" -> ""
                    else -> fileType
                }
                "*" -> when (type) {
                    "*" -> ""
                    else -> fileType
                }
                else -> fileType
            }
        } else fileType
        var hasNoTitle = false
        if (imageUri != null) {
            mViews.fileActionsInfoImage.setImageURI(imageUri)
        } else {
            mViews.fileActionsInfoImage.visibility = View.GONE
        }
        if (imageLabel.isNotEmpty()) {
            mViews.fileActionsInfoTitle.text = imageLabel
        } else {
            val fileName = fileUri.lastPathSegment ?: fileUri.path
            if (fileName != null) {
                mViews.fileActionsInfoTitle.text = fileName
            } else {
                mViews.fileActionsInfoTitle.text = displayFileType
                hasNoTitle = true
            }
        }
        if (displayFileType.isNotEmpty()) {
            // Normal title case, set file type
            if (!hasNoTitle) mViews.fileActionsInfoSubtitle.text = displayFileType
            // Title set as file type, hide subtitle
            else mViews.fileActionsInfoSubtitle.visibility = View.GONE
        } else {
            // Normal title case, hide subtitle
            if (!hasNoTitle) mViews.fileActionsInfoSubtitle.visibility = View.GONE
            // Title set as file type, hide file info
            else mViews.fileActionsInfo.visibility = View.GONE
        }
    }

    private fun open(){
        try {
            val intent = Intent().apply {
                action = Intent.ACTION_VIEW
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                setDataAndType(fileUri, fileType)
            }
            startActivity(intent)
        } catch (e: Exception){
            e.printStackTrace()
            notifyBriefly(R.string.text_error)
        }
        dismiss()
    }

    private fun share(){
        startActivity(Intent().apply {
            action = Intent.ACTION_SEND
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            type = fileType
            putExtra(Intent.EXTRA_STREAM, fileUri)
            // set android q preview title
            if (X.aboveOn(X.Q) && title.isNotBlank()) putExtra(Intent.EXTRA_TITLE, title)
            // set android q preview image
            if (X.aboveOn(X.Q) && imageUri != null) clipData = ClipData.newUri(mContext.contentResolver, imageLabel, imageUri)
        }.let { Intent.createChooser(it, title) }) // deprecated in android 10
        dismiss()
    }

    private fun save() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            type = fileType
            val fileName = fileUri.lastPathSegment ?: "file"
            putExtra(Intent.EXTRA_TITLE, fileName)
        }
        startActivityForResult(intent, REQUEST_SAVE_FILE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_SAVE_FILE){
            if (resultCode != Activity.RESULT_OK || data?.data == null) {
                dismiss()
                return
            }
            val uri = data.data!!
            mContext.contentResolver.openOutputStream(uri).use { out ->
                out ?: return
                mContext.contentResolver.openInputStream(fileUri).use {
                    it?.copyTo(out)
                }
            }
            dismiss()
        }
    }
}
