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

package com.madness.collision.unit.themed_wallpaper

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.observe
import com.madness.collision.R
import com.madness.collision.settings.ExteriorFragment
import com.madness.collision.unit.Unit
import com.madness.collision.util.F
import com.madness.collision.util.ImageUtil
import com.madness.collision.util.X
import com.madness.collision.util.alterPadding
import kotlinx.android.synthetic.main.unit_themed_wallpaper.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import com.madness.collision.unit.themed_wallpaper.R as MyR

class MyUnit: Unit() {

    override fun createOptions(context: Context, toolbar: Toolbar, iconColor: Int): Boolean {
        toolbar.setTitle(R.string.twService)
        inflateAndTint(MyR.menu.toolbar_tw, toolbar, iconColor)
        return true
    }

    override fun selectOption(item: MenuItem): Boolean {
        when (item.itemId){
            MyR.id.twToolbarDone -> {
                val context = context ?: return false
                val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
                intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, ComponentName(context, ThemedWallpaperService::class.java))
                startActivity(intent)
                return true
            }
        }
        return false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(MyR.layout.unit_themed_wallpaper, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val context = context ?: return
        val mViewModel: TwViewModel by activityViewModels()
        mViewModel.isWallpaperChanged.observe(viewLifecycleOwner) { isChanged ->
            if (isChanged != true) return@observe
            GlobalScope.launch {
                val pathLight = F.valFilePubTwPortrait(context)
                val pathDark = F.valFilePubTwPortraitDark(context)
                val imgLight = ImageUtil.getBitmap(pathLight)
                val imgDark = ImageUtil.getBitmap(pathDark)
                val width: Int by lazy { X.size(context, 150f, X.DP).roundToInt() }
                val imageDefaultLight by lazy {
                    X.toTarget(X.drawableToBitmap(ColorDrawable(Color.WHITE)), width, width)
                }
                val imageDefaultDark by lazy {
                    X.toTarget(X.drawableToBitmap(ColorDrawable(Color.BLACK)), width, width)
                }
                launch(Dispatchers.Main){
                    twImgLight.setImageBitmap(imgLight ?: imageDefaultLight)
                    twImgDark.setImageBitmap(imgDark ?: imageDefaultDark)
                }
            }
        }

        mViewModel.isWallpaperChanged.value = true
        twCardLight.setOnClickListener {
            ExteriorFragment.newInstance(ExteriorFragment.MODE_TW_LIGHT).let { mainViewModel.displayFragment(it) }
        }
        twCardDark.setOnClickListener {
            ExteriorFragment.newInstance(ExteriorFragment.MODE_TW_DARK).let { mainViewModel.displayFragment(it) }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        democratize()
        mainViewModel.contentWidthTop.observe(viewLifecycleOwner){
            twRoot.alterPadding(top = it)
        }
    }

}
