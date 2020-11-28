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

package com.madness.collision.unit.device_manager.list

import android.app.Activity
import android.bluetooth.*
import android.content.BroadcastReceiver
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.madness.collision.R
import com.madness.collision.settings.SettingsFunc
import com.madness.collision.unit.device_manager.list.item.DeviceItemAdapter
import com.madness.collision.unit.device_manager.manager.DeviceManager
import com.madness.collision.util.TaggedFragment
import com.madness.collision.util.X
import com.madness.collision.util.availableWidth
import kotlinx.android.synthetic.main.unit_dm_device_list.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

internal class DeviceListFragment: TaggedFragment(), StateObservable {

    override val category: String = "DM"
    override val id: String = "DM-DeviceList"

    companion object {
        private const val REQUEST_ENABLE_BT = 1
    }

    private val viewModel: DeviceListViewModel by viewModels()
    private val manager = DeviceManager()
    private val service = DeviceListService(manager)
    private lateinit var adapter: DeviceItemAdapter
    override val stateReceiver: BroadcastReceiver = stateReceiverImp

    override fun onBluetoothStateChanged(state: Int) {
        loadDeviceItems()
    }

    override fun onDeviceStateChanged(device: BluetoothDevice, state: Int) {
        updateDeviceItem(device.address, state)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val context = context ?: return
        registerStateReceiver(context)
        adapter = DeviceItemAdapter(context, manager)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val context = context
        if (context != null) SettingsFunc.updateLanguage(context)
        return inflater.inflate(R.layout.unit_dm_device_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val context = context ?: return
        viewModel.data = MutableLiveData()
        if (manager.isDisabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }
        lifecycleScope.launch {
            if (manager.isEnabled) manager.initProxy(context)
            loadDeviceItems()
        }

        val itemWidth = X.size(context, 400f, X.DP)
        val spanCount = (availableWidth / itemWidth).roundToInt().run {
            if (this < 2) 1 else this
        }
        dmDeviceListRecycler.run {
            layoutManager = if (spanCount == 1) LinearLayoutManager(context)
            else GridLayoutManager(context, spanCount)
            adapter = this@DeviceListFragment.adapter
        }

        viewModel.data.observe(viewLifecycleOwner) {
            val (items, updateBlock) = it ?: return@observe
            adapter.setData(items)
            updateBlock?.invoke()
        }
    }

    /**
     * Get device items from service and load them
     */
    private fun loadDeviceItems() {
        lifecycleScope.launch {
            viewModel.data.value = service.getDeviceItems() to {
                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun updateDeviceItem(mac: String, state: Int) {
        val (items, _) = viewModel.data.value ?: return
        for (index in items.indices) {
            val item = items[index]
            if (item.mac != mac) continue
            item.state = state
            adapter.notifyItemChanged(index)
            break
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        manager.close()
        val context = context ?: return
        unregisterStateReceiver(context)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode != Activity.RESULT_OK) return
            val context = context ?: return
            if (manager.isDisabled) return
            lifecycleScope.launch {
                manager.initProxy(context)
                loadDeviceItems()
            }
        }
    }
}