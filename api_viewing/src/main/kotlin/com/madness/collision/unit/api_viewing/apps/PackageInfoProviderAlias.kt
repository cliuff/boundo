package com.madness.collision.unit.api_viewing.apps

import com.madness.collision.chief.chiefContext
import io.cliuff.boundo.data.AppListPermission
import io.cliuff.boundo.data.PackageInfoProvider
import io.cliuff.boundo.data.PlatformAppProvider
import io.cliuff.boundo.data.PlatformAppsFetcher
import io.cliuff.boundo.data.ShellAppsFetcher

@Deprecated(
    "Use AppListPermission instead",
    ReplaceWith("AppListPermission", "io.cliuff.boundo.data.AppListPermission")
)
typealias AppListPermission = AppListPermission

@Deprecated(
    "Use PackageInfoProvider instead",
    ReplaceWith("PackageInfoProvider", "io.cliuff.boundo.data.PackageInfoProvider")
)
typealias PackageInfoProvider = PackageInfoProvider

@Deprecated(
    "Use PlatformAppProvider instead",
    ReplaceWith("PlatformAppProvider", "io.cliuff.boundo.data.PlatformAppProvider")
)
typealias PlatformAppProvider = PlatformAppProvider


@Deprecated("Use AppListPermission.getInstalledAppsPkg instead")
val AppListPermission.GetInstalledAppsPkg: String?
    get() = getInstalledAppsPkg(chiefContext)

@Suppress("StaticFieldLeak")
@Deprecated("Use PlatformAppsFetcher instead", ReplaceWith("PlatformAppsFetcher()"))
val PlatformAppsFetcher = PlatformAppsFetcher(chiefContext)

@Suppress("StaticFieldLeak")
@Deprecated("Use ShellAppsFetcher instead", ReplaceWith("ShellAppsFetcher()"))
val ShellAppsFetcher = ShellAppsFetcher(chiefContext)
