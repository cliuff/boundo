/*
 * Copyright 2022 Clifford Liu
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
package com.madness.collision.util.ui

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.UserHandle
import android.os.UserManager
import android.util.ArrayMap
import androidx.annotation.Px
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.os.UserHandleCompat
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.key.Keyer
import coil3.request.Options
import me.zhanghai.android.appiconloader.iconloaderlib.BaseIconFactory
import me.zhanghai.android.appiconloader.iconloaderlib.BitmapInfo
import java.util.concurrent.ConcurrentLinkedQueue

private object UserSerialNumberCache {
    private const val CACHE_MILLIS: Long = 1000
    private val sCache = ArrayMap<UserHandle, LongArray>()

    fun getSerialNumber(user: UserHandle, context: Context): Long {
        synchronized(sCache) {
            val serialNoWithTime = sCache[user] ?: LongArray(2).also { sCache[user] = it }
            val time = System.currentTimeMillis()
            if (serialNoWithTime[1] + CACHE_MILLIS <= time) {
                serialNoWithTime[0] = user.getSerialNo(context)
                serialNoWithTime[1] = time
            }
            return serialNoWithTime[0]
        }
    }

    private fun UserHandle.getSerialNo(context: Context): Long {
        val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
        return userManager.getSerialNumberForUser(this)
    }
}

private class IconFactory(
    @Px iconBitmapSize: Int, context: Context,
    dpi: Int = context.resources.configuration.densityDpi
) : BaseIconFactory(context, dpi, iconBitmapSize, true) {
    private val transformer = AppIconTransformer()

    fun createBadgedIconBitmap(
        icon: Drawable, user: UserHandle?, shrinkNonAdaptiveIcons: Boolean, isInstantApp: Boolean): BitmapInfo {
        val ic = transformer.applySrc(icon)
        return super.createBadgedIconBitmap(ic, user, shrinkNonAdaptiveIcons, isInstantApp, null)
    }

    override fun createIconBitmap(icon: Drawable, scale: Float, size: Int): Bitmap {
        return transformer.apply(icon, scale, size) { super.createIconBitmap(icon, scale, size) }
    }
}

class AppIconLoader(
    @Px private val iconSize: Int,
    private val shrinkNonAdaptiveIcons: Boolean,
    private val context: Context) {
    private val mIconFactoryPool = ConcurrentLinkedQueue<IconFactory>()

    fun loadIcon(applicationInfo: ApplicationInfo, isInstantApp: Boolean = false): Bitmap {
        val unbadgedIcon = applicationInfo.loadUnbadgedIcon(context.packageManager)
        val user = UserHandleCompat.getUserHandleForUid(applicationInfo.uid)
        // poll-use-offer strategy to ensure iconFactory is thread-safe
        val iconFactory = mIconFactoryPool.poll() ?: IconFactory(iconSize, context)
        try {
            return iconFactory.createBadgedIconBitmap(unbadgedIcon, user, shrinkNonAdaptiveIcons, isInstantApp).icon
        } finally {
            mIconFactoryPool.offer(iconFactory)
        }
    }

    companion object {
        fun getIconKey(applicationInfo: ApplicationInfo, versionCode: Long, context: Context): String {
            val user = UserHandleCompat.getUserHandleForUid(applicationInfo.uid)
            val serialNo = UserSerialNumberCache.getSerialNumber(user, context)
            return applicationInfo.packageName + ":" + versionCode + ":" + serialNo
        }

        fun getIconKey(packageInfo: PackageInfo, context: Context): String {
            return getIconKey(packageInfo.applicationInfo, packageInfo.verCode, context)
        }
    }
}

class AppIconFetcher(private val iconLoader: AppIconLoader, private val data: PackageInfo, private val options: Options) : Fetcher {

    class Factory(@Px iconSize: Int, shrinkNonAdaptiveIcons: Boolean, context: Context)
        : Fetcher.Factory<PackageInfo> {
        private val iconLoader = AppIconLoader(iconSize, shrinkNonAdaptiveIcons, context.applicationContext)

        override fun create(data: PackageInfo, options: Options, imageLoader: ImageLoader): Fetcher {
            return AppIconFetcher(iconLoader, data, options)
        }
    }

    override suspend fun fetch(): FetchResult {
        val icon = iconLoader.loadIcon(data.applicationInfo)
        return ImageFetchResult(icon.asImage(), true, DataSource.DISK)
    }
}

class AppIconKeyer(context: Context) : Keyer<PackageInfo> {
    private val context = context.applicationContext

    override fun key(data: PackageInfo, options: Options): String? {
        if (data.handleable.not()) return null
        return AppIconLoader.getIconKey(data, context)
    }
}

interface PackageInfo {
    val handleable: Boolean
    val verCode: Long
    val applicationInfo: ApplicationInfo
}

interface ApplicationInfo {
    val uid: Int
    val packageName: String
    fun loadUnbadgedIcon(pm: PackageManager): Drawable
}

interface CompactPackageInfo : PackageInfo, ApplicationInfo {
    override val applicationInfo: ApplicationInfo get() = this
}

class AppIconPackageInfo(
    pack: android.content.pm.PackageInfo,
    private val app: android.content.pm.ApplicationInfo): CompactPackageInfo {
    override val handleable: Boolean = true
    override val verCode: Long = PackageInfoCompat.getLongVersionCode(pack)
    override val uid: Int = app.uid
    override val packageName: String = app.packageName
    override fun loadUnbadgedIcon(pm: PackageManager): Drawable = app.loadUnbadgedIcon(pm)
}
