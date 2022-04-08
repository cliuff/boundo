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

package com.madness.collision.versatile

import android.annotation.TargetApi
import android.os.Build
import android.service.controls.Control
import android.service.controls.ControlsProviderService
import android.service.controls.actions.ControlAction
import com.madness.collision.versatile.controls.AudioTimerControlProvider
import com.madness.collision.versatile.controls.ControlProvider
import com.madness.collision.versatile.controls.DeviceManControlProvider
import com.madness.collision.versatile.controls.MonthDataUsageControlProvider
import io.reactivex.rxjava3.processors.ReplayProcessor
import kotlinx.coroutines.*
import org.reactivestreams.FlowAdapters
import java.util.concurrent.Flow
import java.util.function.Consumer

@TargetApi(Build.VERSION_CODES.R)
class MyControlService : ControlsProviderService() {
    private val coroutineScope = CoroutineScope(Dispatchers.Default + Job())
    private var updatePublisher: ReplayProcessor<Control>? = null
    private val providers = arrayOf(
        AudioTimerControlProvider(this),
        MonthDataUsageControlProvider(this),
        DeviceManControlProvider(this),
    )

    override fun onCreate() {
        super.onCreate()
        providers.forEach { it.onCreate() }
    }

    override fun createPublisherForAllAvailable(): Flow.Publisher<Control> {
        val updatePublisher = ReplayProcessor.create<Control>()
        coroutineScope.launch {
            val ids = providers.flatMap { it.getDeviceIds() }
            ids.forEach {
                val control = getStatelessControl(it) ?: return@forEach  // continue
                updatePublisher.onNext(control)
            }
            updatePublisher.onComplete()
        }
        return FlowAdapters.toFlowPublisher(updatePublisher)
    }

    override fun createPublisherFor(controlIds: MutableList<String>): Flow.Publisher<Control> {
        val updatePublisher = ReplayProcessor.create<Control>().also {
            updatePublisher = it
        }
        controlIds.forEach { getControl(updatePublisher, it) }
        return FlowAdapters.toFlowPublisher(updatePublisher)
    }

    override fun performControlAction(controlId: String, action: ControlAction, consumer: Consumer<Int>) {
        // actions can only be performed on stateful controls
        val updatePublisher = updatePublisher ?: return
        // Inform SystemUI that the action has been received and is being processed
        consumer.accept(ControlAction.RESPONSE_OK)
        getControl(updatePublisher, controlId, action)
    }

    override fun onDestroy() {
        coroutineScope.cancel()
        super.onDestroy()
        providers.forEach { it.onDestroy() }
    }

    private fun getProvider(controlId: String): ControlProvider? {
        return providers.find { controlId.matches(it.controlIdRegex.toRegex()) }
    }

    private suspend fun getStatelessControl(controlId: String): Control? {
        return getProvider(controlId)?.getStatelessControl(controlId)
    }

    private fun getControl(updatePublisher: ReplayProcessor<Control>, controlId: String, action: ControlAction? = null) {
        getProvider(controlId)?.getStatefulControl(updatePublisher, controlId, action)
    }

}
