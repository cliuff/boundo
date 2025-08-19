package com.madness.collision.main

import com.madness.collision.util.ui.AppIconTransformer
import io.cliuff.boundo.conf.CoilInitializer
import io.cliuff.boundo.conf.coil.DefaultAppIconTransformer

@Deprecated("Refactor references to dedicated package instead")
internal val CoilInitializer = run {
    DefaultAppIconTransformer.value = AppIconTransformer()
    CoilInitializer
}
