package com.madness.collision.util

import com.google.common.truth.Truth
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.jupiter.api.Test

internal class PrefsUtilTest {

    @Test
    internal fun testFromJson() {
        val mockJson = "{\"Key1\":\"1\",\"Key2\":\"2\"}"
        val reMap: Map<String, String> = Gson().fromJson(mockJson, TypeToken.getParameterized(Map::class.java, String::class.java, String::class.java).type) ?: emptyMap()
        Truth.assertThat(reMap["Key1"]).isEqualTo("1")
        Truth.assertThat(reMap["Key2"]).isEqualTo("2")
    }

    @Test
    internal fun testToJson() {
        val mockMap = mapOf("Key1" to "1", "Key2" to "2")
        val reString = Gson().toJson(mockMap, TypeToken.getParameterized(Map::class.java, String::class.java, String::class.java).type)
        Truth.assertThat(reString).isEqualTo("{\"Key1\":\"1\",\"Key2\":\"2\"}")
    }
}