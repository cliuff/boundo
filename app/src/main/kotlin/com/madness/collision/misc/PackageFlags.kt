/*
 * Copyright 2023 Clifford Liu
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

package com.madness.collision.misc

import android.content.pm.PackageManager.ApplicationInfoFlags
import android.content.pm.PackageManager.ComponentInfoFlags
import android.content.pm.PackageManager.PackageInfoFlags
import android.content.pm.PackageManager.ResolveInfoFlags
import android.os.Build
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
object PackageFlags {
    fun Package(value: Int) = PackageInfoFlags.of(value.toLong())
    fun Application(value: Int) = ApplicationInfoFlags.of(value.toLong())
    fun Component(value: Int) = ComponentInfoFlags.of(value.toLong())
    fun Resolve(value: Int) = ResolveInfoFlags.of(value.toLong())
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
inline fun <reified T> PackageFlags(value: Int): T {
    val v = value.toLong()
    return when (T::class) {
        PackageInfoFlags::class -> PackageInfoFlags.of(v) as T
        ApplicationInfoFlags::class -> ApplicationInfoFlags.of(v) as T
        ComponentInfoFlags::class -> ComponentInfoFlags.of(v) as T
        ResolveInfoFlags::class -> ResolveInfoFlags.of(v) as T
        else -> throw IllegalArgumentException("Unsupported PackageManager flags")
    }
}
