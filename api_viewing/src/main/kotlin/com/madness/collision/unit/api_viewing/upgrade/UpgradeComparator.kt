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

package com.madness.collision.unit.api_viewing.upgrade

import com.madness.collision.util.os.OsUtils
import java.util.stream.Collectors

object UpgradeComparator {
    fun compareTime(list: List<Upgrade>): List<Upgrade> {
        return if (OsUtils.satisfy(OsUtils.N)) {
            val comparator: Comparator<Upgrade> = Comparator.comparingLong { u: Upgrade ->
                u.updateTime.second
            }.reversed()
            list.parallelStream().sorted(comparator).collect(Collectors.toList())
        } else {
            list.toMutableList().apply {
                sortWith { o1, o2 -> o2.updateTime.second.compareTo(o1.updateTime.second) }
            }
        }
    }
}