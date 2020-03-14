package com.madness.collision.unit.api_viewing

import android.graphics.Bitmap
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.madness.collision.unit.api_viewing.data.ApiViewingApp

internal class ApiDecentViewModel() : ViewModel() {
    lateinit var app: MutableLiveData<ApiViewingApp>
    lateinit var type: MutableLiveData<Int>
    lateinit var back: MutableLiveData<Bitmap>
}
