package com.madness.collision.util

import android.content.SharedPreferences
import androidx.core.content.edit

object PrefsUtil {

    // number values may default to Double when without reified
    inline fun <reified K, reified V> getCompoundItem(pref: SharedPreferences, key: String) : Map<K, V> {
        val json = pref.getString(key, "")
        if (json.isNullOrEmpty()) return emptyMap()
        return json.jsonNestedTo() ?: emptyMap()
    }

    // number values may default to Double when without reified
    inline fun <reified K, reified V> putCompoundItem(pref: SharedPreferences, key: String, data: Map<K, V>) {
        pref.edit { putString(key, data.nestedToJson<Map<K, V>>()) }
    }
}
