package com.madness.collision.instant.tile

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.madness.collision.Democratic
import com.madness.collision.R
import com.madness.collision.main.MainViewModel

internal class MonthDataUsageDesc : Fragment(), Democratic {

    override fun createOptions(context: Context, toolbar: Toolbar, iconColor: Int): Boolean {
        toolbar.setTitle(R.string.tileData)
        return true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.instant_tile_month_data_usage_desc, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val mainViewModel: MainViewModel by activityViewModels()
        democratize(mainViewModel)
    }
}
