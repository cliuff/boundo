package com.madness.collision.unit.api_viewing

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.madness.collision.unit.api_viewing.data.ApiViewingApp

internal class ApiInfoViewModel: ViewModel(){
    lateinit var app: MutableLiveData<ApiViewingApp>
}
