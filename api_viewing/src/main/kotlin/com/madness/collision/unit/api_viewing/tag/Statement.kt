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

import android.content.Context
import com.madness.collision.unit.api_viewing.data.ApiViewingApp

internal class Statement(val expression1: Expression, val expression2: Expression,
                         val operator: Operator): Expression {
    var context: Context? = null
    var appPackage: ApiViewingApp? = null

    override fun express(): Boolean {
        if (expression1 is ExpressibleTag) {
            expression1.context = context
            expression1.appPackage = appPackage
        }
        if (expression2 is ExpressibleTag) {
            expression2.context = context
            expression2.appPackage = appPackage
        }
        return operator.operate(expression1, expression2)
    }

    fun setRes(context: Context, appPackage: ApiViewingApp): Statement {
        this.context = context
        this.appPackage = appPackage
        return this
    }
}
