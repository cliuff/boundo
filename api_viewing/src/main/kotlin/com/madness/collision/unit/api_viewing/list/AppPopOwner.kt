/*
 * Copyright 2022 Clifford Liu
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

import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.ui.info.AppInfoFragment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed interface PopUpState {
    object None : PopUpState
    class Pop(val app: ApiViewingApp) : PopUpState
}

interface AppPopOwner {
    var popState: PopUpState

    companion object {
        operator fun invoke() = object : AppPopOwner {
            override var popState: PopUpState = PopUpState.None
        }
    }
}

fun AppPopOwner.pop(fragment: Fragment, app: ApiViewingApp) {
    AppInfoFragment(app).show(fragment.childFragmentManager, AppInfoFragment.TAG)
    popState = PopUpState.Pop(app)
}

fun AppPopOwner.updateState(app: ApiViewingApp) {
    if (popState is PopUpState.Pop) {
        popState = PopUpState.Pop(app)
    }
}

private fun Fragment.observePopUp(popOwner: AppPopOwner) {
    val fragment = this
    lifecycle.addObserver(object : DefaultLifecycleObserver {
        override fun onResume(owner: LifecycleOwner) {
            val popState = popOwner.popState
            if (popState !is PopUpState.Pop) return
            owner.lifecycleScope.launch pop@{
                delay(350)
                if (owner.lifecycle.currentState < Lifecycle.State.STARTED) return@pop
                popOwner.pop(fragment, popState.app)
            }
        }

        override fun onPause(owner: LifecycleOwner) {
            val fMan = childFragmentManager
            val f = fMan.findFragmentByTag(AppInfoFragment.TAG)
            val pop = f as? BottomSheetDialogFragment?
            if (pop == null) popOwner.popState = PopUpState.None
            pop?.dismiss()
        }

        override fun onDestroy(owner: LifecycleOwner) {
            owner.lifecycle.removeObserver(this)
        }
    })
}

fun AppPopOwner.register(fragment: Fragment) = fragment.observePopUp(this)
