package com.madness.collision.unit

import androidx.fragment.app.Fragment

abstract class UpdatesProvider {

    open fun getFragment(): Fragment? {
        return null
    }
}
