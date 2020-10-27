/*
 * Copyright 2020 Clifford Liu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.madness.collision.unit

import android.content.Context

class DescRetriever(private val context: Context? = null) {
    /**
     * Whether to check if a unit is installed when retrieving all descriptions
     * Need [context]
     */
    private var doInstall: Boolean = false

    /**
     * This state is available no matter installed or not
     * Need [context]
     */
    private var doAvailable: Boolean = false

    /**
     * This state is available when both installed and available
     * Need [context]
     */
    private var doEnable: Boolean = false

    /**
     * This state is available when enabled
     * Need [context]
     */
    private var doPin: Boolean = false

    /**
     * Set to true to get only the results that match included state
     */
    private var doFilter: Boolean = false

    fun includeInstallState(): DescRetriever {
        doInstall = true
        return this
    }

    fun includeAvailableState(): DescRetriever {
        doAvailable = true
        return this
    }

    /**
     * This will include extra install and available states
     */
    fun includeEnableState(): DescRetriever {
        doEnable = true
        return this
    }

    /**
     * This will include extra enable state, which will include extra install and available states
     */
    fun includePinState(): DescRetriever {
        doPin = true
        return this
    }

    fun doFilter(): DescRetriever {
        doFilter = true
        return this
    }

    private fun retrieve(isAll: Boolean = false, vararg units: String): List<StatefulDescription> {
        val hasContext = context != null
        val doCustom = units.isNotEmpty()
        val doCheckPin = doPin
        // set to true if doPin
        val doCheckEnable = doEnable || doCheckPin
        // set to true if doEnable
        val doCheckAvailable = doAvailable || doCheckEnable
        // set to true if doEnable
        val doCheckInstall = doInstall || doCheckEnable
        val installedUnits = if (doCheckInstall && hasContext) Unit.getInstalledUnits(context!!)
        else emptyList()
        val disabledUnits = if (doCheckEnable && hasContext) Unit.getDisabledUnits(context!!)
        else emptyList()
        val pinnedUnits = if (doCheckPin && hasContext) Unit.getPinnedUnits(context!!)
        else emptyList()
        val unitsToCheck = if (doCustom) units.toList()
        else (if (isAll) Unit.UNITS else installedUnits)
        return unitsToCheck.mapNotNull {
            val desc = Unit.getDescription(it) ?: return@mapNotNull null
            val isDynamic = desc !is StaticDescription
            val isInstalled = if (isAll || doCustom) (doCheckInstall && installedUnits.contains(it)) else true
            val isAvailable = doCheckAvailable && hasContext && desc.isAvailable(context!!)
            val isEnabled = isInstalled && isAvailable && doCheckEnable && !disabledUnits.contains(it)
            val isPinned = isEnabled && doCheckPin && pinnedUnits.contains(it)
            if (doFilter && ((doCheckPin && !isPinned) || (doCheckEnable && !isEnabled)
                            || (doCheckAvailable && !isAvailable) || (doCheckInstall && !isInstalled)))
                return@mapNotNull null
            StatefulDescription(it, desc, isDynamic, isInstalled, isAvailable, isEnabled, isPinned)
        }
    }

    fun retrieveAll(): List<StatefulDescription> {
        return retrieve(true)
    }

    fun retrieveInstalled(): List<StatefulDescription> {
        includeInstallState()
        return retrieve(false)
    }

    fun retrieve(vararg units: String): List<StatefulDescription> {
        includeInstallState()
        return retrieve(false, *units)
    }
}
