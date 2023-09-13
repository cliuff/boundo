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

package com.madness.collision.unit.device_manager

import android.bluetooth.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.madness.collision.R
import com.madness.collision.databinding.UnitDeviceManagerBinding
import com.madness.collision.unit.Unit
import com.madness.collision.unit.device_manager.list.DeviceListFragment
import com.madness.collision.unit.device_manager.list.DeviceListUiState
import com.madness.collision.util.alterPadding
import com.madness.collision.util.controller.getSavedFragment
import com.madness.collision.util.controller.saveFragment
import com.madness.collision.util.ensureAdded

class MyUnit : Unit(), DeviceListFragment.Listener {

    override val id: String = "DM"

    companion object {
        const val STATE_KEY_LIST = "ListFragment"
    }

    private lateinit var viewBinding: UnitDeviceManagerBinding
    private lateinit var listFragment: DeviceListFragment

    // observe bluetooth on/off events
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action ?: return
            if (action == BluetoothAdapter.ACTION_STATE_CHANGED) return
            val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF)
            val isOn = state == BluetoothAdapter.STATE_ON
        }
    }

    override fun createOptions(context: Context, toolbar: Toolbar, iconColor: Int): Boolean {
        configNavigation(toolbar, iconColor)
        toolbar.setTitle(R.string.unit_device_manager)
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        listFragment = childFragmentManager.getSavedFragment(savedInstanceState, STATE_KEY_LIST)
                ?: DeviceListFragment()
        val context = context ?: return
        context.registerReceiver(receiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewBinding = UnitDeviceManagerBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        democratize()
        ensureAdded(R.id.dmListContainer, listFragment, true)
        mainViewModel.contentWidthTop.observe(viewLifecycleOwner) {
            viewBinding.dmContainer.alterPadding(top = it)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        childFragmentManager.saveFragment(outState, STATE_KEY_LIST, listFragment)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()
        val context = context ?: return
        context.unregisterReceiver(receiver)
    }

    override fun onUiState(state: DeviceListUiState) {
        val controller = listFragment.getController()
        when (state) {
            DeviceListUiState.AccessAvailable -> setListTitle()
            DeviceListUiState.BluetoothDisabled -> {
                setMessageAndAction("Bluetooth OFF", "Turn on")
                viewBinding.dmAction.setOnClickListener { controller.requestBluetoothOn() }
            }
            DeviceListUiState.PermissionDenied -> {
                setMessageAndAction("BLUETOOTH_CONNECT permission denied", "Allow")
                viewBinding.dmAction.setOnClickListener { controller.requestBluetoothConn() }
            }
            DeviceListUiState.PermissionPermanentlyDenied -> {
                setMessageAndAction("BLUETOOTH_CONNECT permission should be granted from app settings", "Change")
                viewBinding.dmAction.setOnClickListener { controller.requestSettingsChange() }
            }
            DeviceListUiState.PermissionGranted -> setListTitle()
            DeviceListUiState.None -> setListTitle()
        }
    }

    private fun setListTitle() = viewBinding.run {
        dmTitle.isVisible = true
        dmMessageContainer.isInvisible = true
    }

    private fun setMessageAndAction(text: String, action: String) = viewBinding.run {
        dmMessage.text = text
        dmAction.text = action
        dmTitle.isInvisible = true
        dmMessage.isVisible = true
        dmAction.isVisible = true
        dmMessageContainer.isVisible = true
    }
}
