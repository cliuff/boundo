package com.madness.collision.instant

import androidx.fragment.app.Fragment
import com.madness.collision.unit.DynamicItem

internal open class InstantItem(displayNameResId: Int, val requiredUnitName: String = "", descriptionPageGetter: (() -> Fragment)? = null)
    : DynamicItem(displayNameResId, descriptionPageGetter) {

    val hasRequiredUnit: Boolean
        get() = requiredUnitName.isNotEmpty()

}
