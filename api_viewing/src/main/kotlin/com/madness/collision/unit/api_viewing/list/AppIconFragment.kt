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

package com.madness.collision.unit.api_viewing.list

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.madness.collision.Democratic
import com.madness.collision.R
import com.madness.collision.main.MainViewModel
import com.madness.collision.misc.MiscApp
import com.madness.collision.unit.api_viewing.data.AppIcon
import com.madness.collision.unit.api_viewing.databinding.DialogApiSubAiBinding
import com.madness.collision.unit.api_viewing.util.ApkUtil
import com.madness.collision.unit.api_viewing.util.ManifestUtil
import com.madness.collision.util.*
import com.madness.collision.util.AppUtils.asBottomMargin
import com.madness.collision.util.os.OsUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import com.madness.collision.unit.api_viewing.R as MyR

internal class AppIconFragment : TaggedFragment(), Democratic {

    override val category: String = "AV"
    override val id: String = "AppIcon"

    companion object {
        const val ARG_APP_NAME = "Name"
        const val ARG_PACKAGE_NAME = "PackageName"
        const val ARG_APK_Path = "ApkPath"
        const val ARG_IS_ARCHIVE = "IsArchive"

        @JvmStatic
        fun newInstance(appName: String, packageName: String, path: String, isArchive: Boolean) = AppIconFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_APP_NAME, appName)
                putString(ARG_PACKAGE_NAME, packageName)
                putString(ARG_APK_Path, path)
                putBoolean(ARG_IS_ARCHIVE, isArchive)
            }
        }
    }

    private val mainViewModel: MainViewModel by activityViewModels()
    private var iconWidth: Int = 0
    private lateinit var viewBinding: DialogApiSubAiBinding

    override fun createOptions(context: Context, toolbar: Toolbar, iconColor: Int): Boolean {
        mainViewModel.configNavigation(toolbar, iconColor)
        toolbar.title = arguments?.getString(ARG_APP_NAME) ?: ""
        inflateAndTint(MyR.menu.toolbar_av_icon, toolbar, iconColor)
        return true
    }

    override fun selectOption(item: MenuItem): Boolean {
        when (item.itemId) {
            MyR.id.avIconToolbarManual -> {
                val context = context ?: return false
                CollisionDialog.alert(context, MyR.string.apiInfoAiOverallDes).show()
                return true
            }
        }
        return false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewBinding = DialogApiSubAiBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val context = context ?: return

        democratize(mainViewModel)
        mainViewModel.contentWidthTop.observe(viewLifecycleOwner) {
            viewBinding.apiInfoAiGuideTop.setGuidelineBegin(it)
        }
        mainViewModel.contentWidthBottom.observe(viewLifecycleOwner) {
            viewBinding.apiInfoAiGuideBottom.setGuidelineEnd(asBottomMargin(it))
//            (apiInfoAiSpace.layoutParams as ConstraintLayout.LayoutParams).height = it
//            apiInfoAiSpace.layoutParams = ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, it)
        }

        lifecycleScope.launch(Dispatchers.Default) {
            val appName = arguments?.getString(ARG_APP_NAME) ?: ""
            val packageName = arguments?.getString(ARG_PACKAGE_NAME) ?: ""
            val apkPath = arguments?.getString(ARG_APK_Path) ?: ""
            val isArchive = arguments?.getBoolean(ARG_IS_ARCHIVE) ?: false

            val applicationInfo = if (isArchive) MiscApp.getApplicationInfo(context, apkPath = apkPath)
            else MiscApp.getApplicationInfo(context, packageName = packageName)
            applicationInfo ?: return@launch

            var idIcon = 0
            var idIconR = 0
            var dIcon: Drawable? = null
            var dIconR: Drawable? = null
            // reuse this resources object
            val res = context.packageManager.getResourcesForApplication(applicationInfo)
            try {
                val resID = ManifestUtil.getManifestAttr(apkPath, arrayOf("application", "icon"))
                if (resID.isNotEmpty()) {
                    idIcon = resID.toInt()
                    dIcon = ResourcesCompat.getDrawable(res, idIcon, null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                val resID = ManifestUtil.getManifestAttr(apkPath, arrayOf("application", "roundIcon"))
                if (resID.isNotEmpty()) {
                    idIconR = resID.toInt()
                    dIconR = ResourcesCompat.getDrawable(res, idIconR, null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            if (dIcon == null) {
                dIcon = applicationInfo.loadIcon(context.packageManager)
                launch(Dispatchers.Main) {
                    viewBinding.apiInfoAiIconDes.setText(MyR.string.apiInfoAiIconDesSys)
                }
            }

            iconWidth = X.size(context, 72f, X.DP).toInt()

            launch(Dispatchers.Main) {
                viewBinding.run {
                    setIcon(dIcon, res, idIcon, apkPath, appName,
                        apiInfoAiIcon, apiInfoAiIconFore, apiInfoAiIconBack,
                        apiInfoAiIconRound, apiInfoAiIconRounded, apiInfoAiIconSquircle,
                        apiInfoAiIconGroup, apiInfoAiIconGroupAi, apiInfoAiMono, apiInfoAiMonoDes)

                    setIcon(dIconR, res, idIconR, apkPath, "$appName-R",
                        apiInfoAiIconR, apiInfoAiIconRFore, apiInfoAiIconRBack,
                        apiInfoAiIconRRound, apiInfoAiIconRRounded, apiInfoAiIconRSquircle,
                        apiInfoAiIconRGroup, apiInfoAiIconRGroupAi, apiInfoAiMonoR, apiInfoAiMonoRDes)
                }
            }
        }
    }

    private fun setIcon(drawable: Drawable?, res: Resources, iconId: Int, apkPath: String, exportPrefix: String,
                        vIcon: ImageView, vIconFore: ImageView, vIconBack: ImageView,
                        vIconRound: TextView, vIconRounded: TextView, vIconSquircle: TextView,
                        vIconGroup: View, vAiGroup: View, vMono: ImageView, vMonoLabel: TextView) {
        if (drawable == null) {
            arrayOf(vIconGroup, vAiGroup, vMono, vMonoLabel).forEach { it.isGone = true }
            return
        }
        val context = context ?: return
        val icon = AppIcon(context, drawable)
        vIcon.setIcon(icon.bitmap, res, iconId, apkPath, "$exportPrefix-Full")
        vIconGroup.visibility = View.VISIBLE
        if (!icon.isAdaptive || !X.aboveOn(X.O)) {
            arrayOf(vAiGroup, vMono, vMonoLabel).forEach { it.isGone = true }
            return
        }
        val logoDrawable = icon.drawable as AdaptiveIconDrawable
        val mono = if (OsUtils.satisfy(OsUtils.T)) logoDrawable.monochrome else null
        if (mono == null) {
            arrayOf(vMono, vMonoLabel).forEach { it.isGone = true }
        } else {
            vMono.setIconLayer(mono, "$exportPrefix-Mono")
            arrayOf(vMono, vMonoLabel).forEach { it.isVisible = true }
        }
        logoDrawable.foreground?.let { vIconFore.setIconLayer(it, "$exportPrefix-Fore") }
        logoDrawable.background?.let { vIconBack.setIconLayer(it, "$exportPrefix-Back") }
        vIconRound.setShapedIcon(icon.round, "$exportPrefix-Round")
        vIconRounded.setShapedIcon(icon.rounded, "$exportPrefix-Rounded")
        vIconSquircle.setShapedIcon(icon.squircle, "$exportPrefix-Squircle")
        vAiGroup.visibility = View.VISIBLE
    }

    private fun ImageView.setIcon(icon: Bitmap, res: Resources, iconId: Int, apkPath: String, exportName: String) {
        setImageBitmap(icon)
        val context = context ?: return
        setOnLongClickListener {
            actionIcon(context, icon, exportName)
            true
        }
        if (iconId == 0) return
        setOnClickListener {
            lifecycleScope.launch(Dispatchers.Default) {
                val (title, entries) = ApkUtil.getResourceEntries(res, iconId, apkPath)
                launch(Dispatchers.Main) launchMain@ {
                    if (title.isEmpty()) {
                        CollisionDialog.alert(context, R.string.text_no_content).show()
                        return@launchMain
                    }
                    val content = if (entries.isEmpty()) title
                    else "$title\n\n" + entries.joinToString("\n")
                    CollisionDialog.alert(context, content).show()
                }
            }
        }
    }

    private fun ImageView.setIconLayer(icon: Drawable, exportName: String) {
        setImageDrawable(icon)
        val context = context ?: return
        setOnLongClickListener {
            actionIcon(context, icon, exportName)
            true
        }
    }

    private fun TextView.setShapedIcon(icon: Bitmap, exportName: String) {
        val context = context ?: return
        val drawable = BitmapDrawable(context.resources, icon)
        drawable.setBounds(0, 0, iconWidth, iconWidth)
        setCompoundDrawablesRelative(null, drawable, null, null)
        setOnLongClickListener {
            actionIcon(context, icon, exportName)
            true
        }
    }

    private fun actionIcon(context: Context, imgIcon: Drawable, exportName: String){
        actionIcon(context, X.drawableToBitmap(imgIcon), exportName)
    }

    private fun actionIcon(context: Context, imgIcon: Bitmap, exportName: String){
        val path = F.createPath(F.cachePublicPath(context), "App", "Logo", "${exportName}.png")
        val image = File(path)
        imgIcon.let { if (F.prepare4(image)) X.savePNG(it, path) }
        val uri: Uri = image.getProviderUri(context)
        childFragmentManager.let {
            FilePop.by(context, uri, "image/png", R.string.textShareImage, uri, exportName).show(it, FilePop.TAG)
        }
    }

}
