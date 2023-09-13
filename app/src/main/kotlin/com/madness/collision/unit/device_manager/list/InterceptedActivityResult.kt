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

package com.madness.collision.unit.device_manager.list

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.registerForActivityResult
import androidx.fragment.app.Fragment

interface InterceptedActivityResult {
    val registryFragment: Fragment

    fun onInterceptResult()

    private fun <I, O> interceptedContract(contract: ActivityResultContract<I, O>) =
        object : ActivityResultContract<I, O>() {
            override fun createIntent(context: Context, input: I): Intent {
                return contract.createIntent(context, input)
            }

            override fun parseResult(resultCode: Int, intent: Intent?): O {
                onInterceptResult()
                return contract.parseResult(resultCode, intent)
            }
        }

    fun <I, O> registerForActivityResultV(
        contract: ActivityResultContract<I, O>,
        callback: ActivityResultCallback<O>
    ) = registryFragment.registerForActivityResult(interceptedContract(contract), callback)

    fun <I, O> registerForActivityResultV(
        contract: ActivityResultContract<I, O>,
        input: I, callback: (O) -> Unit
    ) = registryFragment.registerForActivityResult(interceptedContract(contract), input, callback)
}
