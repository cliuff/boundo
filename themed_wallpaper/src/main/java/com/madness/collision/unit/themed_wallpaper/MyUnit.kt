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
        twCardLight.setOnClickListener {
            ExteriorFragment.newInstance(ExteriorFragment.MODE_TW_LIGHT).let { mainViewModel.displayFragment(it) }
        }
        twCardDark.setOnClickListener {
            ExteriorFragment.newInstance(ExteriorFragment.MODE_TW_DARK).let { mainViewModel.displayFragment(it) }
        }
        val context = context ?: return
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

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        democratize()
        mainViewModel.contentWidthTop.observe(viewLifecycleOwner){
            twRoot.alterPadding(top = it)
        }
    }

}
