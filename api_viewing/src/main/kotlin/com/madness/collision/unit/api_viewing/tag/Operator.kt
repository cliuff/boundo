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

package com.madness.collision.unit.api_viewing.tag

interface Operator {
    fun operate(expression1: Expression, expression2: Expression): Boolean

    companion object {
        val AND = object : Operator {
            override fun operate(expression1: Expression, expression2: Expression): Boolean {
                if (expression1 is Statement || expression2 is Statement) {
                    return (expression1 * expression2).express()
                }
                return expression1.express() && expression2.express()
            }
        }

        val OR = object : Operator {
            override fun operate(expression1: Expression, expression2: Expression): Boolean {
                if (expression1 is Statement || expression2 is Statement) {
                    return (expression1 + expression2).express()
                }
                return expression1.express() || expression2.express()
            }
        }
    }
}
