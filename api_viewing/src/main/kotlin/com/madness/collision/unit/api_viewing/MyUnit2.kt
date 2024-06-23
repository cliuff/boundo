/*
 * Copyright 2024 Clifford Liu
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

package com.madness.collision.unit.api_viewing

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import com.madness.collision.chief.app.ComposeViewOwner
import com.madness.collision.unit.api_viewing.ui.list.AppListFragment

class MyUnit : AppListFragment() {
    companion object {
        const val SORT_POSITION_API_LOW: Int = 0
        const val SORT_POSITION_API_HIGH: Int = 1
        const val SORT_POSITION_API_NAME: Int = 2
        const val SORT_POSITION_API_TIME: Int = 3
    }
}

abstract class ComposeUnit : com.madness.collision.unit.Unit() {
    protected val composeViewOwner = ComposeViewOwner()

    @Composable
    protected abstract fun ComposeContent()

    @Composable
    protected fun rememberContentPadding() =
        com.madness.collision.chief.app.rememberContentPadding(mainViewModel)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return composeViewOwner.createView(inflater.context, viewLifecycleOwner).apply {
            setContent { ComposeContent() }
        }
    }
}
