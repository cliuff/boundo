/*
 * Copyright 2025 Clifford Liu
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

package com.madness.collision.chief.app

import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import java.lang.ref.WeakReference

fun interface ContentComposable {
    @Suppress("ComposableNaming")
    @Composable
    fun content()
}

/** The route that is passed to [ComposePageActivity]. */
interface ComposePageRoute : ContentComposable, Parcelable


interface ComposePageNavController {
    fun navigateTo(route: ComposePageRoute)
    fun navigateBack()
}

@Suppress("FunctionName")
fun ActivityPageNavController(hostActivity: ComponentActivity): ComposePageNavController =
    CompPageNavControllerImpl(hostActivity)

val LocalPageNavController =
    staticCompositionLocalOf<ComposePageNavController> { PseudoNavController() }


internal class PseudoNavController : ComposePageNavController {
    override fun navigateTo(route: ComposePageRoute) = throw NotImplementedError()
    override fun navigateBack() = throw NotImplementedError()
}

internal class CompPageNavControllerImpl(hostActivity: ComponentActivity) : ComposePageNavController {
    private val activityRef: WeakReference<ComponentActivity> = WeakReference(hostActivity)

    override fun navigateTo(route: ComposePageRoute) {
        val intent = ComposePageActivityIntent(route)
        activityRef.get()?.startActivity(intent)
    }

    override fun navigateBack() {
        activityRef.get()?.onBackPressedDispatcher?.onBackPressed()
    }
}
