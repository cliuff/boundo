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

package com.madness.collision.unit.device_manager.list

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.WorkerThread
import androidx.fragment.app.viewModels
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.madness.collision.R
import com.madness.collision.databinding.UnitDmDeviceListBinding
import com.madness.collision.unit.device_manager.manager.DeviceManager
import com.madness.collision.util.PermissionUtils
import com.madness.collision.util.TaggedFragment
import com.madness.collision.util.notifyBriefly
import com.madness.collision.util.os.OsUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class DeviceListFragment: TaggedFragment(), StateObservable {

    override val category: String = "DM"
    override val id: String = "DM-DeviceList"

    private val viewModel: DeviceListViewModel by viewModels()
    private val manager = DeviceManager()
    private val service = DeviceListService(manager)
    private lateinit var adapter: DeviceItemAdapter
    private lateinit var viewBinding: UnitDmDeviceListBinding
    private val updateRegulator: StateUpdateRegulator by lazy { StateUpdateRegulator() }

    override val stateReceiver: BroadcastReceiver = stateReceiverImp

    override fun onBluetoothStateChanged(state: Int) {
        val uiState = if (state == BluetoothAdapter.STATE_ON) DeviceListUiState.AccessAvailable
        else DeviceListUiState.BluetoothDisabled
        viewModel.setState(uiState)
    }

    override fun onDeviceStateChanged(device: BluetoothDevice, state: Int) {
        lifecycleScope.launch(Dispatchers.Default) {
            updateDeviceItem(device.address, state)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val context = context ?: return
        registerStateReceiver(context)
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                unregisterStateReceiver(context)
                manager.close()
            }
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewBinding = UnitDmDeviceListBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val context = context ?: return

        adapter = DeviceItemAdapter(context, object : DeviceItemAdapter.Listener {
            override val click: (DeviceItem) -> Unit = {
                val op = when (it.state) {
                    BluetoothProfile.STATE_CONNECTED,
                    BluetoothProfile.STATE_CONNECTING -> DeviceManager.OP_DISCONNECT
                    else -> DeviceManager.OP_CONNECT
                }
                val re = manager.operate(it.device, op)
                if (!re) notifyBriefly(R.string.text_error)
            }
        })
        adapter.resolveSpanCount(this, 290f)
        viewBinding.dmDeviceListRecycler.run {
            layoutManager = this@DeviceListFragment.adapter.suggestLayoutManager()
            adapter = this@DeviceListFragment.adapter
        }

        viewModel.data.observe(viewLifecycleOwner) {
            val (items, updateBlock) = it ?: return@observe
            adapter.setData(items)
            updateBlock?.invoke()
        }
        viewModel.uiState
            .flowWithLifecycle(viewLifecycleOwner.lifecycle)
            .onEach(::resolveUiState)
            .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private suspend fun resolveUiState(uiState: DeviceListUiState) {
        when (uiState) {
            DeviceListUiState.None -> {
                if (OsUtils.dissatisfy(OsUtils.S)) {
                    viewModel.setState(DeviceListUiState.PermissionGranted)
                    return
                }
                val context = context ?: return
                val permission = Manifest.permission.BLUETOOTH_CONNECT
                if (PermissionUtils.check(context, arrayOf(permission)).isNotEmpty()) {
                    bluetoothConnectLauncher.launch(permission)
                } else {
                    viewModel.setState(DeviceListUiState.PermissionGranted)
                }
            }
            DeviceListUiState.PermissionGranted -> {
                if (manager.isEnabled) {
                    viewModel.setState(DeviceListUiState.AccessAvailable)
                    return
                }
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                bluetoothEnableLauncher.launch(enableBtIntent)
            }
            DeviceListUiState.PermissionDenied -> {}
            DeviceListUiState.BluetoothDisabled -> {}
            DeviceListUiState.AccessAvailable -> {
                val context = context ?: return
                withContext(Dispatchers.Default) {
                    manager.initProxy(context)
                    loadDeviceItemsActual()
                }
            }
        }
    }

    @WorkerThread
    private suspend fun loadDeviceItemsActual() {
        val items = service.getDeviceItems()
        withContext(Dispatchers.Main) {
            viewModel.data.value = items to {
                adapter.notifyDataSetChanged()
            }
        }
    }

    private val bluetoothConnectLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()) register@{ granted ->
        val state = if (granted) DeviceListUiState.PermissionGranted
        else DeviceListUiState.PermissionDenied
        viewModel.setState(state)
    }

    // Observe state change in state receiver instead
    private val bluetoothEnableLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) { }

    @WorkerThread
    private suspend fun updateDeviceItem(mac: String, state: Int) {
        val (items, _) = viewModel.data.value ?: return
        for (index in items.indices) {
            val item = items[index]
            if (item.mac != mac) continue
            // a copy of the device item with new state must be stored for later state check
            val updateItem = item.copy(state = state)
            val updateItemState = updateItem.state
            val regulation = StateUpdateRegulation(600, updateItem) {
                item.state = updateItemState
                adapter.notifyItemChanged(index)
            }
            updateRegulator.regulate(regulation)
            break
        }
    }
}