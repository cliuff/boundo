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

package com.madness.collision.instant.other

import com.madness.collision.R
import com.madness.collision.instant.instantComponent
import com.madness.collision.unit.Unit
import com.madness.collision.util.X
import com.madness.collision.versatile.TextProcessingActivity

internal object InstantOthers {

    val OTHERS = listOf(
            instantComponent<TextProcessingActivity>(R.string.activityTextProcessingApp, Unit.UNIT_NAME_API_VIEWING)
                    .setRequirement { X.aboveOn(X.M) }
    )

}
