package com.madness.collision.util

import android.content.Context

interface NameCached {

    var name: String
    val nameResId: Int

    fun getName(context: Context): String {
        return if (name.isEmpty()) context.getString(nameResId) else name
    }
}
