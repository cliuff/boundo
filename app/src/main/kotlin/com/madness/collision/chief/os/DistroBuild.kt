/*
 * Copyright 2023 Clifford Liu
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

package com.madness.collision.chief.os

/** Android distribution build info during runtime */
val distro: DistroBuild = getBuild()

/** DistributionBuild */
sealed interface DistroBuild {
    val displayName: String
}

/** UndefinedDistro */
data object UndefDistro : DistroBuild {
    override val displayName: String = "Android"
}

data class EmuiDistro(val apiLevel: Int) : DistroBuild {
    override val displayName: String = "EMUI"
}

data class HarmonyOsDistro(val verName: String) : DistroBuild {
    override val displayName: String = "HarmonyOS"
}

data class MiuiDistro(
    val verCode: Int,
    val verName: String,
    val displayVersion: String?,
) : DistroBuild {
    override val displayName: String = "MIUI"
}

private fun getBuild(): DistroBuild {
    run hos@{
        if (getHuaweiOsBrand() != "harmony") return@hos
        val verName = BuildProp["hw_sc.build.platform.version"] ?: return@hos
        return HarmonyOsDistro(verName)
    }
    run miui@{
        val verCode = BuildProp["ro.miui.ui.version.code"]?.toIntOrNull() ?: return@miui
        val verName = BuildProp["ro.miui.ui.version.name"] ?: return@miui
        val displayVersion = parseMiuiDisplayVersion(verName)
        return MiuiDistro(verCode, verName, displayVersion)
    }
    run emui@{
        val api = BuildProp["ro.build.hw_emui_api_level"]?.toIntOrNull() ?: return@emui
        return EmuiDistro(api)
    }
    return UndefDistro
}

private fun getHuaweiOsBrand(): Any? {
    return try {
        Class.forName("com.huawei.system.BuildEx")
            .getDeclaredMethod("getOsBrand").apply { isAccessible = true }
            .invoke(null)
    } catch (e: ClassNotFoundException) {
        null
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun parseMiuiDisplayVersion(verName: String): String? {
    return when {
        // MIUI 12.5,13-99 (V125,V130,V140)
        verName.matches("""V\d{3}""".toRegex()) -> {
            listOfNotNull(verName.substring(1, 3), verName[3].takeIf { it != '0' })
                .joinToString(separator = ".")
        }
        // MIUI V1-V5,6-12 (V1-V12)
        verName.matches("""V\d\d?""".toRegex()) -> {
            val verDigits = verName.substring(1)
            if (verDigits.toInt() >= 6) verDigits else verName
        }
        else -> null
    }
}
