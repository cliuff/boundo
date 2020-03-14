package com.madness.collision.unit.api_viewing

import androidx.fragment.app.Fragment
import com.madness.collision.unit.UpdatesProvider

internal class MyUpdatesProvider : UpdatesProvider() {

    override fun getFragment(): Fragment? {
        return MyUpdatesFragment()
    }
}
