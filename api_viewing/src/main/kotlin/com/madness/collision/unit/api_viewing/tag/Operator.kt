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

package com.madness.collision.unit.api_viewing.tag

fun interface Operator {
    fun operate(expression1: Expression, expression2: Expression): Boolean

    companion object {
        val AND = Operator { exp1: Expression, exp2: Expression ->
            if (exp1 is Statement || exp2 is Statement) return@Operator (exp1 * exp2).express()
            exp1.express() && exp2.express()
        }

        val OR = Operator { exp1: Expression, exp2: Expression ->
            if (exp1 is Statement || exp2 is Statement) return@Operator (exp1 + exp2).express()
            exp1.express() || exp2.express()
        }
    }
}
