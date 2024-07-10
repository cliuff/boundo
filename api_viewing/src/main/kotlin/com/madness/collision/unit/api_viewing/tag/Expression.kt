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

interface Expression {
    fun express(): Boolean

    // &&, and
    operator fun times(other: Expression): Expression {
        return CompExpression(this, other, Operator.AND)
    }

    // ||, or
    operator fun plus(other: Expression): Expression {
        return CompExpression(this, other, Operator.OR)
    }
}

fun interface Operator {
    fun operate(exp1: Expression, exp2: Expression): Boolean

    companion object {
        val AND = Operator { exp1, exp2 -> exp1.express() && exp2.express() }
        val OR = Operator { exp1, exp2 -> exp1.express() || exp2.express() }
    }
}

// CompositeExpression
class CompExpression(val exp1: Expression, val exp2: Expression, val op: Operator) : Expression {
    override fun express(): Boolean {
        return op.operate(exp1, exp2)
    }
}