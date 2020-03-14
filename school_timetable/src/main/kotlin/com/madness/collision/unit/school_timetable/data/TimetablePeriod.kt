package com.madness.collision.unit.school_timetable.data

import java.lang.IllegalArgumentException

data class TimetablePeriod(val fromPeriod: Int, val classDuration: Int) {
    companion object {
        const val PERIOD_ONE = 1
        const val PERIOD_TWO = 2
        const val PERIOD_THREE = 3
        const val PERIOD_FOUR = 4
        const val PERIOD_FIVE = 5
        const val PERIOD_SIX = 6
        const val PERIOD_SEVEN = 7
        const val PERIOD_EIGHT = 8
        const val PERIOD_NINE = 9
        const val PERIOD_TEN = 10
        const val PERIOD_ELEVEN = 11
        const val PERIOD_TWELVE = 12
        const val PERIOD_THIRTEEN = 13
        const val PERIOD_FOURTEEN = 14

        const val PERIOD_DOUBLE_ONE_TWO = -1
        const val PERIOD_DOUBLE_THREE_FOUR = -3
        const val PERIOD_DOUBLE_FIVE_SIX = -5
        const val PERIOD_DOUBLE_SEVEN_EIGHT = -7
        const val PERIOD_DOUBLE_NINE_TEN = -9

        fun parse(src: String): TimetablePeriod {
            if (src.contains(",")) {
                val content = src.split(",")
                val start = content[0].toInt()
                val duration = content[1].toInt() - start
                return TimetablePeriod(start, duration)
            }
            val value = src.toInt()
            if (value in 1..14) return TimetablePeriod(value)
            else throw IllegalArgumentException("cannot be converted to legit Timetable Period")
        }
    }

    constructor(period: Int) : this(
            if (period in -9..-1) -period
            else period,
            if (period in -9..-1) 2
            else 1
    )

    constructor(period: IntRange) : this(period.start, period.count())

    fun isSingleClass(): Boolean = classDuration == 1

    override fun toString(): String = (fromPeriod..(fromPeriod + classDuration)).toString()

    fun getUidLegacy(): String = fromPeriod.toString() + (fromPeriod + classDuration).toString()
/*
    override fun equals(other: Any?): Boolean {
        if (other !is TimetablePeriod) return false
        if (fromPeriod == other.fromPeriod
                && classDuration == other.classDuration)
            return true
        return false
    }

    override fun hashCode(): Int {
        var result = fromPeriod
        result = 31 * result + classDuration
        return result
    }*/
}