import com.cliuff.boundo.build.getCustomConfig
import java.io.ByteArrayOutputStream

plugins {
    alias(libs.plugins.android)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.ksp)
    // implement parcelable interface by using annotation
    id("kotlin-parcelize")
    alias(libs.plugins.aboutLibraries)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

aboutLibraries {
    // font licenses are configured under app/config
    collect.configPath = file("config")
}

android {
    var verInc = 0
    var verCommit: String? = null
    // ver.inc: property used by CI to enable incremental version by commit count
    val isIncVer = (findProperty("ver.inc") as? String)?.toBooleanStrictOrNull()
    if (isIncVer == true) {
        val commitIncOutput = ByteArrayOutputStream()
        val commitHashOutput = ByteArrayOutputStream()
        exec {
            commandLine("git", "rev-list", "--count", "HEAD")
            standardOutput = commitIncOutput
        }
        exec {
            commandLine("git", "rev-parse", "--short", "HEAD")
            standardOutput = commitHashOutput
        }
        verInc = commitIncOutput.toString().trim().toInt()
        verCommit = commitHashOutput.toString().trim()
    }

    // higher version supports older SDK versions
    buildToolsVersion = "36.0.0"
    sourceSets {
        getByName("main").java.srcDir("src/main/kotlin")
    }
    val customConfig = getCustomConfig(project)
    val buildPackage = customConfig.buildPackage
    val configSigning = customConfig.signing != null
    signingConfigs {
        customConfig.signing?.run {
            create("Sign4Release") {
                keyAlias = key.alias
                keyPassword = key.password
                storeFile = rootProject.file(store.path)
                storePassword = store.password
            }
        }
    }
    // namespace is used by R and BuildConfig classes
    namespace = "com.madness.collision"
    compileSdk = 36
    defaultConfig {
        // below: manifest placeholders
        manifestPlaceholders["buildPackage"] = buildPackage
        applicationId = "com.madness.collision"
        minSdk = 23
        targetSdk = 36
        // versionCode = baseVerCode + (verInc % baseCommitInc)
        versionCode = 25060200 + (verInc % 540)
        versionName = listOfNotNull("5.0.1", verCommit).joinToString(separator = "-")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testApplicationId = "${applicationId}.test"
        renderscriptSupportModeEnabled = true
        if (configSigning) {
            signingConfig = signingConfigs.getByName("Sign4Release")
        }
        // below: inject the desired values into BuildConfig and Res
        // the string values have to be wrapped in quotes because the value in local.properties does not have quotes
        buildConfigField("String", "BUILD_PACKAGE", "\"$buildPackage\"")
        // override with current time in release builds to increase performance during debugging
        buildConfigField("long", "BUILD_TIMESTAMP", "0")
        resValue("string", "buildPackage", buildPackage)
        // below: fix multi-locale support
        resourceConfigurations.addAll(arrayOf(
            "ar",
            "bn", "bn-rBD",
            "de", "de-rDE",
            "el", "el-rGR",
            "en", "en-rGB", "en-rUS",
            "es", "es-rES", "es-rUS",
            "fa", "fa-rAF", "fa-rIR",
            "fr", "fr-rFR",
            "hi", "hi-rIN",
            "in", "in-rID",
            "it", "it-rIT",
            "ja", "ja-rJP",
            "ko", "ko-rKR",
            "mr", "mr-rIN",
            "pa", "pa-rPK",
            "pl", "pl-rPL",
            "pt", "pt-rPT",
            "ru", "ru-rRU",
            "th", "th-rTH",
            "tr", "tr-rTR",
            "uk", "uk-rUA",
            "vi", "vi-rVN",
            "zh", "zh-rCN", "zh-rHK", "zh-rMO", "zh-rSG", "zh-rTW",
        ))
    }
    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".mortal"
            isDebuggable = true
            isJniDebuggable = false
            if (configSigning) {
                signingConfig = signingConfigs.getByName("Sign4Release")
            }
            renderscriptOptimLevel = 3
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        getByName("release") {
            isDebuggable = false
            isJniDebuggable = false
            if (configSigning) {
                signingConfig = signingConfigs.getByName("Sign4Release")
            }
            renderscriptOptimLevel = 3
            isMinifyEnabled = true
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // override field with current time in release builds only
            buildConfigField("long", "BUILD_TIMESTAMP", System.currentTimeMillis().toString())
        }
        // FOSS release
        create("foss") {
            // foss inherits from release build
            initWith(getByName("release"))
            // match the release build type for submodules
            matchingFallbacks += "release"
            // override field from inherited release config with zero for reproducible builds
            buildConfigField("long", "BUILD_TIMESTAMP", "0")
        }
    }
    compileOptions {
        // Flag to enable support for the new Java 8+ APIs
        isCoreLibraryDesugaringEnabled = true
        targetCompatibility = JavaVersion.VERSION_17
        sourceCompatibility = JavaVersion.VERSION_17
    }
    kotlin.jvmToolchain(17)
    packaging {
        // a resource file from kotlinx-coroutines that is only used by the debugger
        resources.excludes.add("DebugProbesKt.bin")
    }
    lint {
        checkReleaseBuilds = false
        // Or, if you prefer, you can continue to check for errors in release builds,
        // but continue the build even when errors are found:
        abortOnError = false
    }
    buildFeatures {
        buildConfig = true
        viewBinding = true
        compose = true
    }
    dynamicFeatures.add(":api_viewing")
    bundle {
        // disable split apks for languages to better support in-app language switching,
        // for language resources occupy a little space and implementing on-demand language downloads is tedious
        language.enableSplit = false
    }
}

dependencies {

    coreLibraryDesugaring(libs.androidDesugaring)

    implementation(platform(libs.androidxComposeBom))
    listOf(
        fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))),
//            libs.androidxWorkRuntime,
//            libs.androidxWorkFirebase,
        libs.androidxCore,
        libs.androidxCoreKtx,
        libs.androidxComposeRuntimeLiveData,
        libs.androidxComposeFoundation,
        libs.androidxComposeUi,
        libs.androidxComposeActivity,
        libs.androidxComposeMaterial3,
        libs.androidxComposeMaterialIcons,
        libs.androidxComposeMaterialIconsExtended,
        libs.androidxComposeAnimation,
        libs.androidxComposeUiToolingPreview,
        libs.androidxComposeViewModel,
        libs.androidxActivity,
        libs.androidxAppcompat,
        libs.androidxFragment,
        libs.androidxFragmentCompose,
        libs.androidxWindow,
        libs.androidxDrawerLayout,
        libs.androidxSwipeRefreshLayout,
        libs.androidxConstraintLayout,
        libs.androidxPalette,
        libs.androidxCardView,
        libs.androidxRecyclerView,
        libs.androidxViewPager,
        libs.androidxLifecycleRuntime,
        libs.androidxLifecycleRuntimeCompose,
        libs.androidxLifecycleCommon,
        libs.androidxLifecycleViewModel,
        libs.androidxLifecycleLiveData,
        libs.androidxPaging,
        libs.androidxPreference,
        libs.androidxNavigationFragment,
        libs.androidxNavigationUI,
        libs.androidxDocumentFile,
        libs.androidxHeifWriter,
        libs.googleMaterialComponents,
        libs.googleGson,
        libs.gglGuava,
        libs.kotlinStdlib,
        libs.kotlinCoroutines,
        libs.rxJava,
        libs.jbAnnotations,
        libs.okhttp,
        libs.coil,
        libs.coilCompose,
        libs.androidDeviceNames,
        libs.appIconLoader,
        libs.smoothCornerCompose,
        libs.haze,
        libs.aboutlibraries.core,
        libs.aboutlibraries.compose.m3,
    ).forEach { implementation(it) }

    debugImplementation(libs.androidxComposeUiTooling)

    listOf(libs.mockito, libs.googleTruth, libs.googleTruthExtensions, libs.junit4).forEach { testImplementation(it) }

}

// Generate universal APK from AAB that includes necessary dynamic modules (dist:module in manifests).
tasks.register("genUniversalApks") {
    tasks["printBundleToolVersion"].mustRunAfter("buildUniversalApks")
    dependsOn("buildUniversalApks", "printBundleToolVersion")
}

// Generate universal APK from AAB that includes necessary dynamic modules (dist:module in manifests).
tasks.register("genFossApks") {
    tasks["printBundleToolVersion"].mustRunAfter("buildFossApks")
    dependsOn("buildFossApks", "printBundleToolVersion")
}

tasks.register<JavaExec>("printBundleToolVersion") {
    val bundleTool = rootProject.file("doconfig/bundletool.jar")
    classpath = files(bundleTool)
    mainClass.set("com.android.tools.build.bundletool.BundleToolMain")
    args("version")
    doFirst { print("BundleTool ") }
}

tasks.register<JavaExec>("buildUniversalApks") {
    val bundleTool = rootProject.file("doconfig/bundletool.jar")
    classpath = files(bundleTool)
    mainClass.set("com.android.tools.build.bundletool.BundleToolMain")
    val customConfig = getCustomConfig(project)
    if (customConfig.signing != null) {
        customConfig.signing?.run {
            args(
                "build-apks",
                "--bundle", file("build/outputs/bundle/release/app-release.aab").absolutePath,
                "--output", file("build/outputs/app-universal-release.apks").absolutePath,
                "--ks", rootProject.file(store.path).absolutePath,
                "--ks-pass=pass:${store.password}",
                "--ks-key-alias", key.alias,
                "--key-pass=pass:${key.password}",
                "--overwrite",
                "--mode=universal",
            )
        }
    } else {
        args(
            "build-apks",
            "--bundle", file("build/outputs/bundle/release/app-release.aab").absolutePath,
            "--output", file("build/outputs/app-universal-release.apks").absolutePath,
            "--overwrite",
            "--mode=universal",
        )
    }

    dependsOn("bundleRelease")
}

tasks.register<JavaExec>("buildFossApks") {
    val bundleTool = rootProject.file("doconfig/bundletool.jar")
    classpath = files(bundleTool)
    mainClass.set("com.android.tools.build.bundletool.BundleToolMain")
    val customConfig = getCustomConfig(project)
    if (customConfig.signing != null) {
        customConfig.signing?.run {
            args(
                "build-apks",
                "--bundle", file("build/outputs/bundle/foss/app-foss.aab").absolutePath,
                "--output", file("build/outputs/app-universal-foss.apks").absolutePath,
                "--ks", rootProject.file(store.path).absolutePath,
                "--ks-pass=pass:${store.password}",
                "--ks-key-alias", key.alias,
                "--key-pass=pass:${key.password}",
                "--overwrite",
                "--mode=universal",
            )
        }
    } else {
        args(
            "build-apks",
            "--bundle", file("build/outputs/bundle/foss/app-foss.aab").absolutePath,
            "--output", file("build/outputs/app-universal-foss.apks").absolutePath,
            "--overwrite",
            "--mode=universal",
        )
    }

    dependsOn("bundleFoss")
}
