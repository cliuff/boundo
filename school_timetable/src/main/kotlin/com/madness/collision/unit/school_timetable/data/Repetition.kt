package com.madness.collision.unit.school_timetable.data

class Repetition(val fromWeek: Int, val toWeek: Int, val repeatWeeks: Int = WEEKS_ALL) {
    val fortnightly = repeatWeeks != WEEKS_ALL
    private val range = (fromWeek..toWeek).run { if (fortnightly) this step 2 else this }
    var displayFrom = fromWeek
    private set
    var displayTo = toWeek
    private set

    companion object {
        const val WEEKS_ALL: Int = 0
        const val WEEKS_ODD: Int = 1
        const val WEEKS_EVEN: Int = 2
        const val CN_ODD: Char = '单'
        const val CN_EVEN: Char = '双'
        const val ODD: Char = 'o'
        const val EVEN: Char = 'e'
        const val ALL: Char = 'a'
        val REGEX_RAW = "(\\d+)-(\\d+)([${"$CN_ODD$CN_EVEN"}]?)".toRegex()
        val REGEX_STANDARD = "(\\d+)-(\\d+)\\*(\\d+)-(\\d+)([${"$ALL$EVEN$ODD"}])".toRegex()

        /**
         * week 1 - week 7 single week & week 10 - 15 : 1-7单, 10-15
         */
        fun parseRaw(repetitionsText: String): Array<Repetition> {
            if (repetitionsText.isBlank()) return emptyArray()
            val srcText = repetitionsText.replace(" ", "")
            val repsText: Array<String> = if (srcText.contains(",")) {
                srcText.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            } else arrayOf(srcText)
            val repsResult = ArrayList<Repetition>()
            for (period in repsText) {
                var fromWeek: Int
                var toWeek: Int
                var displayFrom: Int
                var displayTo: Int
                var repeatWeeks = WEEKS_ALL
                if (period.contains("-")) {
                    val subPeriod = period.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    displayFrom = Integer.parseInt(subPeriod[0])
                    fromWeek = displayFrom
                    when (subPeriod[1].last()) {
                        CN_ODD -> {
                            repeatWeeks = WEEKS_ODD
                            if (fromWeek % 2 == 0)
                                fromWeek += 1
                        }
                        CN_EVEN -> {
                            repeatWeeks = WEEKS_EVEN
                            if (fromWeek % 2 != 0)
                                fromWeek += 1
                        }
                    }
                    displayTo = Integer.parseInt(subPeriod[1].let { if (repeatWeeks != WEEKS_ALL) it.dropLast(1) else it })
                    toWeek = displayTo
                    when (repeatWeeks) {
                        WEEKS_ODD -> if (toWeek % 2 == 0) toWeek -= 1
                        WEEKS_EVEN -> if (toWeek % 2 != 0) toWeek -= 1
                    }
                } else {
                    fromWeek = Integer.parseInt(period)
                    toWeek = fromWeek
                    displayFrom = fromWeek
                    displayTo = toWeek
                }
                if (fromWeek > toWeek) {
                    fromWeek = 0
                    toWeek = 0
                    displayFrom = 0
                    displayTo = 0
                }
                Repetition(fromWeek, toWeek, repeatWeeks).run {
                    this.displayFrom = displayFrom
                    this.displayTo = displayTo
                    repsResult.add(this)
                }
            }
            return repsResult.toTypedArray()
        }

        /**
         * week 1 - week 7 single week & week 10 - 15 : 1-7*1-7o, 10-15*10-15a
         */
        fun parseStandard(repetitionsText: String): Array<Repetition> {
            if (repetitionsText.isBlank()) return emptyArray()
            val srcText = repetitionsText.replace(" ", "")
            val repsResult = ArrayList<Repetition>()
            for (period in REGEX_STANDARD.findAll(srcText)) {
                val (fromWeek, toWeek, displayFrom, displayTo, repeatWeeks) = period.destructured
                Repetition(fromWeek.toInt(), toWeek.toInt(), when (repeatWeeks[0]) {
                    EVEN -> WEEKS_EVEN;ODD -> WEEKS_ODD;else -> WEEKS_ALL
                }).run {
                    this.displayFrom = displayFrom.toInt()
                    this.displayTo = displayTo.toInt()
                    repsResult.add(this)
                }
            }
            return repsResult.toTypedArray()
        }

        fun parseFortnightly(fromWeek: Int, toWeek: Int, fortnightly: Boolean = false): Repetition {
            val repeatWeeks = if (fortnightly){
                if (fromWeek % 2 == 0) WEEKS_EVEN
                else WEEKS_ODD
            } else WEEKS_ALL
            return Repetition(fromWeek, toWeek, repeatWeeks)
        }

        fun toString(repetitions: Array<Repetition>): String {
            if (repetitions.isEmpty()) return ""
            val result = StringBuilder()
            repetitions.forEach { repetition -> result.append(repetition.toString()).append(", ") }
            return result.dropLast(2).toString()
        }

        fun toStandardString(repetitions: Array<Repetition>): String {
            if (repetitions.isEmpty()) return ""
            val result = StringBuilder()
            repetitions.forEach { repetition -> result.append(repetition.toStandardString()).append(", ") }
            return result.dropLast(2).toString()
        }
    }

    infix fun contains(value: Int): Boolean = value in range

    /**
     * Standard => Raw
     * 3-17*2-18o => 2-18单
     */
    fun toStandardString(): String{
        return StringBuilder("$fromWeek-$toWeek*$displayFrom-$displayTo").apply {
            when(repeatWeeks){
                WEEKS_ODD -> append(ODD)
                WEEKS_EVEN -> append(EVEN)
                else -> append(ALL)
            }
        }.toString()
    }

    override fun toString(): String {
        if (displayFrom == displayTo) return displayFrom.toString()
        return StringBuilder("$displayFrom-$displayTo").apply {
            when(repeatWeeks){
                WEEKS_ODD -> append(CN_ODD)
                WEEKS_EVEN -> append(CN_EVEN)
            }
        }.toString()
    }
    /*
    override fun equals(other: Any?): Boolean {
        if (other !is Repetition) return false
        if (fromWeek == other.fromWeek
                && toWeek == other.toWeek
                && fortnightly == other.fortnightly)
            return true
        return false
    }

    override fun hashCode(): Int {
        var result = fromWeek
        result = 31 * result + toWeek
        result = 31 * result + fortnightly.hashCode()
        return result
    }*/
}
