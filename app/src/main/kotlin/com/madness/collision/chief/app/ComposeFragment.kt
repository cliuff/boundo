/*
 * Copyright 2023 Clifford Liu
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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.fragment.app.activityViewModels
import com.madness.collision.main.MainViewModel
import com.madness.collision.util.TaggedFragment

abstract class ComposeFragment : TaggedFragment() {
    protected val mainViewModel: MainViewModel by activityViewModels()
    private val composeViewOwner = ComposeViewOwner()

    @Composable
    protected abstract fun ComposeContent()

    @Composable
    protected fun rememberContentPadding() = rememberContentPadding(mainViewModel)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return composeViewOwner.createView(inflater.context, viewLifecycleOwner).apply {
            setContent { ComposeContent() }
        }
    }
}
