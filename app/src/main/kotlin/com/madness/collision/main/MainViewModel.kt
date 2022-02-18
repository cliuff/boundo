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

package com.madness.collision.main

import android.graphics.drawable.Drawable
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.madness.collision.Democratic
import com.madness.collision.unit.Unit
import com.madness.collision.util.mainApplication

/**
 * For [MainActivity]
 */
class MainViewModel: ViewModel(){
    val democratic: MutableLiveData<Democratic> = MutableLiveData()
    val action: MutableLiveData<Pair<String, Any?>> = MutableLiveData("" to null)
    val insetTop: MutableLiveData<Int> = MutableLiveData(mainApplication.insetTop)
    val insetBottom: MutableLiveData<Int> = MutableLiveData(mainApplication.insetBottom)
    val insetStart: MutableLiveData<Int> = MutableLiveData(mainApplication.insetStart)
    val insetEnd: MutableLiveData<Int> = MutableLiveData(mainApplication.insetEnd)
    val contentWidthTop: MutableLiveData<Int> = MutableLiveData(insetTop.value ?: 0)
    val contentWidthBottom: MutableLiveData<Int> = MutableLiveData(insetBottom.value ?: 0)
    val background: MutableLiveData<Drawable?> = MutableLiveData(null)
    val unit: MutableLiveData<Pair<Fragment, BooleanArray>> = MutableLiveData()
    private var _timestamp = 0L
    val timestamp: Long
        get() = _timestamp

    fun updateTimestamp() {
        _timestamp = System.currentTimeMillis()
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

    fun popUpBackStack(isFromNav: Boolean = false, shouldShowNavAfterBack: Boolean = false) {
    }

}
