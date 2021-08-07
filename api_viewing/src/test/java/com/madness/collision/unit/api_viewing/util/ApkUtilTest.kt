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

package com.madness.collision.unit.api_viewing.util

import com.google.common.truth.Truth.*
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.system.measureTimeMillis

class ApkUtilTest {

    /**
     * original: 122 packages (434ms), 440ms, 461ms
     * modified: 122 packages (500ms), 448ms, 439ms
     */
    @Test
    fun getThirdPartyPkg() {
        val path = javaClass.classLoader?.getResource("apk/boundo-3.7.5-arm.apk")?.path
        if (path == null) assert_().fail()
        val pkgList: List<CharSequence>
        val t = measureTimeMillis {
            pkgList = ApkUtil.getThirdPartyPkg(path!!, "com.madness.collision")
        }
        println("\n\nAPK path: $path \n${pkgList.size} packages (${t}ms):")
        pkgList.forEach { println(it) }
        println()
    }

    /**
     * original: 219ms, 265ms, 180ms
     * modified: 132ms, 119ms, 152ms
     */
    @Test
    fun checkPkg() {
        val path = javaClass.classLoader?.getResource("apk/boundo-3.7.5-arm.apk")?.path
        if (path == null) assert_().fail()
        val pkg = "com.google.android.material"
        val re: Boolean
        val t = measureTimeMillis { re = ApkUtil.checkPkg(path!!, pkg) }
        assertThat(re).isTrue()
        println("\n\nAPK path: $path \nChecking \"$pkg\" finished in ${t}ms\n")
    }
}