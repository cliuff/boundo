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

package com.madness.collision.unit.api_viewing

import com.google.common.truth.Truth
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class UtilsTest {

    @Test
    fun checkStoreLink() {
        val links = listOf(
                "https://play.google.com/store/apps/details?id=com.madness.collision",
                "https://coolapk.com/apk/com.tencent.mobileqq",
                "http://apps.galaxyappstore.com/detail/com.happyelements.AndroidAnimal?session_id=W_dfb0db3059259b399e1d869f4200bd93",
                "demommmmm hhh",
        )
        val re = links.map { Utils.checkStoreLink(it) }
        Truth.assertThat(re[0]).isEqualTo("com.madness.collision")
        Truth.assertThat(re[1]).isEqualTo("com.tencent.mobileqq")
        Truth.assertThat(re[2]).isEqualTo("com.happyelements.AndroidAnimal")
        Truth.assertThat(re[3]).isNull()
    }
}