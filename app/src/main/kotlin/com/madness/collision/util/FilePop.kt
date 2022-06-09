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

package com.madness.collision.util

import android.app.Activity
import android.content.ClipData
import android.content.ClipDescription
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Bundle
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updatePaddingRelative
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.madness.collision.R
import com.madness.collision.databinding.FileActionsBinding
import com.madness.collision.diy.WindowInsets
import com.madness.collision.util.os.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileNotFoundException
import kotlin.math.max
import kotlin.math.roundToInt

class FilePop: BottomSheetDialogFragment(), SystemBarMaintainerOwner {
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
                else if (imageLabel.isNotEmpty()) clipData = ClipData.newPlainText("File title", imageLabel)
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
    override val systemBarMaintainer: SystemBarMaintainer = DialogFragmentSystemBarMaintainer(this)
    private val edgeToEdge = BottomSheetEdgeToEdge(this, this::consumeInsets) { activity?.window }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AppTheme_Pop)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        mViews = FileActionsBinding.inflate(inflater, container, false)
        return mViews.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val context = context ?: return
        edgeToEdge.applyInsets(view, context)
    }

    override fun onStart() {
        super.onStart()
        val context = context ?: return
        val rootView = view ?: return
        BottomSheetBehavior.from(rootView.parent as View).configure(context)
    }

    private fun consumeInsets(insets: WindowInsets) {
        val context = context ?: return
        val minMargin = X.size(context, 12f, X.DP).roundToInt()
        val extraMargin = max(insets.bottom, minMargin)
        mViews.fileActionsContainer.updatePaddingRelative(bottom = extraMargin)
        mViews.fileActionsRoot.updatePaddingRelative(start = insets.start, end = insets.end)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mContext = context ?: return

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
        // Determine mime type
        val type = FileUtils.getType(mContext, fileUri)
        if (type.isNotEmpty()) {
            fileType = type
        } else {
            val intentType = intent.type
            if (intentType != null) fileType = intentType
        }
        val intentTitle = intent.getStringExtra(Intent.EXTRA_TITLE)
        if (intentTitle != null) title = intentTitle
        val intentClipData = intent.clipData
        if (intentClipData != null) {
            val clipItem = intentClipData.getItemAt(0)
            val clipDesc = intentClipData.description
            if (clipDesc.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
                imageLabel = clipItem.text.toString()
            } else {
                imageUri = clipItem.uri
                imageLabel = intentClipData.description.label.toString()
            }
        }
        loadData()
        mViews.fileActionsOpen.setOnClickListener { open() }
        mViews.fileActionsShare.setOnClickListener { share() }
        mViews.fileActionsSave.setOnClickListener { save() }
    }

    private fun getTypeLabel(fileType: String): String {
        val matchRe = "(.+)/(.+)".toRegex().find(fileType)
        return if (matchRe != null) {
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
    }

    private fun loadData() {
        // Icon is available on M+
        var typeIcon: Any? = null
        val displayFileType = if (X.aboveOn(X.Q)) {
            val info = FileUtils.getTypeInfo(mContext, fileUri)
            typeIcon = info.icon
            info.label
        } else getTypeLabel(fileType)
        var hasNoTitle = false
        if (imageUri != null) {
            mViews.fileActionsInfoImage.setImageURI(imageUri)
        } else {
            mViews.fileActionsInfoImage.visibility = View.GONE
        }
        if (imageLabel.isNotEmpty()) {
            mViews.fileActionsInfoTitle.text = imageLabel
        } else {
            val uriName = FileUtils.getName(mContext, fileUri)
            val fileName = if (uriName.isNotEmpty()) uriName
            else fileUri.lastPathSegment ?: fileUri.path
            if (fileName != null) {
                mViews.fileActionsInfoTitle.text = fileName
            } else {
                mViews.fileActionsInfoTitle.text = displayFileType
                hasNoTitle = true
            }
        }
        val fileSize = FileUtils.getSize(mContext, fileUri)
        val displayFileSize = Formatter.formatFileSize(mContext, fileSize)
        if (!hasNoTitle) {
            val subtitle = if (displayFileType.isEmpty()) displayFileSize
            else "$displayFileType • $displayFileSize"
            // Normal title case, set file type and size
            mViews.fileActionsInfoSubtitle.text = subtitle
        } else {
            // Title set as file type, show size only
            mViews.fileActionsInfoSubtitle.text = displayFileSize
        }
        if (typeIcon != null && X.aboveOn(X.M) && typeIcon is Icon) lifecycleScope.launch(Dispatchers.Default) {
            val d = typeIcon.loadDrawable(mContext)
            launch(Dispatchers.Main) {
                mViews.fileActionsInfoSubtitle.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        d, null, null, null
                )
            }
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
        if (requestCode == REQUEST_SAVE_FILE) {
            if (resultCode != Activity.RESULT_OK || data?.data == null) {
                dismiss()
                return
            }
            val uri = data.data!!
            val contentResolver = mContext.contentResolver
            var isSuccess = false
            try {
                contentResolver.openOutputStream(uri)?.use { out ->
                    contentResolver.openInputStream(fileUri)?.use {
                        it.copyTo(out)
                        isSuccess = true
                    }
                }
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            }
            dismiss()
            if (!isSuccess) notifyBriefly(R.string.text_error)
        }
    }
}
