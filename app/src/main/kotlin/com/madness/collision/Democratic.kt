package com.madness.collision

import android.content.Context
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar
import com.madness.collision.main.MainViewModel

/**
 * Used for fragments to implement their own tool bar interface.
 * This is the equivalent of [android.app.Activity.onCreateOptionsMenu], [android.app.Activity.onOptionsItemSelected]
 */
interface Democratic{
    fun createOptions(context: Context, toolbar: Toolbar, iconColor: Int): Boolean{
        return true
    }

    fun selectOption(item: MenuItem): Boolean {
        return false
    }

    fun tintOptionIcons(toolbar: Toolbar, iconColor: Int) {
        val menu =  toolbar.menu
        for (i in 0 until menu.size()) menu.getItem(i).icon?.setTint(iconColor)
    }

    fun inflateAndTint(menuResId: Int, toolbar: Toolbar, iconColor: Int) {
        toolbar.inflateMenu(menuResId)
        tintOptionIcons(toolbar, iconColor)
    }

    fun democratize(mainViewModel: MainViewModel) {
        mainViewModel.democratic.value = this
    }
}
