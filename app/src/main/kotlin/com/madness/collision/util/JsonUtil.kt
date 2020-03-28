package com.madness.collision.util

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * @param T Generic type that has nested generic type like Map<String, String>
 */
// number values may default to Double when without reified
inline fun <reified T> String.jsonNestedTo(): T? {
    return Gson().fromJson(this, object : TypeToken<T>() {}.type)
}

/**
 * @param T Type of json
 */
inline fun <reified T> String.jsonSimpleTo(): T? {
    return Gson().fromJson(this, T::class.java)
}

/**
 * @param T Generic type that has nested generic type like Map<String, String>
 */
// number values may default to Double when without reified
inline fun <reified T> Any.nestedToJson(): String {
    return Gson().toJson(this, object : TypeToken<T>() {}.type)
}

fun Any.simpleToJson(): String {
    return Gson().toJson(this)
}
