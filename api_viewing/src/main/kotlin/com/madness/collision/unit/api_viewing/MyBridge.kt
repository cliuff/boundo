package com.madness.collision.unit.api_viewing

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import com.madness.collision.unit.Bridge
import com.madness.collision.unit.Unit
import com.madness.collision.unit.UpdatesProvider
import kotlin.reflect.KClass

object MyBridge: Bridge() {

    override val unitName: String = Unit.UNIT_NAME_API_VIEWING
    override val args: List<KClass<*>> = listOf(Bundle::class)

    /**
     * @param args extras: [Bundle]?
     */
    override fun getUnitInstance(vararg args: Any?): Unit {
        return MyUnit().apply { arguments = args[0] as Bundle? }
    }

    override fun getUpdates(): UpdatesProvider? {
        return MyUpdatesProvider()
    }

    fun clearSeals() {
        APIAdapter.seals.clear()
        APIAdapter.sealBack.clear()
    }

    fun clearApps(activity: ComponentActivity) {
        val viewModel: ApiViewingViewModel by activity.viewModels()
        viewModel.clearCache()
    }

    override fun getSettings(): Fragment? {
        return AvSettingsFragment()
    }
}
