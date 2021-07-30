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

package com.madness.collision.unit.image_modifying

import com.madness.collision.unit.Bridge
import com.madness.collision.unit.Unit

object MyBridge: Bridge() {

    override val unitName: String = Unit.UNIT_NAME_IMAGE_MODIFYING

    /**
     * @param args empty
     */
    override fun getUnitInstance(vararg args: Any?): Unit {
        return MyUnit()
    }
}
