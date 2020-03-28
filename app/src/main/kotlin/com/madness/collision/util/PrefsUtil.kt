package com.madness.collision.util

import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object PrefsUtil {

    // no number values
    inline fun <reified K, reified V> getCompoundItem(pref: SharedPreferences, key: String) : Map<K, V> {
        val json = pref.getString(key, "")
        if (json.isNullOrEmpty()) return emptyMap()
        return Gson().fromJson(json, TypeToken.getParameterized(Map::class.java, K::class.java, V::class.java).type) ?: emptyMap()
    }

    // no number values
    inline fun <reified K, reified V> putCompoundItem(pref: SharedPreferences, key: String, data: Map<K, V>) {
        pref.edit { putString(key, Gson().toJson(data, TypeToken.getParameterized(Map::class.java, K::class.java, V::class.java).type)) }
    }
}
