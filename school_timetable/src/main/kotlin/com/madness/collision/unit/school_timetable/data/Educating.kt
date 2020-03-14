package com.madness.collision.unit.school_timetable.data

class Educating {
    var educator = ""
    var location = ""
    var dayOfWeek = DayOfWeek.MONDAY
    var period = TimetablePeriod(TimetablePeriod.PERIOD_ONE, 1)
    var repetitions = arrayOf(Repetition(1, 1))

    override fun equals(other: Any?): Boolean {
        if (other !is Educating) return false
        if (educator == other.educator
                && location == other.location
                && dayOfWeek == other.dayOfWeek
                && period == other.period
                && repetitions.contentEquals(other.repetitions))
            return true
        return false
    }

    override fun hashCode(): Int {
        var result = educator.hashCode()
        result = 31 * result + location.hashCode()
        result = 31 * result + dayOfWeek
        result = 31 * result + period.hashCode()
        result = 31 * result + repetitions.contentHashCode()
        return result
    }

    fun copy(): Educating {
        val edu = Educating()
        edu.educator = educator
        edu.location = location
        edu.dayOfWeek = dayOfWeek
        edu.period = period.copy()
        edu.repetitions = repetitions.copyOf()
        return edu
    }
}