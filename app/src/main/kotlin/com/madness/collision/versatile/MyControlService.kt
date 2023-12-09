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
import com.madness.collision.versatile.controls.*
import com.madness.collision.versatile.ctrl.ControlActionRequest
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.processors.FlowableProcessor
import io.reactivex.rxjava3.processors.ReplayProcessor
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import org.reactivestreams.FlowAdapters
import java.util.concurrent.Flow
import java.util.function.Consumer

@TargetApi(Build.VERSION_CODES.R)
class MyControlService : ControlsProviderService() {
    private val coroutineScope = CoroutineScope(Dispatchers.Default + Job())
    // many ids to one publisher
    private val publishers: MutableMap<String, Pair<FlowableProcessor<Control>, Job>> = hashMapOf()
    private var actionChannel: SendChannel<ControlActionRequest>? = null
    private lateinit var actionFlow: kotlinx.coroutines.flow.Flow<ControlActionRequest>
    private val providers = arrayOf(
        AtControlsProvider(),
        MduControlsProvider(),
        DevManControlsProvider(this),
    )

    override fun onCreate() {
        super.onCreate()
        val flow = channelFlow {
            actionChannel = channel
            awaitClose { actionChannel = null }
        }
        // SharedFlow/hot flow required for multiple subscribers to be allowed
        actionFlow = flow.shareIn(coroutineScope, SharingStarted.WhileSubscribed())
        providers.forEach { it.onCreate() }
    }

    override fun createPublisherForAllAvailable(): Flow.Publisher<Control> {
        val context = this
        // invoke toSerialized() to use ReplayProcessor in a Kotlin coroutine (multi-thread)
        val updatePublisher = ReplayProcessor.create<Control>().toSerialized()
        coroutineScope.launch {
            val ids = providers.flatMap { it.getDeviceIds() }
            for (id in ids) {
                if (!isActive) break
                try {
                    val control = getProvider(id)?.create(context, id) ?: continue
                    updatePublisher.onNext(control)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            updatePublisher.onComplete()
        }
        return FlowAdapters.toFlowPublisher(updatePublisher)
    }

    override fun createPublisherFor(controlIds: MutableList<String>): Flow.Publisher<Control> {
        // ReplayProcessor.create(capacity: Int) requires capacity > 0
        if (controlIds.size <= 0) return FlowAdapters.toFlowPublisher(Flowable.empty())
        // invoke toSerialized() to use ReplayProcessor in a Kotlin Flow (multi-thread),
        // fixes IndexOutOfBoundsException from (ReplayProcessor.java:772)
        val updatePublisher = ReplayProcessor.create<Control>(controlIds.size).toSerialized()
        for (id in controlIds) {
            publishers[id]?.let { (_, oldJob) ->
                // update publisher to be the new value
                publishers[id] = updatePublisher to oldJob
                // cancel the old statusJob
                oldJob.cancel()
            }
            val provider = getProvider(id) ?: continue
            val flow = actionFlow.filter { (i, _) -> i == id && publishers[i] != null }
            val statusJob = provider.create(this, id, flow)
                .onEach { control ->
                    publishers[control.controlId]?.let { (pub, _) -> pub.onNext(control) }
                }
                .launchIn(coroutineScope)
            publishers[id] = updatePublisher to statusJob
        }
        return FlowAdapters.toFlowPublisher(updatePublisher)
    }

    override fun performControlAction(controlId: String, action: ControlAction, consumer: Consumer<Int>) {
        if (publishers[controlId] != null) {
            coroutineScope.launch { actionChannel?.send(controlId to action) }
            consumer.accept(ControlAction.RESPONSE_OK)
        } else {
            consumer.accept(ControlAction.RESPONSE_FAIL)
        }
    }

    override fun onDestroy() {
        actionChannel?.close()
        coroutineScope.coroutineContext.cancelChildren()
        super.onDestroy()
        providers.forEach { it.onDestroy() }
    }

    private fun getProvider(controlId: String): ControlsProvider? {
        return providers.find { controlId.matches(it.controlIdRegex.toRegex()) }
    }
}
