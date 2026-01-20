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

package com.madness.collision.unit.api_viewing.tag.app

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlin.concurrent.thread
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Deprecated("")
class TagRequisiteRunner(private val context: Context) {
    private var serviceMessenger: Messenger? = null
    private val serviceConn = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            // IPC by a messenger
            serviceMessenger = service?.let(::Messenger)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            serviceMessenger = null
        }
    }
    private var replyLooper: Looper? = null
    private var replyThread: Thread? = null

    fun init(): TagRequisiteRunner {
        replyThread = thread(name = "TagReqRunner") {
            Looper.prepare()
            replyLooper = Looper.myLooper()
            Looper.loop()
        }
        return this
    }

    fun initAndBind(lifecycle: Lifecycle): TagRequisiteRunner {
        if (lifecycle.currentState < Lifecycle.State.INITIALIZED) return this
        init()
        try {
            val intent = Intent(context, TagRequisiteService::class.java)
            context.bindService(intent, serviceConn, Service.BIND_AUTO_CREATE)
            lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    owner.lifecycle.removeObserver(this)
                    context.unbindService(serviceConn)
                    replyLooper?.quitSafely()
                    replyThread?.interrupt()
                }
            })
        } catch (e: SecurityException) {
            e.printStackTrace()
            context.unbindService(serviceConn)
            replyLooper?.quitSafely()
            replyThread?.interrupt()
        }
        return this
    }

    /** Unbind the connection after use. */
    private suspend fun bindAndGetService(context: Context): Pair<ServiceConnection, Messenger?> =
        suspendCancellableCoroutine { cont ->
            val conn = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    serviceConn.onServiceConnected(name, service)
                    if (cont.isActive) cont.resume(this to service?.let(::Messenger))
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    serviceConn.onServiceDisconnected(name)
                }
            }
            var isInvoked = false
            try {
                val intent = Intent(context, TagRequisiteService::class.java)
                context.bindService(intent, conn, Service.BIND_AUTO_CREATE)
                isInvoked = true
            } catch (e: SecurityException) {
                context.unbindService(conn)
                cont.resumeWithException(e)
            }
            cont.invokeOnCancellation {
                if (isInvoked) context.unbindService(conn)
            }
        }

    private suspend inline fun <T> bindService(block: (Messenger) -> T): T? {
        val messenger = serviceMessenger
        if (messenger != null) {
            return block(messenger)
        } else {
            try {
                val (conn, service) = withTimeout(1500) { bindAndGetService(context) }
                return service?.let(block).also { context.unbindService(conn) }
            } catch (e: TimeoutCancellationException) {
                e.printStackTrace()
                return null
            } catch (e: Exception) {
                // bind was unsuccessful (e.g. SecurityException)
                e.printStackTrace()
                return null
            }
        }
    }

    /** Better wrap in a [withTimeout] call. */
    suspend fun findSuperclass(apks: List<String>, names: Set<String>): Set<String>? =
        bindService { messenger ->
            suspendCancellableCoroutine { cont ->
                val msg = Message.obtain(null, 1)
                msg.data = Bundle().apply {
                    putStringArrayList("apks", ArrayList(apks))
                    putStringArrayList("names", ArrayList(names))
                }
                val looper = replyLooper ?: Looper.getMainLooper()
                msg.replyTo = Messenger(Handler(looper) { reply ->
                    if (!cont.isActive) return@Handler true
                    when (reply.what) {
                        1 -> cont.resume(reply.data.getStringArrayList("value")?.toSet().orEmpty())
                        else -> cont.resumeWithException(IllegalStateException("what ${reply.what}?"))
                    }
                    true
                })
                try {
                    messenger.send(msg)
                } catch (e: RemoteException) {
                    cont.resumeWithException(e)
                }
            }
        }
}
