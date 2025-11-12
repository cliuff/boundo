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
    val specs: Map<DistroSpec.Id<*>, DistroSpec>

    @Suppress("UNCHECKED_CAST")
    operator fun <Spec : DistroSpec> contains(spec: DistroSpec.Id<Spec>): Boolean =
        specs.contains(spec)

    @Suppress("UNCHECKED_CAST")
    operator fun <Spec : DistroSpec> get(spec: DistroSpec.Id<Spec>): Spec? {
        return specs[spec] as? Spec
    }
}


/** DistributionSpecification */
sealed interface DistroSpec {
    val id: Id<*>

    sealed interface Id<Spec : DistroSpec>

    data class OneUI(val verCode: Int, val verName: String) : DistroSpec {
        companion object Id : DistroSpec.Id<OneUI>
        override val id: DistroSpec.Id<*> = Id
    }

    data class EMUI(val apiLevel: Int) : DistroSpec {
        companion object Id : DistroSpec.Id<EMUI>
        override val id: DistroSpec.Id<*> = Id
    }

    data class MIUI(val verCode: Int, val verName: String, val displayVersion: String?) : DistroSpec {
        companion object Id : DistroSpec.Id<MIUI>
        override val id: DistroSpec.Id<*> = Id
    }

    data class HyperOS(val verCode: Int, val verName: String, val displayVersion: String?) : DistroSpec {
        companion object Id : DistroSpec.Id<HyperOS>
        override val id: DistroSpec.Id<*> = Id
    }

    data class LineageOS(val apiLevel: Int) : DistroSpec {
        companion object Id : DistroSpec.Id<LineageOS>
        override val id: DistroSpec.Id<*> = Id
    }
}


/** UndefinedDistro */
data object UndefDistro : DistroBuild {
    override val displayName: String = "Android"
    override val specs: Map<DistroSpec.Id<*>, DistroSpec> = emptyMap()
}

data class OneUiDistro(val oneUI: DistroSpec.OneUI) : DistroBuild {
    override val displayName: String = "OneUI"
    override val specs: Map<DistroSpec.Id<*>, DistroSpec> = specMapOf(oneUI)
}

data class EmuiDistro(val emui: DistroSpec.EMUI) : DistroBuild {
    override val displayName: String = "EMUI"
    override val specs: Map<DistroSpec.Id<*>, DistroSpec> = specMapOf(emui)
}

data class MiuiDistro(val miui: DistroSpec.MIUI) : DistroBuild {
    override val displayName: String = "MIUI"
    override val specs: Map<DistroSpec.Id<*>, DistroSpec> = specMapOf(miui)
}

data class HyperOsDistro(val miui: DistroSpec.MIUI, val hyperOS: DistroSpec.HyperOS) : DistroBuild {
    override val displayName: String = "HyperOS"
    override val specs: Map<DistroSpec.Id<*>, DistroSpec> = specMapOf(miui, hyperOS)
}

data class LineageOsDistro(val lineageOS: DistroSpec.LineageOS) : DistroBuild {
    override val displayName: String get() = "LineageOS"
    override val specs: Map<DistroSpec.Id<*>, DistroSpec> = specMapOf(lineageOS)
}


private fun specMapOf(spec: DistroSpec) = mapOf(spec.id to spec)
private fun specMapOf(vararg specs: DistroSpec) = specs.associateBy(DistroSpec::id)

@Suppress("PrivateApi")
private fun getBuild(): DistroBuild {
    run oneUI@{
        val semPlatformInt = runCatching {
            android.os.Build.VERSION::class.java
                .getDeclaredField("SEM_PLATFORM_INT")
                .getInt(null)
        }.onFailure(Throwable::printStackTrace)
        val verCode = semPlatformInt.getOrDefault(-1)
        if (verCode <= 90000) return@oneUI
        val verName = (verCode - 90000).let { i -> "${i/10000}.${(i%10000)/100}" }
        val oneUI = DistroSpec.OneUI(verCode, verName)
        return OneUiDistro(oneUI)
    }
    run emui@{
        val api = BuildProp["ro.build.hw_emui_api_level"]?.toIntOrNull() ?: return@emui
        val emui = DistroSpec.EMUI(api)
        return EmuiDistro(emui)
    }
    run miui@{
        val miui = run {
            val verCode = BuildProp["ro.miui.ui.version.code"]?.toIntOrNull() ?: return@miui
            val verName = BuildProp["ro.miui.ui.version.name"] ?: return@miui
            val displayVersion = parseMiuiDisplayVersion(verName)
            DistroSpec.MIUI(verCode, verName, displayVersion)
        }
        run hyper@{
            val verCode = BuildProp["ro.mi.os.version.code"]?.toIntOrNull() ?: return@hyper
            val verName = BuildProp["ro.mi.os.version.name"] ?: return@hyper
            val displayVersion = parseHyperOsDisplayVersion(verName)
            val hyperOS = DistroSpec.HyperOS(verCode, verName, displayVersion)
            return HyperOsDistro(miui, hyperOS)
        }
        return MiuiDistro(miui)
    }
    run los@{
        val api = BuildProp["ro.lineage.build.version.plat.sdk"]?.toIntOrNull() ?: return@los
        return LineageOsDistro(DistroSpec.LineageOS(api))
    }
    return UndefDistro
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

private fun parseHyperOsDisplayVersion(verName: String): String? {
    // OS1.0
    if (verName.matches("""OS\d.*""".toRegex())) return verName.substring(2)
    return null
}
