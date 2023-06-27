/*
 * Copyright 2023 Clifford Liu
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

package com.madness.collision.util.collection

/**
 * Group elements by [predicate] consecutively.
 */
inline fun <reified T> Iterable<T>.groupConsecBy(predicate: (T, T) -> Boolean): List<List<T>> {
    return groupConsecByTo(arrayListOf(), predicate)
}

/**
 * Group elements by [predicate] consecutively.
 *
 * Before: [ "A", "A",   "B",   "C", "C",   "A"]
 *
 * After:  [["A", "A"], ["B"], ["C", "C"], ["A"]]
 */
inline fun <reified T, M: MutableList<MutableList<T>>> Iterable<T>.groupConsecByTo(destination: M, predicate: (T, T) -> Boolean): M {
    var lastRef: Array<T>? = null
    for (element in this) {
        if (lastRef == null) {
            lastRef = arrayOf(element)
            destination.add(arrayListOf(element))
        } else {
            if (predicate(lastRef[0], element)) {
                destination.last().add(element)
            } else {
                destination.add(arrayListOf(element))
            }
            lastRef[0] = element
        }
    }
    return destination
}
