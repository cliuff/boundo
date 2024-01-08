package com.madness.collision.settings.instant

import android.content.Context
import androidx.fragment.app.Fragment
import com.madness.collision.util.controller.DynamicItem
import kotlin.reflect.KClass

sealed class InstantItem(
    displayNameResId: Int,
    val requiredUnitName: String = "",
    descriptionPageGetter: (() -> Fragment)? = null
) : DynamicItem(displayNameResId, descriptionPageGetter) {

    val hasRequiredUnit: Boolean
        get() = requiredUnitName.isNotEmpty()

}

class InstantComponent<T: Any>(
    displayNameResId: Int,
    val klass: KClass<T>,
    requiredUnitName: String = "",
    descriptionPageGetter: (() -> Fragment)? = null
): InstantItem(displayNameResId, requiredUnitName, descriptionPageGetter) {

    fun setRequirement(checker: (context: Context) -> Boolean): InstantComponent<T> {
        availabilityChecker = checker
        return this
    }
}

class InstantShortcut(
    val id: String,
    displayNameResId: Int,
    requiredUnitName: String = "",
    descriptionPageGetter: (() -> Fragment)? = null
): InstantItem(displayNameResId, requiredUnitName, descriptionPageGetter) {

    fun setRequirement(checker: (context: Context) -> Boolean): InstantShortcut {
        availabilityChecker = checker
        return this
    }
}
