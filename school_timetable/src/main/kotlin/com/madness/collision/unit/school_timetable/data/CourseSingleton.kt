package com.madness.collision.unit.school_timetable.data

import java.nio.charset.StandardCharsets

class CourseSingleton(var name: String, val educating: Educating) {
    var legacyDuplicate = false
    var legacyUid = ""
    var legacyTemplate = '?'
    var legacyClassPhase = "11" // 11,12,13,21,22,23,31,32,33
    var legacyClassPeriod = 'a' // a: 1 class, b: 2 classes, c: 3 classes
    // position in timetable
    var legacyClassSchedule = 1

    constructor() : this("", Educating())

    fun renderUid(){
        val uidBuilder = StringBuilder(name)
        educating.repetitions.forEach {
            uidBuilder.append(it.fromWeek).append(it.toWeek).append(it.fortnightly)
        }
        uidBuilder.append(educating.dayOfWeek)
        uidBuilder.append(legacyClassPhase)
        val ushText = uidBuilder.toString().toByteArray(StandardCharsets.UTF_8)
        uidBuilder.clear()
        for (bt in ushText) uidBuilder.append(bt.toInt())
        legacyUid = uidBuilder.toString().replace("-", "6")
    }

    fun legacyLiveUid(rp: Repetition): String{
        return StringBuilder(legacyUid)
                .append(rp.fromWeek)
                .append(rp.toWeek)
                .append(if (rp.fortnightly) 1 else 0)
                .toString()
    }

    override fun equals(other: Any?): Boolean {
        if (other !is CourseSingleton) return false
        if (name == other.name
                && educating == other.educating
                && legacyDuplicate == other.legacyDuplicate
                && legacyUid == other.legacyUid
                && legacyTemplate == other.legacyTemplate
                && legacyClassPhase == other.legacyClassPhase
                && legacyClassPeriod == other.legacyClassPeriod
                && legacyClassSchedule == other.legacyClassSchedule)
            return true
        return super.equals(other)
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + educating.hashCode()
        result = 31 * result + legacyDuplicate.hashCode()
        result = 31 * result + legacyUid.hashCode()
        result = 31 * result + legacyTemplate.hashCode()
        result = 31 * result + legacyClassPhase.hashCode()
        result = 31 * result + legacyClassPeriod.hashCode()
        result = 31 * result + legacyClassSchedule
        return result
    }

    fun copy(): CourseSingleton {
        val cs = CourseSingleton(name, educating.copy())
        cs.legacyDuplicate = legacyDuplicate
        cs.legacyUid = legacyUid
        cs.legacyTemplate = legacyTemplate
        cs.legacyClassPhase = legacyClassPhase
        cs.legacyClassPeriod = legacyClassPeriod
        cs.legacyClassSchedule = legacyClassSchedule
        return cs
    }
}