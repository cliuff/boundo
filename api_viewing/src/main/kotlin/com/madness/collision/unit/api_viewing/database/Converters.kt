package com.madness.collision.unit.api_viewing.database

import androidx.room.TypeConverter
import com.madness.collision.util.jsonSimpleTo
import com.madness.collision.util.simpleToJson

class Converters {
    @TypeConverter
    fun arrayToString(array: BooleanArray): String {
        return array.simpleToJson()
    }

    @TypeConverter
    fun fromString(string: String?): BooleanArray {
        return string?.jsonSimpleTo<BooleanArray>() ?: BooleanArray(7) { false }
    }
}
