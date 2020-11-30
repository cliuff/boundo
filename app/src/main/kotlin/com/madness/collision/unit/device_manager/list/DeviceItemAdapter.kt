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

import android.bluetooth.BluetoothProfile
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.madness.collision.R
import com.madness.collision.databinding.UnitDmDeviceItemBinding

internal class DeviceItemAdapter(
        context: Context, private val listener: Listener, private var data: List<DeviceItem> = emptyList()
) : RecyclerView.Adapter<DeviceItemAdapter.ViewHolder>() {

    class ViewHolder(binding: UnitDmDeviceItemBinding) : RecyclerView.ViewHolder(binding.root) {
        val container = binding.dmDeviceItemContainer
        val icon = binding.dmDeviceItemIcon
        val name = binding.dmDeviceItemName
        val hint = binding.dmDeviceItemHint
        val indicator = binding.dmDeviceItemIndicator
    }

    interface Listener {
        val click: (DeviceItem) -> Unit
    }

    private val layoutInflater = LayoutInflater.from(context)

    fun setData(data: List<DeviceItem>): DeviceItemAdapter {
        this.data = data
        return this
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(UnitDmDeviceItemBinding.inflate(layoutInflater, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = data[position]
        holder.icon.setImageResource(item.iconRes)
        holder.name.text = item.name
        val isConnected = item.state == BluetoothProfile.STATE_CONNECTED
        holder.indicator.visibility = if (isConnected) View.VISIBLE else View.GONE
        when (item.state) {
            BluetoothProfile.STATE_CONNECTED -> R.string.dm_list_item_hint_connected
            BluetoothProfile.STATE_CONNECTING -> R.string.dm_list_item_state_connecting
            BluetoothProfile.STATE_DISCONNECTING -> R.string.dm_list_item_state_disconnecting
            else -> R.string.dm_list_item_hint_disconnected
        }.let { holder.hint.setText(it) }
        holder.container.setOnClickListener {
            listener.click.invoke(item)
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }
}