package com.madness.collision.util

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * @param T Type of json
 */
inline fun <reified T> String.jsonSimpleTo(): T? {
    return Gson().fromJson(this, T::class.java)
}

fun Any.simpleToJson(): String {
    return Gson().toJson(this)
}
