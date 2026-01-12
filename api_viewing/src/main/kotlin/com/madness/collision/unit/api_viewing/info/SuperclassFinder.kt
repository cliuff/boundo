package com.madness.collision.unit.api_viewing.info

import dalvik.system.PathClassLoader
import java.io.File

/**
 * Find superclasses in APKs.
 * For finding component names registered in Android manifests,
 * please note that they may actually not be present in DEX files at all.
 * So be prepared for missing results.
 *
 * [LoadSuperFinder] is dozens of times faster than [DexLibSuperFinder] according to benchmarks.
 * But for a very small number of cases, [DexLibSuperFinder] reports a couple more results,
 * while the other way around does happen in even fewer cases.
 */
interface SuperclassFinder {
    fun resolve(apks: List<String>, names: Set<String>): Set<String>
}

/** Find superclasses by [PathClassLoader]. Must be used for read-only files. */
class LoadSuperFinder : SuperclassFinder {
    override fun resolve(apks: List<String>, names: Set<String>): Set<String> {
        if (apks.isEmpty()) return names
        if (names.isEmpty()) return emptySet()
        val classes = HashSet<String>(names.size * 2)
        classes.addAll(names)
        try {
            val appDexPath = apks.joinToString(File.pathSeparator)
            val loader = PathClassLoader(appDexPath, ClassLoader.getSystemClassLoader())
            for (service in names) {
                try {
                    val klass = Class.forName(service, false, loader)
                    klass.superclass?.name?.let(classes::add)
                } catch (_: ClassNotFoundException) {
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        return classes
    }
}

/** Find superclasses by [DexContainerFactory], iteratively. */
class DexLibSuperFinder : SuperclassFinder {
    override fun resolve(apks: List<String>, names: Set<String>): Set<String> {
        if (apks.isEmpty()) return names
        if (names.isEmpty()) return emptySet()
        val typeNames = names.mapTo(HashSet(names.size)) { n ->
            "L" + n.replace('.', '/') + ";"
        }
        val superclasses = ArrayList<String>(names.size)
        try {
            apk@ for (apkPath in apks) {
                dex@ for (ent in DexContainerFactory.load(apkPath).dexEntrySeq) {
                    klass@ for (classDef in ent.dexFile.classes) {
                        if (classDef.type in typeNames) {
                            val superOrThis = classDef.superclass?.let(::mapToName)
                                ?: mapToName(classDef.type) ?: continue@klass
                            superclasses.add(superOrThis)
                        }
                        if (superclasses.size == names.size) break@apk
                    }
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        return names + superclasses
    }

    // Landroid/app/AppComponentFactory; -> android.app.AppComponentFactory
    private fun mapToName(type: String): String? {
        val name = kotlin.run {
            if (type.startsWith('L') && type.endsWith(';')) {
                type.substring(1, type.length - 1).replace('/', '.')
            } else {
                type
            }
        }
        return name.takeUnless { it.isBlank() }
    }
}
