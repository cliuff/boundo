package com.madness.collision.util

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class ManifestUtilTest {

    @BeforeEach
    fun setUp() {
    }

    @AfterEach
    fun tearDown() {
    }

    @Test
    fun getIntents() {
        val resID = ManifestUtil.getManifestAttr("C:\\00a\\ProjectsAndroid\\Backups\\BandReleases\\Band1-1.apk", arrayOf("uses-sdk", "minSdkVersion"))
        println(resID.toInt().toString())
    }
}