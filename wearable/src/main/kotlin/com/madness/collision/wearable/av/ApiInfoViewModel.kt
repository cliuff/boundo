package com.madness.collision.wearable.av

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.madness.collision.wearable.av.data.ApiViewingApp

internal class ApiInfoViewModel: ViewModel(){
    lateinit var app: MutableLiveData<ApiViewingApp>
}
