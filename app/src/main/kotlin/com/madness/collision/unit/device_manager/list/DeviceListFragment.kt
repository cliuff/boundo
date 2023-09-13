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
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.annotation.WorkerThread
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.madness.collision.BuildConfig
import com.madness.collision.R
import com.madness.collision.chief.auth.PermissionHandler
import com.madness.collision.chief.auth.PermissionState
import com.madness.collision.databinding.UnitDmDeviceListBinding
import com.madness.collision.unit.device_manager.manager.DeviceManager
import com.madness.collision.util.ElapsingTime
import com.madness.collision.util.TaggedFragment
import com.madness.collision.util.notifyBriefly
import com.madness.collision.util.os.OsUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

interface DeviceListController {
    fun requestBluetoothOn()
    fun requestBluetoothConn()
    fun requestSettingsChange()
}

internal class DeviceListFragment: TaggedFragment(), StateObservable, InterceptedActivityResult {
    interface Listener {
        fun onUiState(state: DeviceListUiState)
    }

    override val category: String = "DM"
    override val id: String = "DM-DeviceList"

    private val viewModel: DeviceListViewModel by viewModels()
    private val manager = DeviceManager()
    private val service = DeviceListService(manager)
    private lateinit var adapter: DeviceItemAdapter
    private lateinit var viewBinding: UnitDmDeviceListBinding
    private val updateRegulator: StateUpdateRegulator by lazy { StateUpdateRegulator() }

    override val registryFragment: Fragment get() = this
    override val stateReceiver: BroadcastReceiver = stateReceiverImp

    override fun onBluetoothStateChanged(state: Int) {
        // use None state to recheck and get current state,
        // to ensure bluetooth state is checked after bluetooth connect permission
        viewModel.setState(DeviceListUiState.None)
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
            .filterNot { it == DeviceListUiState.None }
            .onEach { viewModel.someUiState = it }
            .launchIn(viewLifecycleOwner.lifecycleScope)
        elapsingResume.reset()
    }

    private val elapsingResume = ElapsingTime()

    override fun onResume() {
        super.onResume()
        if (elapsingResume.interval(800)) {
            // recheck current state
            viewModel.setState(DeviceListUiState.None)
        }
    }

    override fun onInterceptResult() {
        // reset to block subsequent state check in onResume()
        elapsingResume.reset()
    }

    private suspend fun resolveUiState(uiState: DeviceListUiState) {
        val host = parentFragment ?: activity ?: host
        if (host is Listener && uiState != DeviceListUiState.None) host.onUiState(uiState)
        when (uiState) {
            DeviceListUiState.None -> when {
                OsUtils.satisfy(OsUtils.S) -> bluetoothConnHandler.checkState()
                else -> viewModel.setState(DeviceListUiState.PermissionGranted)
            }
            DeviceListUiState.PermissionGranted -> when {
                manager.isEnabled -> viewModel.setState(DeviceListUiState.AccessAvailable)
                else -> viewModel.setState(DeviceListUiState.BluetoothDisabled)
            }
            DeviceListUiState.PermissionDenied -> Unit
            DeviceListUiState.PermissionPermanentlyDenied -> Unit
            DeviceListUiState.BluetoothDisabled -> Unit
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

    private val _btConnPerm = if (OsUtils.satisfy(OsUtils.S)) Manifest.permission.BLUETOOTH_CONNECT else ""
    private val bluetoothConnHandler = PermissionHandler(this, _btConnPerm) { handler, state ->
        // onActivityResult(): reset to block subsequent state check in onResume()
        elapsingResume.reset()
        viewModel.setState(when (state) {
            PermissionState.Granted -> DeviceListUiState.PermissionGranted
            is PermissionState.PermanentlyDenied -> DeviceListUiState.PermissionPermanentlyDenied
            else -> {
                // retain PermissionPermanentlyDenied state
                val retain = DeviceListUiState.PermissionPermanentlyDenied
                viewModel.someUiState.takeIf { it == retain } ?: DeviceListUiState.PermissionDenied
            }
        })
        when (state) {
            PermissionState.Granted -> Unit
            is PermissionState.Denied -> Unit//if (state.requestCount <= 0) handler.request()
            is PermissionState.ShowRationale -> Unit//if (state.requestCount <= 0) handler.request()
            is PermissionState.PermanentlyDenied -> Unit
        }
    }

    // Observe state change in state receiver instead
    private val bluetoothEnableLauncher = registerForActivityResultV(
        ActivityResultContracts.StartActivityForResult(),
        Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)) { }

    private val controller = object : DeviceListController {
        override fun requestBluetoothOn() = bluetoothEnableLauncher.launch()
        override fun requestBluetoothConn() = bluetoothConnHandler.request()
        override fun requestSettingsChange() {
            val uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
            // as stated in the doc, avoid using Intent.FLAG_ACTIVITY_NEW_TASK with startActivityForResult()
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri)
            // use resultLauncher to trigger onActivityResult()
            appSettingsLauncher.launch(intent)
        }
        private val appSettingsLauncher = registerForActivityResultV(
            ActivityResultContracts.StartActivityForResult()) {
            viewModel.setState(DeviceListUiState.None)
        }
    }

    fun getController() = controller

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