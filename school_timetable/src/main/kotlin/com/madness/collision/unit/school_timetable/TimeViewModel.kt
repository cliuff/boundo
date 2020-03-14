package com.madness.collision.unit.school_timetable

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class TimeViewModel: ViewModel(){
    val durationClassInferiorDisplay: MutableLiveData<String> = MutableLiveData("")
    val durationRestInferiorDisplay: MutableLiveData<String> = MutableLiveData("")
    val durationRestSuperiorDisplay: MutableLiveData<String> = MutableLiveData("")
    val durationRestInferiorAmDisplay: MutableLiveData<String> = MutableLiveData("")
    val durationRestSuperiorAmDisplay: MutableLiveData<String> = MutableLiveData("")
    val durationRestInferiorPmDisplay: MutableLiveData<String> = MutableLiveData("")
    val durationRestSuperiorPmDisplay: MutableLiveData<String> = MutableLiveData("")
    val durationRestInferiorEveDisplay: MutableLiveData<String> = MutableLiveData("")
    val durationRestSuperiorEveDisplay: MutableLiveData<String> = MutableLiveData("")
    var timeDateStart: MutableLiveData<String> = MutableLiveData("")
    var timeAm: MutableLiveData<String> = MutableLiveData("")
    var timePm: MutableLiveData<String> = MutableLiveData("")
    var timeEve: MutableLiveData<String> = MutableLiveData("")
}
