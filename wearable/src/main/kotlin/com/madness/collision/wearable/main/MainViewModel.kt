package com.madness.collision.wearable.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.madness.collision.wearable.util.mainApplication

internal class MainViewModel: ViewModel(){
    val insetBottom: MutableLiveData<Int> = MutableLiveData(mainApplication.insetBottom)
}
