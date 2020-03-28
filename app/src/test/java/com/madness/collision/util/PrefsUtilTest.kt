package com.madness.collision.util

import com.google.common.truth.Truth
import org.junit.jupiter.api.Test

internal class PrefsUtilTest {

    @Test
    internal fun testFromJson() {
        val mockJson = "{\"Key1\":1,\"Key2\":2}"
        val reMap = mockJson.jsonNestedTo<Map<String, Int>>()
        Truth.assertThat(reMap["Key1"]).isEqualTo(1)
        Truth.assertThat(reMap["Key2"]).isEqualTo(2)
    }

    @Test
    internal fun testToJson() {
        val mockMap = mapOf("Key1" to 1, "Key2" to 2)
        val reString = mockMap.nestedToJson<Map<String, Int>>()
        Truth.assertThat(reString).isEqualTo("{\"Key1\":1,\"Key2\":2}")
    }
}