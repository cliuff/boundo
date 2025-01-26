/*
 * Copyright 2025 Clifford Liu
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

package com.madness.collision.unit.api_viewing.env

import androidx.annotation.IntDef
import com.madness.collision.unit.api_viewing.env.PartitionProp.Companion.FLAG_CONTAINS_NONE
import com.madness.collision.unit.api_viewing.env.PartitionProp.Companion.FLAG_CONTAINS_OVERLAY
import com.madness.collision.unit.api_viewing.env.PartitionProp.Companion.FLAG_CONTAINS_PRIV_APP

@JvmInline
value class PartitionProp(@Flags val value: Int) {
    val containsPrivApp: Boolean
        get() = value and FLAG_CONTAINS_PRIV_APP != 0
    val containsOverlay: Boolean
        get() = value and FLAG_CONTAINS_OVERLAY != 0

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(flag = true, value = [FLAG_CONTAINS_NONE, FLAG_CONTAINS_PRIV_APP, FLAG_CONTAINS_OVERLAY])
    annotation class Flags

    companion object {
        const val FLAG_CONTAINS_NONE: Int = 0
        const val FLAG_CONTAINS_PRIV_APP: Int = 1 shl 1
        const val FLAG_CONTAINS_OVERLAY: Int = 1 shl 2
    }
}

// refer android.content.pm.PackagePartitions
class SystemPartition
private constructor(
    val path: String,
    @PartitionProp.Flags prop: Int,
    private val order: Int
) : Comparable<SystemPartition> {

    val prop: PartitionProp = PartitionProp(prop)

    override fun compareTo(other: SystemPartition): Int {
        return order.compareTo(other.order)
    }

    companion object {
        val System = SystemPartition("/system", FLAG_CONTAINS_PRIV_APP or FLAG_CONTAINS_NONE, 0)
        val Vendor = SystemPartition("/vendor", FLAG_CONTAINS_PRIV_APP or FLAG_CONTAINS_OVERLAY, 1)
        val ODM = SystemPartition("/odm", FLAG_CONTAINS_PRIV_APP or FLAG_CONTAINS_OVERLAY, 2)
        val OEM = SystemPartition("/oem", FLAG_CONTAINS_NONE or FLAG_CONTAINS_OVERLAY, 3)
        val Product = SystemPartition("/product", FLAG_CONTAINS_PRIV_APP or FLAG_CONTAINS_OVERLAY, 4)
        val SystemExt = SystemPartition("/system_ext", FLAG_CONTAINS_PRIV_APP or FLAG_CONTAINS_OVERLAY, 5)
        // vivo OriginOS 4, Android 14
        val SystemCustom = SystemPartition("/system/custom", FLAG_CONTAINS_PRIV_APP or FLAG_CONTAINS_NONE, 6)
        val ProductH = SystemPartition("/product_h", FLAG_CONTAINS_PRIV_APP or FLAG_CONTAINS_OVERLAY, 7)
        val HwProduct = SystemPartition("/hw_product", FLAG_CONTAINS_PRIV_APP or FLAG_CONTAINS_OVERLAY, 8)

        val enum: Array<SystemPartition> = arrayOf(System, Vendor, ODM, OEM, Product, SystemExt, SystemCustom, ProductH, HwProduct)
    }
}
