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
import com.madness.collision.unit.api_viewing.Utils
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.data.EasyAccess
import com.madness.collision.unit.api_viewing.data.VerInfo
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
import javax.security.cert.CertificateException
import javax.security.cert.X509Certificate
import com.madness.collision.unit.api_viewing.R as RAv

internal class AppListService(private val serviceContext: Context? = null) {
    private var regexFields: MutableMap<String, String> = HashMap()

    sealed class AppInfoItem(val text: String) {
        class Normal(text: String) : AppInfoItem(text)
        class Bold(text: String) : AppInfoItem(text)
    }

    private val String.item: AppInfoItem.Normal get() = AppInfoItem.Normal(this)
    private val Int.item: AppInfoItem.Normal get() = AppInfoItem.Normal((serviceContext ?: appContext).getString(this))
    private val String.boldItem: AppInfoItem.Bold get() = AppInfoItem.Bold(this)
    private val Int.boldItem: AppInfoItem.Bold get() = AppInfoItem.Bold((serviceContext ?: appContext).getString(this))

    private val LineBreakItem = "\n".item
    private suspend fun SequenceScope<AppInfoItem>.yield(text: String) = yield(text.item)
    private suspend fun SequenceScope<AppInfoItem>.yield(resId: Int) = yield(resId.item)
    private suspend fun SequenceScope<AppInfoItem>.yieldLineBreak() = yield(LineBreakItem)

    fun getRetrievedPkgInfo(context: Context, app: ApiViewingApp): PackageInfo? {
        return retrieveOn(context, app, 0, "")
    }

    fun getAppDetailsSequence(context: Context, appInfo: ApiViewingApp): Sequence<AppInfoItem> {
        val pkgInfo = getRetrievedPkgInfo(context, appInfo) ?: return emptySequence()
        return sequenceOf(
            getAppInfoDetailsSequence(context, appInfo, pkgInfo),
            sequenceOf(LineBreakItem, LineBreakItem),
            getAppExtendedDetailsSequence(context, appInfo, pkgInfo),
        ).flatten()
    }

    fun getAppInfoDetailsSequence(context: Context, appInfo: ApiViewingApp, pkgInfo: PackageInfo) = sequence<AppInfoItem> {
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
            val androidVer = if (ver.api == OsUtils.DEV) "Developer Preview" else ver.sdk
            if (androidVer.isEmpty()) return@sdkInfo ver.apiText
            val sdk = "Android $androidVer"
            val codeName = ver.codeName(context)
            val sdkDetails = if (codeName != ver.sdk) "$sdk, $codeName" else sdk
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

    fun getAppExtendedDetailsSequence(context: Context, appInfo: ApiViewingApp, pkgInfo: PackageInfo) = sequence<AppInfoItem> {
        var pi = pkgInfo
        var permissions: Array<String> = emptyArray()
        var activities: Array<ActivityInfo> = emptyArray()
        var receivers: Array<ActivityInfo> = emptyArray()
        var services: Array<ServiceInfo> = emptyArray()
        var providers: Array<ProviderInfo> = emptyArray()

        val flagGetDisabled = if (OsUtils.satisfy(OsUtils.N)) PackageManager.MATCH_DISABLED_COMPONENTS
        else flagGetDisabledLegacy
        val flagSignature = if (X.aboveOn(X.P)) PackageManager.GET_SIGNING_CERTIFICATES
        else getSigFlagLegacy
        val flags = PackageManager.GET_PERMISSIONS or PackageManager.GET_ACTIVITIES or
                PackageManager.GET_RECEIVERS or PackageManager.GET_SERVICES or
                PackageManager.GET_PROVIDERS or flagGetDisabled or flagSignature
        val reDetails = retrieveOn(context, appInfo, flags, "details")
        if (reDetails != null) {
            pi = reDetails
            permissions = pi.requestedPermissions ?: emptyArray()
            activities = pi.activities ?: emptyArray()
            receivers = pi.receivers ?: emptyArray()
            services = pi.services ?: emptyArray()
            providers = pi.providers ?: emptyArray()
        } else {
            retrieveOn(context, appInfo, PackageManager.GET_PERMISSIONS, "permissions")?.let {
                permissions = it.requestedPermissions ?: emptyArray()
            }
            retrieveOn(context, appInfo, PackageManager.GET_ACTIVITIES or flagGetDisabled, "activities")?.let {
                activities = it.activities ?: emptyArray()
            }
            retrieveOn(context, appInfo, PackageManager.GET_RECEIVERS or flagGetDisabled, "receivers")?.let {
                receivers = it.receivers ?: emptyArray()
            }
            retrieveOn(context, appInfo, PackageManager.GET_SERVICES or flagGetDisabled, "services")?.let {
                services = it.services ?: emptyArray()
            }
            retrieveOn(context, appInfo, flagSignature, "signing")?.let {
                pi = it
            }
        }

        var signatures: Array<Signature> = emptyArray()
        if (X.aboveOn(X.P)) {
            if (pi.signingInfo != null) {
                signatures = if (pi.signingInfo.hasMultipleSigners()) {
                    pi.signingInfo.apkContentsSigners
                } else {
                    pi.signingInfo.signingCertificateHistory
                }
            }
        } else {
            val piSignature = pi.sigLegacy
            if (piSignature != null) signatures = piSignature
        }
        if (regexFields.isEmpty()) {
            Utils.principalFields(context, regexFields)
        }
        for (s in signatures) {
            val cert: X509Certificate? = try {
                X509Certificate.getInstance(s.toByteArray())
            } catch (e: CertificateException) {
                e.printStackTrace()
                null
            }
            if (cert != null) {
                val issuerInfo = Utils.getDesc(regexFields, cert.issuerDN)
                val subjectInfo = Utils.getDesc(regexFields, cert.subjectDN)
                val formerPart = "X.509 " +
                        context.getString(RAv.string.apiDetailsCert) +
                        "\nNo." + cert.serialNumber.toString(16).uppercase(appLocale) +
                        " v${cert.version + 1}" +
                        '\n' + context.getString(RAv.string.apiDetailsValiSince)

                val format = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, appLocale)

                yield(formerPart.boldItem)
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

                yield(RAv.string.apiDetailsSigAlgOID.boldItem)
                yield(cert.sigAlgOID)
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

        appendCompSection(context, RAv.string.apiDetailsActivities, activities)
        appendCompSection(context, RAv.string.apiDetailsReceivers, receivers)
        appendCompSection(context, RAv.string.apiDetailsServices, services)
        appendCompSection(context, RAv.string.apiDetailsProviders, providers)
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

    private suspend fun SequenceScope<AppInfoItem>.appendSection(context: Context, titleId: Int) {
        yieldLineBreak()
        yieldLineBreak()
        yield(titleId.boldItem)
        yieldLineBreak()
    }

    private suspend fun SequenceScope<AppInfoItem>.appendComp(context: Context, components: Array<out ComponentInfo>) {
        if (components.isNotEmpty()) {
            Arrays.sort(components) { o1, o2 -> o1.name.compareTo(o2.name) }
            for (p in components) {
                yield(p.name)
                yieldLineBreak()
            }
        } else {
            yield(R.string.text_no_content)
            yieldLineBreak()
        }
    }

    private suspend fun SequenceScope<AppInfoItem>.appendCompSection(
            context: Context, titleId: Int, components: Array<out ComponentInfo>) {
        appendSection(context, titleId)
        appendComp(context, components)
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
            if (appInfo.isArchive) pm.getPackageArchiveInfo(appInfo.appPackage.basePath, extraFlags)
            else pm.getPackageInfo(appInfo.packageName, extraFlags)
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