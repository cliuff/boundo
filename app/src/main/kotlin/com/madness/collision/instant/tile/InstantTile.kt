package com.madness.collision.instant.tile

import android.content.Context
import android.service.quicksettings.TileService
import androidx.fragment.app.Fragment
import com.madness.collision.instant.InstantItem
import kotlin.reflect.KClass

internal class InstantTile<T : TileService>(displayNameResId: Int, val tileClass: KClass<T>,
                                    requiredUnitName: String = "", descriptionPageGetter: (() -> Fragment)? = null
): InstantItem(displayNameResId, requiredUnitName, descriptionPageGetter) {

    fun setRequirement(checker: (context: Context) -> Boolean): InstantTile<T> {
        availabilityChecker = checker
        return this
    }

}

internal inline fun <reified T: TileService> instantTile(displayNameResId: Int, requiredUnitName: String = "",
                                                noinline descriptionPageGetter: (() -> Fragment)? = null): InstantTile<T> {
    return InstantTile(displayNameResId, T::class, requiredUnitName, descriptionPageGetter)
}
