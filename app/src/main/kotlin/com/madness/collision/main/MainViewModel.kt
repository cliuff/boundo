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

package com.madness.collision.main

import android.graphics.drawable.Drawable
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.madness.collision.Democratic
import com.madness.collision.unit.Unit
import com.madness.collision.util.mainApplication
import com.madness.collision.util.measure
import java.lang.ref.WeakReference

class MainViewModel: ViewModel(){
    val democratic: MutableLiveData<Democratic> = MutableLiveData()
    val action: MutableLiveData<Pair<String, Any?>> = MutableLiveData("" to null)
    val insetTop: MutableLiveData<Int> = MutableLiveData(mainApplication.insetTop)
    val insetBottom: MutableLiveData<Int> = MutableLiveData(mainApplication.insetBottom)
    val insetLeft: MutableLiveData<Int> = MutableLiveData(mainApplication.insetLeft)
    val insetRight: MutableLiveData<Int> = MutableLiveData(mainApplication.insetRight)
    val contentWidthTop: MutableLiveData<Int> = MutableLiveData(insetTop.value ?: 0)
    val contentWidthBottom: MutableLiveData<Int> = MutableLiveData(insetBottom.value ?: 0)
    val background: MutableLiveData<Drawable?> = MutableLiveData(null)
    var navViewRef: WeakReference<View?>? = null
    val navView: View?
        get() = navViewRef?.get()
    val unit: MutableLiveData<Pair<Fragment, BooleanArray>> = MutableLiveData()
    var popUpBackStackFun: ((isFromNav: Boolean, shouldShowNavAfterBack: Boolean) -> kotlin.Unit)? = null
    private var _timestamp = 0L
    val timestamp: Long
        get() = _timestamp

    fun updateTimestamp() {
        _timestamp = System.currentTimeMillis()
    }

    val sideNavWidth: Int
        get() {
            val sideNav = navView ?: return 0
            val width = sideNav.width
            if (width != 0) return width
            // view has not been inflated yet
            sideNav.measure()
            return sideNav.measuredWidth
        }

    fun displayUnit(unitName: String, shouldShowNavAfterBack: Boolean = false, shouldExitAppAfterBack: Boolean = false, vararg args: Any?) {
        val mArgs = if (args.isEmpty()) {
            val bridge = Unit.getBridge(unitName) ?: return
            val classArgs = bridge.args
            Array<Any?>(classArgs.size) { null }
        } else {
            args
        }
        unit.value = Unit.getUnit(unitName, *mArgs)?.run { this to booleanArrayOf(shouldShowNavAfterBack, shouldExitAppAfterBack)}
    }

    fun displayFragment(fragment: Fragment, shouldShowNavAfterBack: Boolean = false, shouldExitAppAfterBack: Boolean = false) {
        unit.value = fragment to booleanArrayOf(shouldShowNavAfterBack, shouldExitAppAfterBack)
    }

    fun removeCurrentUnit() {
        unit.value?.first?.let {
            if (!it.isAdded) return@let
            it.parentFragmentManager.beginTransaction().remove(it).commitNow()
        }
        unit.value = null
    }

    fun popUpBackStack(isFromNav: Boolean = false, shouldShowNavAfterBack: Boolean = false) {
        popUpBackStackFun?.invoke(isFromNav, shouldShowNavAfterBack)
    }

}
