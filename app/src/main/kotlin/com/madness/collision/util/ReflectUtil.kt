package com.madness.collision.util

import java.lang.reflect.Field

// clazz.kotlin.objectInstance not working with proguard
@Suppress("UNCHECKED_CAST")
val <T> Class<T>.objectInstance: T?
    get() {
        val field: Field = getDeclaredField("INSTANCE")
        field.isAccessible = true
        return field.get(null) as T?
    }
