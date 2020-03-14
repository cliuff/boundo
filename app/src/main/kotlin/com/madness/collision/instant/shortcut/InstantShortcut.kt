package com.madness.collision.instant.shortcut

import android.content.Context
import androidx.fragment.app.Fragment
import com.madness.collision.instant.InstantItem

internal class InstantShortcut(val id: String, displayNameResId: Int,
                               requiredUnitName: String = "",
                               descriptionPageGetter: (() -> Fragment)? = null
): InstantItem(displayNameResId, requiredUnitName, descriptionPageGetter) {

    fun setRequirement(checker: (context: Context) -> Boolean): InstantShortcut {
        availabilityChecker = checker
        return this
    }

}
