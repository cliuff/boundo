package com.madness.collision.unit.school_timetable.data

class Course(val name: String) : HashSet<Educating>() {
    constructor(courseS: List<CourseSingleton>) : this(courseS[0].name){
        addAll(courseS.map { it.educating })
    }

    companion object {
        /**
         * classify course repetitions by course name
         */
        fun mergeLegacy(items: List<CourseSingleton>): List<Course>{
            return items.groupBy { it.name }.entries.map { Course(it.value) }
        }
    }
}