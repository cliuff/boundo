package com.madness.collision.unit

import android.content.Context
import android.content.SharedPreferences
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.play.core.splitinstall.SplitInstallManager
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import com.madness.collision.Democratic
import com.madness.collision.R
import com.madness.collision.main.MainViewModel
import com.madness.collision.misc.MiscApp
import com.madness.collision.util.P
import com.madness.collision.util.PrefsUtil
import com.madness.collision.util.X
import com.madness.collision.util.objectInstance

/**
 * Dynamic feature
 */
abstract class Unit: Fragment(), Democratic {

    companion object {
        const val UNIT_NAME_API_VIEWING = "api_viewing"
        const val UNIT_NAME_SCHOOL_TIMETABLE = "school_timetable"
        const val UNIT_NAME_IMAGE_MODIFYING = "image_modifying"
        const val UNIT_NAME_COOL_APP = "cool_app"
        const val UNIT_NAME_NO_MEDIA = "no_media"
        const val UNIT_NAME_THEMED_WALLPAPER = "themed_wallpaper"
        const val UNIT_NAME_AUDIO_TIMER = "audio_timer"
        const val UNIT_NAME_WE_CHAT_EVO = "we_chat_evo"
        const val UNIT_NAME_QQ_CONTACTS = "qq_contacts"
        private val UNIT_CLASSES: MutableMap<String, Class<Unit>> = hashMapOf()
        private val UNIT_BRIDGE_CLASSES: MutableMap<String, Class<Bridge>> = hashMapOf()
        private val mUnitDescriptions: List<Description> = listOf(
                Description(UNIT_NAME_API_VIEWING, R.string.apiViewer, R.drawable.ic_app_24).setDescResId(R.string.unit_desc_av),
                Description(UNIT_NAME_SCHOOL_TIMETABLE, R.string.unit_school_timetable, R.drawable.ic_tt_24).setDescResId(R.string.unit_desc_st),
                Description(UNIT_NAME_IMAGE_MODIFYING, R.string.developertools_cropimage, R.drawable.ic_landscape_24).setDescResId(R.string.unit_desc_im),
                Description(UNIT_NAME_COOL_APP, R.string.developertools_appinfowidget, R.drawable.ic_widgets_24).setDescResId(R.string.unit_desc_ca),
                Description(UNIT_NAME_NO_MEDIA, R.string.tools_nm, R.drawable.ic_flip_24).setDescResId(R.string.unit_desc_nm)
                        .setRequirement(Description.Checker(R.string.unit_desc_requirement_nm) { X.belowOff(X.Q) }),
                Description(UNIT_NAME_THEMED_WALLPAPER, R.string.twService, R.drawable.ic_image_24).setDescResId(R.string.unit_desc_tw),
                Description(UNIT_NAME_AUDIO_TIMER, R.string.unit_audio_timer, R.drawable.ic_music_off_24).setDescResId(R.string.unit_desc_at),
                Description(UNIT_NAME_WE_CHAT_EVO, R.string.unit_we_chat_evo, R.drawable.ic_we_chat_24).setDescResId(R.string.unit_desc_we)
                        .setRequirement(
                                Description.Checker(R.string.unit_desc_requirement_shortcut) { X.aboveOn(X.N_MR1) },
                                Description.Checker(R.string.unit_desc_requirement_we) {
                                    MiscApp.getPackageInfo(it, packageName = "com.tencent.mm") != null
                                }),
                Description(UNIT_NAME_QQ_CONTACTS, R.string.unit_qq_contacts, R.drawable.ic_qq_24).setDescResId(R.string.unit_desc_qc)
                        .setRequirement(
                                Description.Checker(R.string.unit_desc_requirement_shortcut) { X.aboveOn(X.N_MR1) },
                                Description.Checker(R.string.unit_desc_requirement_qc) {
                                    MiscApp.getPackageInfo(it, packageName = "com.tencent.mobileqq") != null
                                })
        )
        private val UNIT_DESCRIPTIONS: Map<String, Description> = mUnitDescriptions.associateBy { it.unitName }
        val UNITS: List<String> = UNIT_DESCRIPTIONS.keys.toList().sorted()

        fun getInstalledUnits(context: Context): List<String> {
            return getInstalledUnits(SplitInstallManagerFactory.create(context))
        }

        fun getInstalledUnits(splitInstallManager: SplitInstallManager): List<String> {
            val installedModules = splitInstallManager.installedModules
            if (!installedModules.isNullOrEmpty()) return installedModules.toList()
            return UNITS.mapNotNull { if (getUnitClass(it) != null) it else null }
        }

        fun loadUnitClasses(
                context: Context,
                installedUnits: List<String> = getInstalledUnits(context),
                frequencies: Map<String, Int> = getFrequencies(context)
        ) {
            getSortedUnitNamesByFrequency(null, installedUnits, frequencies).forEach { unitName ->
                loadUnitClass(unitName)?.let { UNIT_CLASSES[unitName] = it }
                loadBridgeClass(unitName)?.let { UNIT_BRIDGE_CLASSES[unitName] = it }
            }
        }

        /**
         * From installed units
         */
        fun getSortedUnitNamesByFrequency(
                context: Context? = null,
                installedUnits: List<String> = getInstalledUnits(context!!),
                frequencies: Map<String, Int> = getFrequencies(context!!)
        ): List<String> {
            return frequencies.toList().filter { installedUnits.contains(it.first) }
                    .plus(installedUnits.filter { !frequencies.containsKey(it) }.map { it to 0 })
                    .sortedByDescending { it.second }.map { it.first }
        }

        fun getFrequencies(context: Context, pref: SharedPreferences = context.getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE)) : Map<String, Int> {
            return PrefsUtil.getCompoundItem(pref, P.UNIT_FREQUENCIES).mapValues { it.value.toInt() }
        }

        fun increaseFrequency(context: Context, unitName: String, pref: SharedPreferences = context.getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE)) {
            getFrequencies(context, pref).toMutableMap().apply {
                val f = if (X.aboveOn(X.N)) getOrDefault(unitName, 0) else get(unitName) ?: 0
                put(unitName, f + 1)
            }.let { PrefsUtil.putCompoundItem(pref, P.UNIT_FREQUENCIES, it) }
        }

        @Suppress("UNCHECKED_CAST")
        fun loadUnitClass(unitName: String): Class<Unit>? {
            val packagedName = UNIT_DESCRIPTIONS[unitName]?.packagedName ?: ""
            return try {
                Class.forName("$packagedName.MyUnit") as Class<Unit>
            } catch (e: ClassNotFoundException) {
                e.printStackTrace()
                null
            }
        }

        @Suppress("UNCHECKED_CAST")
        fun loadBridgeClass(unitName: String): Class<Bridge>? {
            val packagedName = UNIT_DESCRIPTIONS[unitName]?.packagedName ?: ""
            return try {
                Class.forName("$packagedName.MyBridge") as Class<Bridge>
            } catch (e: ClassNotFoundException) {
                e.printStackTrace()
                null
            }
        }

        fun getUnitClass(unitName: String): Class<Unit>? {
            return UNIT_CLASSES[unitName] ?: loadUnitClass(unitName)
        }

        fun getBridgeClass(unitName: String): Class<Bridge>? {
            return UNIT_BRIDGE_CLASSES[unitName] ?: loadBridgeClass(unitName)
        }

        fun getDescription(unitName: String) : Description? {
            return UNIT_DESCRIPTIONS[unitName]
        }

        fun getUnit(unitName: String, vararg args: Any?): Unit? {
            val bridge = getBridge(unitName) ?: return null
            return bridge.getUnitInstance(*args)
        }

        fun getBridge(unitName: String): Bridge? {
            val clazz = getBridgeClass(unitName) ?: return null
            return clazz.objectInstance
        }

        fun getUpdates(unitName: String): UpdatesProvider? {
            return getBridge(unitName)?.getUpdates()
        }
    }

    protected val mainViewModel : MainViewModel by activityViewModels()

    protected fun democratize() {
        democratize(mainViewModel)
    }

}
