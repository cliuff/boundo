/*
 * Copyright 2021 Clifford Liu
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

package com.madness.collision.unit.api_viewing.list

import android.content.Context
import android.content.pm.*
import android.net.Uri
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.madness.collision.R
import com.madness.collision.misc.MiscApp
import com.madness.collision.misc.PackageCompat
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.data.EasyAccess
import com.madness.collision.unit.api_viewing.data.VerInfo
import com.madness.collision.unit.api_viewing.data.codenameOrNull
import com.madness.collision.unit.api_viewing.data.verNameOrNull
import com.madness.collision.unit.api_viewing.info.CertResolver
import com.madness.collision.util.*
import com.madness.collision.util.os.OsUtils
import com.madness.collision.util.ui.appContext
import com.madness.collision.util.ui.appLocale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.text.DateFormat
import java.util.*
import com.madness.collision.unit.api_viewing.R as RAv

internal class AppListService(private val serviceContext: Context? = null) {
    sealed class AppInfoItem(val text: String) {
        class Normal(text: String) : AppInfoItem(text)
        class Bold(text: String) : AppInfoItem(text)
    }

    private val String.item: AppInfoItem.Normal get() = AppInfoItem.Normal(this)
    private val Int.item: AppInfoItem.Normal get() = AppInfoItem.Normal((serviceContext ?: appContext).getString(this))
    private val String.boldItem: AppInfoItem.Bold get() = AppInfoItem.Bold(this)
    private val Int.boldItem: AppInfoItem.Bold get() = AppInfoItem.Bold((serviceContext ?: appContext).getString(this))

    private val LineBreakItem = "\n".item
    private fun MutableList<AppInfoItem>.yield(item: AppInfoItem) = add(item)
    private fun MutableList<AppInfoItem>.yield(text: String) = add(text.item)
    private fun MutableList<AppInfoItem>.yield(resId: Int) = add(resId.item)
    private fun MutableList<AppInfoItem>.yieldLineBreak() = add(LineBreakItem)

    fun getRetrievedPkgInfo(context: Context, app: ApiViewingApp): PackageInfo? {
        return retrieveOn(context, app, 0, "")
    }

    fun getAppDetailsSequence(context: Context, appInfo: ApiViewingApp): Sequence<AppInfoItem> {
        val pkgInfo = getRetrievedPkgInfo(context, appInfo) ?: return emptySequence()
        return sequenceOf(
            getAppInfoDetailsSequence(context, appInfo, pkgInfo).asSequence(),
            sequenceOf(LineBreakItem, LineBreakItem),
            getAppExtendedDetailsSequence(context, appInfo, pkgInfo).asSequence(),
        ).flatten()
    }

    // returns List instead of Sequence,
    // which has compatibility issues when running from IDE on lower APIs
    // (sequence {} builder uses coroutines internally to suspend yield() invocations)
    fun getAppInfoDetailsSequence(context: Context, appInfo: ApiViewingApp, pkgInfo: PackageInfo) = buildList<AppInfoItem> {
        val format = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, appLocale)

        yield(R.string.apiDetailsPackageName.boldItem)
        yield(appInfo.packageName)
        yieldLineBreak()

        yield(RAv.string.apiDetailsVerName.boldItem)
        yield(appInfo.verName)
        yieldLineBreak()

        yield(RAv.string.apiDetailsVerCode.boldItem)
        yield(appInfo.verCode.toString())
        yieldLineBreak()

        val sdkInfo = sdkInfo@ { ver: VerInfo ->
            val sdkDetails = when (ver.api) {
                OsUtils.DEV -> "Android Preview"
                else -> ver.verNameOrNull?.let { v ->
                    listOfNotNull("Android $v", ver.codenameOrNull(context)).joinToString()
                } ?: return@sdkInfo ver.apiText
            }
            ver.apiText + context.getString(R.string.textParentheses, sdkDetails)
        }

        if (appInfo.compileAPI >= OsUtils.A) {
            yield(RAv.string.av_list_info_compile_sdk.boldItem)
            yield(R.string.textColon.boldItem)
            yield(sdkInfo(VerInfo(appInfo.compileAPI, true)))
            yieldLineBreak()
        }

        if (appInfo.targetAPI >= OsUtils.A) {
            yield(R.string.apiSdkTarget.boldItem)
            yield(R.string.textColon.boldItem)
            yield(sdkInfo(VerInfo(appInfo.targetAPI, true)))
            yieldLineBreak()
        }

        if (appInfo.minAPI >= OsUtils.A) {
            yield(R.string.apiSdkMin.boldItem)
            yield(R.string.textColon.boldItem)
            yield(sdkInfo(VerInfo(appInfo.minAPI, true)))
            yieldLineBreak()
        }

        if (appInfo.isNotArchive) {
            val cal = Calendar.getInstance()

            cal.timeInMillis = pkgInfo.firstInstallTime
            yield(RAv.string.apiDetailsFirstInstall.boldItem)
            yield(format.format(cal.time))
            yieldLineBreak()

            cal.timeInMillis = pkgInfo.lastUpdateTime
            yield(RAv.string.apiDetailsLastUpdate.boldItem)
            yield(format.format(cal.time))
            yieldLineBreak()

            var installer: String? = null
            var realInstaller: String? = null
            var updateOwner: String? = null
            if (OsUtils.satisfy(OsUtils.R)) {
                val si = try {
                    context.packageManager.getInstallSourceInfo(appInfo.packageName)
                } catch (e: PackageManager.NameNotFoundException) {
                    e.printStackTrace()
                    null
                }
                if (si != null) {
                    installer = si.installingPackageName
                    // If the package that requested the install has been uninstalled,
                    // only this app's own install information can be retrieved
                    realInstaller = si.initiatingPackageName
                    // need the INSTALL_PACKAGES permission, which is granted to system apps only
                    val originating = si.originatingPackageName
                    if (originating != null) {
                        yield("Install originator: ".boldItem)
                        yield(originating)
                        yieldLineBreak()
                    }
                    if (OsUtils.satisfy(OsUtils.U)) {
                        updateOwner = si.updateOwnerPackageName
                    }
                }
            } else {
                installer = getInstallerLegacy(context, appInfo)
            }

            yield(RAv.string.apiDetailsInsatllFrom.boldItem)
            yield(getInstallerName(context, installer))
            yieldLineBreak()

            if (OsUtils.satisfy(OsUtils.R)) {
                yield(RAv.string.av_details_real_installer.boldItem)
                yield(getInstallerName(context, realInstaller))
                yieldLineBreak()
            }

            if (OsUtils.satisfy(OsUtils.U)) {
                yield("Update owner: ".boldItem)
                yield(if (updateOwner != null) getInstallerName(context, updateOwner) else "none")
                yieldLineBreak()
            }
        }

        if (!appInfo.isNativeLibrariesRetrieved) appInfo.retrieveNativeLibraries()
        val nls = appInfo.nativeLibraries
        val nlItem = arrayOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64", "Flutter", "React Native", "Xamarin", "Kotlin")
            .mapIndexed { i, s -> "$s " + (if (nls[i]) "✓" else "✗") }
            .joinToString(separator = "  ")

        yield(R.string.av_details_native_libs.boldItem)
        yield(nlItem)
        yieldLineBreak()
    }

    fun getAppExtendedDetailsSequence(context: Context, appInfo: ApiViewingApp, pkgInfo: PackageInfo) = buildList<AppInfoItem> {
        var pi = pkgInfo
        var permissions: Array<String> = emptyArray()

        val flagGetDisabled = when {
            OsUtils.satisfy(OsUtils.N) -> PackageManager.MATCH_DISABLED_COMPONENTS
            else -> flagGetDisabledLegacy
        }
        val flagSignature = when {
            OsUtils.satisfy(OsUtils.P) -> PackageManager.GET_SIGNING_CERTIFICATES
            else -> getSigFlagLegacy
        }
        val flags = PackageManager.GET_PERMISSIONS or flagGetDisabled or flagSignature
        val reDetails = retrieveOn(context, appInfo, flags, "details")
        if (reDetails != null) {
            pi = reDetails
            permissions = pi.requestedPermissions ?: emptyArray()
        } else {
            retrieveOn(context, appInfo, PackageManager.GET_PERMISSIONS, "permissions")?.let {
                permissions = it.requestedPermissions ?: emptyArray()
            }
            retrieveOn(context, appInfo, flagSignature, "signing")?.let {
                pi = it
            }
        }

        val signatures = when {
            OsUtils.satisfy(OsUtils.P) -> when (pi.signingInfo?.hasMultipleSigners()) {
                true -> pi.signingInfo.apkContentsSigners
                false -> pi.signingInfo.signingCertificateHistory
                null -> pi.sigLegacy.orEmpty()
            }
            else -> pi.sigLegacy.orEmpty()
        }
        for ((signatureIndex, signature) in signatures.withIndex()) {
            CertResolver.getCertificateInfo(signature, context)?.run {
                val (issuerInfo, subjectInfo) = listOf(issuerValues, subjectValues).map { values ->
                    values.joinToString(separator = "\n", prefix = "\n") { (k, v, n) ->
                        if (n != null) "$n ($k): $v" else "$k: $v"
                    }
                }
                val certName = cert.type + " " + context.getString(RAv.string.apiDetailsCert)
                val serialNo = cert.serialNumber.toString(16).uppercase(appLocale)
                val formerPart = "$certName v${cert.version}\nNo.$serialNo"
                val certFingerprint = listOf("MD5", "SHA-1", "SHA-256").zip(fingerprint.toList())
                    .joinToString(separator = "\n", prefix = "\n") { (alg, fp) -> "[$alg]  $fp" }

                val format = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, appLocale)

                if (signatureIndex >= 1) yieldLineBreak()
                yield(formerPart.boldItem)
                yieldLineBreak()

                yield(RAv.string.apiDetailsValiSince.boldItem)
                yield(format.format(cert.notBefore))
                yieldLineBreak()

                yield(RAv.string.apiDetailsValiUntil.boldItem)
                yield(format.format(cert.notAfter))
                yieldLineBreak()

                yield(RAv.string.apiDetailsIssuer.boldItem)
                yield(issuerInfo)
                yieldLineBreak()

                yield(RAv.string.apiDetailsSubject.boldItem)
                yield(subjectInfo)
                yieldLineBreak()

                yield(RAv.string.apiDetailsSigAlg.boldItem)
                yield(cert.sigAlgName)
                yieldLineBreak()

                yield("Fingerprint:".boldItem)
                yield(certFingerprint)
                yieldLineBreak()
            }
        }

        appendSection(context, RAv.string.apiDetailsPermissions)
        if (permissions.isNotEmpty()) {
            Arrays.sort(permissions)
            for (permission in permissions) {
                yield(permission)
                yieldLineBreak()
            }
        } else {
            yield(R.string.text_no_content)
            yieldLineBreak()
        }
    }

    fun getInstallerName(context: Context, installer: String?): String {
        return if (installer != null) {
            val msg = "av.info.x" to "Installer not found: $installer"
            val installerName = MiscApp.getApplicationInfo(context, packageName = installer, errorMsg = msg)
                    ?.loadLabel(context.packageManager)?.toString() ?: ""
            if (installerName.isNotEmpty()) {
                installerName
            } else {
                val installerAndroid = ApiViewingApp.packagePackageInstaller
                val installerGPlay = ApiViewingApp.packagePlayStore
                when (installer) {
                    installerGPlay -> context.getString(RAv.string.apiDetailsInstallGP)
                    installerAndroid -> context.getString(RAv.string.apiDetailsInstallPI)
                    "null" -> context.getString(RAv.string.apiDetailsInstallUnknown)
                    else -> installer
                }
            }
        } else {
            context.getString(RAv.string.apiDetailsInstallUnknown)
        }
    }

    private fun MutableList<AppInfoItem>.appendSection(context: Context, titleId: Int) {
        yieldLineBreak()
        yieldLineBreak()
        yield(titleId.boldItem)
        yieldLineBreak()
    }

    @Suppress("deprecation")
    private fun getInstallerLegacy(context: Context, app: ApiViewingApp): String? {
        return try {
            context.packageManager.getInstallerPackageName(app.packageName)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            null
        }
    }

    @Suppress("deprecation")
    private val getSigFlagLegacy = PackageManager.GET_SIGNATURES

    @Suppress("deprecation")
    private val flagGetDisabledLegacy = PackageManager.GET_DISABLED_COMPONENTS

    @Suppress("deprecation")
    private val PackageInfo.sigLegacy: Array<Signature>?
        get() = signatures

    private fun retrieveOn(context: Context, appInfo: ApiViewingApp, extraFlags: Int, subject: String): PackageInfo? {
        return try {
            val pm = context.packageManager
            when {
                appInfo.isArchive -> PackageCompat.getArchivePackage(pm, appInfo.appPackage.basePath, extraFlags)
                else -> PackageCompat.getInstalledPackage(pm, appInfo.packageName, extraFlags)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("APIAdapter", String.format("failed to retrieve %s of %s", subject, appInfo.packageName))
            null
        }
    }

    fun loadAppIcons(fragment: Fragment, appList: AppList, refreshLayout: SwipeRefreshLayout? = null)
    = fragment.lifecycleScope.launch(Dispatchers.Default) {
        val adapter = appList.getAdapter()
        val viewModel: AppListViewModel by fragment.viewModels()
        try {
            for (index in viewModel.apps4DisplayValue.indices) {
                if (index >= EasyAccess.preloadLimit) break
                if (index >= viewModel.apps4DisplayValue.size) break
                if (refreshLayout == null) {
                    adapter.ensureItem(index)
                    continue
                }
                val shouldCeaseRefresh = (index >= EasyAccess.loadAmount - 1)
                        || (index >= adapter.listCount - 1)
                val doCeaseRefresh = shouldCeaseRefresh && refreshLayout.isRefreshing
                if (doCeaseRefresh) withContext(Dispatchers.Main) {
                    refreshLayout.isRefreshing = false
                }
                if (adapter.ensureItem(index)) continue
                if (!doCeaseRefresh) continue
                withContext(Dispatchers.Main) {
                    refreshLayout.isRefreshing = false
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun actionIcon(context: Context, app: ApiViewingApp, fragmentManager: FragmentManager) {
        val path = F.createPath(F.cachePublicPath(context), "App", "Logo", "${app.name}.png")
        val image = File(path)
        app.getOriginalIcon(context)?.let {
            if (F.prepare4(image)) X.savePNG(it, path)
        }
        val uri: Uri = image.getProviderUri(context)
//        val previewTitle = app.name // todo set preview title
        fragmentManager.let {
            FilePop.by(context, uri, "image/png", R.string.textShareImage, uri, app.name).show(it, FilePop.TAG)
        }
    }

    // todo split APKs
    fun actionApk(context: Context, app: ApiViewingApp, scope: CoroutineScope, fragmentManager: FragmentManager) {
        val path = F.createPath(F.cachePublicPath(context), "App", "APK", "${app.name}-${app.verName}.apk")
        val apk = File(path)
        if (F.prepare4(apk)) {
            scope.launch(Dispatchers.Default) {
                try {
                    X.copyFileLessTwoGB(File(app.appPackage.basePath), apk)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        val uri: Uri = apk.getProviderUri(context)
        val previewTitle = "${app.name} ${app.verName}"
//        val flag = Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        val previewPath = F.createPath(F.cachePublicPath(context), "App", "Logo", "${app.name}.png")
        val image = File(previewPath)
        val appIcon = app.getOriginalIcon(context)
        if (appIcon != null && F.prepare4(image)) X.savePNG(appIcon, previewPath)
        val imageUri = image.getProviderUri(context)
        fragmentManager.let {
            val fileType = "application/vnd.android.package-archive"
            FilePop.by(context, uri, fileType, R.string.textShareApk, imageUri, previewTitle).show(it, FilePop.TAG)
        }
    }
}