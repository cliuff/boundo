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

import android.app.Activity
import android.graphics.drawable.Drawable
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.madness.collision.Democratic
import com.madness.collision.unit.Unit
import com.madness.collision.util.mainApplication
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

class NavPage(val fragment: Fragment, val args: BooleanArray)

fun Pair<Fragment, BooleanArray>.toPage() = NavPage(first, second)

/**
 * For [MainActivity]
 */
class MainViewModel: ViewModel(){
    private val _democratic: MutableSharedFlow<Democratic> = MutableSharedFlow()
    val democratic: Flow<Democratic> by ::_democratic
    val action: MutableLiveData<Pair<String, Any?>> = MutableLiveData("" to null)
    val insetTop: MutableLiveData<Int> = MutableLiveData(mainApplication.insetTop)
    val insetBottom: MutableLiveData<Int> = MutableLiveData(mainApplication.insetBottom)
    val insetStart: MutableLiveData<Int> = MutableLiveData(mainApplication.insetStart)
    val insetEnd: MutableLiveData<Int> = MutableLiveData(mainApplication.insetEnd)
    val contentWidthTop: MutableLiveData<Int> = MutableLiveData(insetTop.value ?: 0)
    val contentWidthBottom: MutableLiveData<Int> = MutableLiveData(insetBottom.value ?: 0)
    val background: MutableLiveData<Drawable?> = MutableLiveData(null)
    private val _page: MutableSharedFlow<NavPage> = MutableSharedFlow()
    val page: Flow<NavPage> by ::_page
    private var _timestamp = 0L
    val timestamp: Long by ::_timestamp
    private var lastDemocratic: WeakReference<Democratic> = WeakReference(null)

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
        val unit = Unit.getUnit(unitName, *mArgs) ?: return
        val p = unit to booleanArrayOf(shouldShowNavAfterBack, shouldExitAppAfterBack)
        viewModelScope.launch { _page.emit(p.toPage()) }
    }

    fun displayFragment(fragment: Fragment, shouldShowNavAfterBack: Boolean = false, shouldExitAppAfterBack: Boolean = false) {
        val p = fragment to booleanArrayOf(shouldShowNavAfterBack, shouldExitAppAfterBack)
        viewModelScope.launch { _page.emit(p.toPage()) }
    }

    fun democratize(democratic: Democratic) {
        viewModelScope.launch { _democratic.emit(democratic) }
        lastDemocratic = WeakReference(democratic)
    }

    fun popUpBackStack() {
        val democratic = lastDemocratic.get() ?: return
        val activity = when (democratic) {
            is Activity -> democratic
            is Fragment -> democratic.activity
            else -> null
        }
        activity ?: return
        lastDemocratic.clear()
        activity.finish()
    }

}
