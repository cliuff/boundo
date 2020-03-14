package com.madness.collision.util

import android.content.SharedPreferences
import androidx.core.content.edit

object PrefsUtil {
    fun getCompoundItem(pref: SharedPreferences, key: String) : Map<String, String> {
        val data = pref.getStringSet(key, HashSet())!!
        return data.associate { it.split(":").run { this[0] to this[1] } }
    }

    fun putCompoundItem(pref: SharedPreferences, key: String, data: Map<*, *>) {
        val re = HashSet<String>(data.size)
        data.forEach { re.add("${it.key}:${it.value}") }
        pref.edit { putStringSet(key, re) }
    }
}
