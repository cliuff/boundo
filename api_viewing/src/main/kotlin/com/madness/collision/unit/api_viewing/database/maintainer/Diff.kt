/*
 * Copyright 2023 Clifford Liu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.madness.collision.unit.api_viewing.database.maintainer

import androidx.room.*

@Entity(tableName = "diff_change")
class DiffChange(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    val id: Int = 0,
    @Embedded(prefix = "diff_")
    val diff: DiffInfo,
    @ColumnInfo(name = "type")
    val type: DiffType,
    @ColumnInfo(name = "col_name")
    val columnName: String,
    @ColumnInfo(name = "old_val")
    val oldValue: String,
    @ColumnInfo(name = "new_val")
    val newValue: String,
) {
    val isNone: Boolean get() = type == DiffType.None
    val isNotNone: Boolean get() = !isNone
}

class DiffInfo(
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "time")
    val timeMills: Long,
    @ColumnInfo(name = "pkg_name")
    val packageName: String,
)

open class DiffType(val code: Int) {
    object None : DiffType(0)
    object Add : DiffType(1)
    object Remove : DiffType(2)
    object Change : DiffType(3)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DiffType) return false
        if (code != other.code) return false
        return true
    }

    override fun hashCode(): Int = code
}

class DiffConverters {
    @TypeConverter
    fun fromType(type: DiffType): Int = type.code

    @TypeConverter
    fun toType(code: Int): DiffType = DiffType(code)
}