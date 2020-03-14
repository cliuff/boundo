package com.madness.collision.wearable.av

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import com.madness.collision.wearable.av.data.ApiViewingApp
import com.madness.collision.wearable.av.data.EasyAccess

internal class AppRepository(){

    fun getUserApps(context: Context): List<ApiViewingApp>{
        return getAppsUser(context)
//        return dao.getUserApps().value ?: emptyList()
    }

    fun getSystemApps(context: Context): List<ApiViewingApp>{
        return getAppsSys(context)
//        return dao.getSystemApps().value ?: emptyList()
    }

    fun getAllApps(context: Context): List<ApiViewingApp>{
        return getAppsAll(context)
//        return dao.getAllApps().value ?: emptyList()
    }

    private fun getApps(context: Context, predicate: ((PackageInfo) -> Boolean)? = null): List<ApiViewingApp>{
        return context.packageManager.getInstalledPackages(0).let {
            if (predicate == null) it else it.filter(predicate)
        }.map { ApiViewingApp(context, it, preloadProcess = true) }
    }

    private fun getAppsUser(context: Context): List<ApiViewingApp> {
        val predicate: (PackageInfo) -> Boolean = if (EasyAccess.shouldIncludeDisabled) { packageInfo ->
            (packageInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0
        } else { packageInfo ->
            val app = packageInfo.applicationInfo
            // users without root privilege can only disable system apps
            (app.flags and ApplicationInfo.FLAG_SYSTEM) == 0 && app.enabled
        }
        return getApps(context, predicate)
    }

    private fun getAppsSys(context: Context): List<ApiViewingApp> {
        val predicate: (PackageInfo) -> Boolean = if (EasyAccess.shouldIncludeDisabled) { packageInfo ->
            (packageInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        } else { packageInfo ->
            val app = packageInfo.applicationInfo
            (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0 && app.enabled
        }
        return getApps(context, predicate)
    }

    private fun getAppsAll(context: Context): List<ApiViewingApp> {
        val predicate: ((PackageInfo) -> Boolean)? = if (EasyAccess.shouldIncludeDisabled) null
        else { packageInfo ->
            packageInfo.applicationInfo.enabled
        }
        return getApps(context, predicate)
    }
}
