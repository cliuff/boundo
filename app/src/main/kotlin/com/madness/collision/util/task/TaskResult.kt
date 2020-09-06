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

package com.madness.collision.util.task

class TaskResult<D, R>(override val isSuccess: Boolean, data: D? = null,
                       reason: R? = null): Resultful<D, R> {
    private val mData = data
    private val mReason = reason

    override val data: D?
        get() = mData
    override val failingReason: R?
        get() = mReason

    companion object {
        fun <D> success(data: D? = null) = TaskResult<D, Any>(true, data = data)

        fun <R> failure(reason: R? = null) = TaskResult<Any, R>(false, reason = reason)
    }
}
