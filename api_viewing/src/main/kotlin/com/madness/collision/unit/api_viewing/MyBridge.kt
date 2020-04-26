package com.madness.collision.unit.api_viewing

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import com.madness.collision.unit.Bridge
import com.madness.collision.unit.Unit
import com.madness.collision.unit.UpdatesProvider
import com.madness.collision.unit.api_viewing.util.PrefUtil
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

    override fun getSettings(): Fragment? {
        return AvSettingsFragment()
    }

    fun clearSeals() {
        APIAdapter.seals.clear()
        APIAdapter.sealBack.clear()
    }

    fun clearApps(activity: ComponentActivity) {
        val viewModel: ApiViewingViewModel by activity.viewModels()
        viewModel.clearCache()
    }

    fun initTagSettings(context: Context, prefSettings: SharedPreferences) {
        val tagSettings = mutableSetOf<String>()
        listOf(
                R.string.prefAvTagsValuePackageInstallerGp,
                R.string.prefAvTagsValuePackageInstallerPi,
                R.string.prefAvTagsValueCrossPlatformFlu,
                R.string.prefAvTagsValueCrossPlatformRn,
                R.string.prefAvTagsValueCrossPlatformXar,
                R.string.prefAvTagsValueNativeLibArm,
                R.string.prefAvTagsValueNativeLibX86,
                R.string.prefAvTagsValuePrivilegeSys,
                R.string.prefAvTagsValueIconAdaptive
        ).forEach {
            tagSettings.add(context.getString(it))
        }
        prefSettings.edit {
            putStringSet(PrefUtil.AV_TAGS, tagSettings)
        }
    }

    fun updateTagSettingsAi(context: Context, prefSettings: SharedPreferences) {
        val tagSettings = prefSettings.getStringSet(PrefUtil.AV_TAGS, emptySet())!!.toMutableSet()
        tagSettings.add(context.getString(R.string.prefAvTagsValueIconAdaptive))
        prefSettings.edit {
            putStringSet(PrefUtil.AV_TAGS, tagSettings)
        }
    }
    
}
