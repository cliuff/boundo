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

package com.madness.collision.util

import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.content.pm.ApplicationInfoBuilder
import androidx.test.core.content.pm.PackageInfoBuilder
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
internal class ApiUtilsTest {
    @Mock
    private lateinit var context: Context
    @Mock
    private lateinit var packageManager: PackageManager

    @Test
    fun gglPlayAvailable() {
        val packageGglPlay = "com.android.vending"
        val gglPlayApplicationInfo = ApplicationInfoBuilder.newBuilder()
                .setPackageName(packageGglPlay).build()
        val gglPlayPackageInfo = PackageInfoBuilder.newBuilder()
                .setPackageName(packageGglPlay)
                .setApplicationInfo(gglPlayApplicationInfo)
                .build()
        `when`(packageManager.getPackageInfo(packageGglPlay, 0)).thenReturn(gglPlayPackageInfo)
        `when`(context.packageManager).thenReturn(packageManager)
        assertThat(ApiUtils.isGglPlayAvailable(context)).isTrue()
    }

    @Test
    fun gglPlayUnavailable() {
        val packageGglPlay = "com.android.vending"
        `when`(packageManager.getPackageInfo(packageGglPlay, 0))
                .thenThrow(PackageManager.NameNotFoundException())
        `when`(context.packageManager).thenReturn(packageManager)
        assertThat(ApiUtils.isGglPlayAvailable(context)).isFalse()
    }
}