package com.madness.collision.misc

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.content.edit
import com.madness.collision.util.P

object MiscApp {
    fun getPackageInfo(context: Context, packageName: String = "", apkPath: String = ""): PackageInfo?{
        val isArchive = packageName.isEmpty()
        val pm: PackageManager = context.packageManager
        return if (isArchive){
            if (apkPath.isEmpty()) return null
            pm.getPackageArchiveInfo(apkPath, 0)?.apply {
                applicationInfo.sourceDir = apkPath
                applicationInfo.publicSourceDir = apkPath
            }
        }else{
            if (packageName.isEmpty()) return null
            try {
                pm.getPackageInfo(packageName, 0)
            }catch (e: PackageManager.NameNotFoundException){
                e.printStackTrace()
                null
            }
        }
    }

    fun getApplicationInfo(context: Context, packageName: String = "", apkPath: String = ""): ApplicationInfo?{
        val isArchive = packageName.isEmpty()
        val pm: PackageManager = context.packageManager
        return if (isArchive){
            if (apkPath.isEmpty()) return null
            pm.getPackageArchiveInfo(apkPath, 0)?.applicationInfo?.apply {
                sourceDir = apkPath
                publicSourceDir = apkPath
            }
        }else{
            if (packageName.isEmpty()) return null
            try {
                pm.getApplicationInfo(packageName, 0)
            }catch (e: PackageManager.NameNotFoundException){
                e.printStackTrace()
                null
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getChangedPackageNames(context: Context): List<String>{
        val prefSettings = context.getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE)
        val bootCount = Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, 0)
        val sequenceNum = if (bootCount == prefSettings.getInt(P.PACKAGE_CHANGED_BOOT_COUNT, 0))
            prefSettings.getInt(P.PACKAGE_CHANGED_SEQUENCE_NO, 0)
        else 0
        val changedPackages = context.packageManager.getChangedPackages(sequenceNum)
        prefSettings.edit {
            putInt(P.PACKAGE_CHANGED_BOOT_COUNT, bootCount)
            putInt(P.PACKAGE_CHANGED_SEQUENCE_NO, changedPackages?.sequenceNumber ?: sequenceNum)
        }
        return changedPackages?.packageNames ?: emptyList()
    }

    fun getChangedPackages(context: Context, timestamp: Long = 0): List<PackageInfo> {
//        return if (X.aboveOn(X.O)) {
//            getChangedPackageNames(context).mapNotNull { getPackageInfo(context, it) }
//        } else {
//        }
        val shouldOverride = timestamp != 0L
        val prefSettings = if (shouldOverride) null else context.getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE)
        val finalTimestamp = if (shouldOverride) timestamp
        else prefSettings!!.getLong(P.PACKAGE_CHANGED_TIMESTAMP, System.currentTimeMillis())
        val re = context.packageManager.getInstalledPackages(0).filter {
            it.lastUpdateTime >= finalTimestamp
        }
        if (!shouldOverride) prefSettings!!.edit { putLong(P.PACKAGE_CHANGED_TIMESTAMP, System.currentTimeMillis()) }
        return re
    }

    fun getNewPackages(changedPackages: List<PackageInfo>): List<PackageInfo> {
        return changedPackages.filter {
            it.lastUpdateTime == it.firstInstallTime
        }
    }
}
