package com.madness.collision.unit

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

internal class UnitDescViewModel : ViewModel() {
    var description: MutableLiveData<Description> = MutableLiveData()
}
