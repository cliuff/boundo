/*
 * Copyright 2020 Clifford Liu
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

package com.madness.collision.unit.device_manager.manager

import android.bluetooth.*
import android.content.Context
import android.util.SparseArray
import androidx.core.util.set
import androidx.core.util.size
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.lang.Exception
import java.lang.reflect.Method

/**
 * No need to invoke [close] if [useProxy] is false
 */
@Suppress("DiscouragedPrivateApi")
class DeviceManager(useProxy: Boolean = true): AutoCloseable {
    companion object {
        const val OP_CONNECT: String = "connect"
        const val OP_DISCONNECT: String = "disconnect"
    }
    private val doUseProxy: Boolean = useProxy
    val adapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val profiles = if (useProxy) arrayOf(BluetoothProfile.HEADSET, BluetoothProfile.A2DP)
    else emptyArray()
    private val operations = if (useProxy) arrayOf(OP_CONNECT, OP_DISCONNECT) else emptyArray()
    private val proxies: SparseArray<BluetoothProfile> = if (useProxy) SparseArray(2)
    else SparseArray(0)
    private val methods: SparseArray<Map<String, Method?>> = if (useProxy) SparseArray(2)
    else SparseArray(0)
    val isEnabled: Boolean
        get() = adapter?.isEnabled == true
    val isDisabled: Boolean
        get() = !isEnabled
    val hasProxy: Boolean
        get() = proxies.size == profiles.size

    fun operate(device: BluetoothDevice, operation: String): Boolean {
        var isSuccess: Boolean? = null
        for (profile in profiles) {
            val proxy = proxies[profile]
            // check whether to skip the operation
            when(operation) {
                OP_CONNECT -> if (device in proxy.connectedDevices) continue
                OP_DISCONNECT -> if (device !in proxy.connectedDevices) continue
            }
            val re = methods[profile][operation]?.invoke(proxy, device) == true
            isSuccess = if (isSuccess == null) re else (re && isSuccess == true)
        }
        return isSuccess ?: false
    }

    private fun BluetoothDevice.doOp(operation: String): Boolean {
        return operate(this, operation)
    }

    fun connect(device: BluetoothDevice): Boolean {
        return device.doOp(OP_CONNECT)
    }

    fun disconnect(device: BluetoothDevice): Boolean {
        return device.doOp(OP_DISCONNECT)
    }

    fun getConnectedDevices(): List<BluetoothDevice> {
        return profiles.map { proxies[it]?.connectedDevices ?: emptyList() }.flatten()
    }

    fun initProxy(context: Context, timeout: Long = 400) {
        if (!doUseProxy || hasProxy) return
        val proxyClasses = arrayOf(BluetoothHeadset::class.java, BluetoothA2dp::class.java)
        val paramClass = BluetoothDevice::class.java
        profiles.forEachIndexed { index, profile ->
            val profileMethods = HashMap<String, Method?>()
            operations.forEach { op ->
                profileMethods[op] = try {
                    proxyClasses[index].getDeclaredMethod(op, paramClass)
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
            methods[profile] = profileMethods
        }

        val profileListener = object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                proxies[profile] = proxy
            }

            override fun onServiceDisconnected(profile: Int) {
                proxies.remove(profile)
            }
        }
        for (profile in profiles) {
            if (proxies[profile] != null) continue
            // Establish connection to the proxy.
            adapter?.getProfileProxy(context, profileListener, profile)
        }
        // skip waiting
        if (timeout < 100) return
        // wait for proxies to be ready
        runBlocking {
            val startTime = System.currentTimeMillis()
            while (true) {
                delay(100)
                if (hasProxy) break
                if (System.currentTimeMillis() - startTime > timeout) break
            }
        }
    }

    override fun close() {
        profiles.forEach {
            // Close proxy connection after use.
            adapter?.closeProfileProxy(it, proxies[it])
            proxies.remove(it)
        }
    }

    fun getPairedDevices(): List<BluetoothDevice> {
        return adapter?.bondedDevices?.toList() ?: emptyList()
    }

}