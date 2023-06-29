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

package com.madness.collision.unit.api_viewing

import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.content.edit
import androidx.core.content.pm.PackageInfoCompat
import androidx.lifecycle.LifecycleOwner
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.database.AppDao
import com.madness.collision.unit.api_viewing.database.DataMaintainer
import com.madness.collision.unit.api_viewing.database.RecordMaintainer
import com.madness.collision.unit.api_viewing.database.maintainer.RecordMtn
import com.madness.collision.unit.api_viewing.origin.PackageRetriever
import com.madness.collision.util.X
import com.madness.collision.util.X.A
import com.madness.collision.util.X.B
import com.madness.collision.util.X.C
import com.madness.collision.util.X.D
import com.madness.collision.util.X.E
import com.madness.collision.util.X.E_0_1
import com.madness.collision.util.X.E_MR1
import com.madness.collision.util.X.F
import com.madness.collision.util.X.G
import com.madness.collision.util.X.G_MR1
import com.madness.collision.util.X.H
import com.madness.collision.util.X.H_MR1
import com.madness.collision.util.X.H_MR2
import com.madness.collision.util.X.I
import com.madness.collision.util.X.I_MR1
import com.madness.collision.util.X.J
import com.madness.collision.util.X.J_MR1
import com.madness.collision.util.X.J_MR2
import com.madness.collision.util.X.K
import com.madness.collision.util.X.K_WATCH
import com.madness.collision.util.X.L
import com.madness.collision.util.X.L_MR1
import com.madness.collision.util.X.M
import com.madness.collision.util.X.N
import com.madness.collision.util.X.N_MR1
import com.madness.collision.util.X.O
import com.madness.collision.util.X.O_MR1
import com.madness.collision.util.X.P
import com.madness.collision.util.X.Q
import com.madness.collision.util.os.OsUtils
import com.madness.collision.util.regexOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.security.Principal
import java.util.regex.Matcher
import java.util.regex.Pattern

internal object Utils {

    fun getAndroidVersionByAPI(api: Int, exact: Boolean, isCompact: Boolean = false): String {
        return when (api) {
            OsUtils.U -> "14"
            OsUtils.T -> "13"
            OsUtils.S_V2 -> if (exact) "12L" else "12"
            OsUtils.S -> "12"
            X.R -> "11"
            Q -> "10"
            P -> "9"  // 9 Pie
            O_MR1 -> if (exact) "8.1" else "8"  // 8.1.0 Oreo
            O -> if (exact) "8.0" else "8"  // 8.0.0 Oreo
            N_MR1-> if (exact) "7.1" else "7"  // 7.1 Nougat
            N -> if (exact) "7.0" else "7"  // 7.0 Nougat
            M -> if (exact) "6.0" else "6"  // 6.0 Marshmallow
            L_MR1 -> if (exact) "5.1" else "5"  // 5.1 Lollipop
            L -> if (exact) "5.0" else "5"  // 5.0 Lollipop
            K_WATCH -> if (exact) "4.4W" else "4"  // 4.4W KitKat
            K -> if (isCompact) "4.4" else if (exact) "4.4 - 4.4.4" else "4"  // 4.4 - 4.4.4 KitKat
            J_MR2 -> if (isCompact) "4.3" else if (exact) "4.3.x" else "4"  // 4.3.x Jelly Bean
            J_MR1 -> if (isCompact) "4.2" else if (exact) "4.2.x" else "4"  // 4.2.x Jelly Bean
            J -> if (isCompact) "4.1" else if (exact) "4.1.x" else "4"  // 4.1.x Jelly Bean
            I_MR1 -> if (isCompact) "4.0" else if (exact) "4.0.3 - 4.0.4" else "4"  // 4.0.3 - 4.0.4 Ice Cream Sandwich
            I -> if (isCompact) "4.0" else if (exact) "4.0.1 - 4.0.2" else "4"  // 4.0.1 - 4.0.2 Ice Cream Sandwich
            H_MR2 -> if (isCompact) "3.2" else if (exact) "3.2.x" else "3"  // 3.2.x Honeycomb
            H_MR1 -> if (exact) "3.1" else "3"  // 3.1 Honeycomb
            H -> if (exact) "3.0" else "3"  // 3.0 Honeycomb
            G_MR1 -> if (isCompact) "2.3" else if (exact)  "2.3.3 - 2.3.7" else "2"  // 2.3.3 - 2.3.7 Gingerbread
            G -> if (isCompact) "2.3" else if (exact) "2.3 - 2.3.2" else "2"  // 2.3 - 2.3.2 Gingerbread
            F -> if (isCompact) "2.2" else if (exact) "2.2.x" else "2"  // 2.2.x Froyo
            E_MR1 -> if (exact) "2.1" else "2"  // 2.1 Eclair
            E_0_1 -> if (exact) "2.0.1" else "2"  // 2.0.1 Eclair
            E -> if (exact) "2.0" else "2"  // 2.0 Eclair
            D -> if (exact) "1.6" else "1"  // 1.6 Donut
            C -> if (exact) "1.5" else "1"  // 1.5 Cupcake
            B -> if (exact) "1.1" else "1"  // 1.1 null
            A -> if (exact) "1.0" else "1"  // 1.0 null
            else -> ""
        }
    }

    fun getAndroidLetterByAPI(apiLevel: Int): Char{
        return androidCodenameInfo(null, apiLevel, false)[0]
    }

    fun getAndroidCodenameByAPI( context: Context, api: Int): String{
        return androidCodenameInfo(context, api, true)
    }

    fun getDevCodenameLetter(): Char? {
        return when (Build.VERSION.CODENAME) {
            "UpsideDownCake", "UpsideDownCakePrivacySandbox" -> 'u'
            "Tiramisu", "TiramisuPrivacySandbox" -> 't'
            "REL" -> null
            else -> null
        }
    }

    private fun androidCodenameInfo(context: Context?, apiLevel: Int, fullName: Boolean): String {
        if (fullName) context ?: return " "
        val getString: (id: Int) -> String = {
            context?.getString(it) ?: " "
        }
        return when (apiLevel) {
            OsUtils.U -> if (fullName) "Upside Down Cake" else "u"
            OsUtils.T -> if (fullName) "Tiramisu" else "t"
            OsUtils.S_V2 -> if (fullName) "12L" else "s"
            OsUtils.S -> if (fullName) "12" else "s"
            X.R -> if (fullName) "11" else "r"
            Q -> if (fullName) "10" else "q"
            P -> if (fullName) getString(R.string.res_api_code_names_p) else "p"  // Pie
            O, O_MR1 -> if (fullName) getString(R.string.res_api_code_names_o) else "o"  // Oreo
            N, N_MR1 -> if (fullName) getString(R.string.res_api_code_names_n) else "n"  // Nougat
            M -> if (fullName) getString(R.string.res_api_code_names_m) else "m"  // Marshmallow
            L, L_MR1 -> if (fullName) getString(R.string.res_api_code_names_l) else "l"  // Lollipop
            K, K_WATCH -> if (fullName) getString(R.string.res_api_code_names_k) else "k"  // KitKat
            J, J_MR1, J_MR2 -> if (fullName) getString(R.string.res_api_code_names_j) else "j"  // Jelly Bean
            I, I_MR1 -> if (fullName) getString(R.string.res_api_code_names_i) else "i"  // Ice Cream Sandwich
            H, H_MR1, H_MR2 -> if (fullName) getString(R.string.res_api_code_names_h) else "h"  // Honeycomb
            G, G_MR1 -> if (fullName) getString(R.string.res_api_code_names_g) else "g"  // Gingerbread
            F -> if (fullName) getString(R.string.res_api_code_names_f) else "f"  // Froyo
            E, E_0_1, E_MR1 -> if (fullName) getString(R.string.res_api_code_names_e) else "e"  // Eclair
            D -> if (fullName) getString(R.string.res_api_code_names_d) else "d"  // Donut
            C -> if (fullName) getString(R.string.res_api_code_names_c) else "c"  // Cupcake
            B -> if (fullName) getString(R.string.resApiCodeNamesB) else " "  // from wikipedia
//            1 -> if (fullName) "Base" else " " // from Build.VERSION_CODES.BASE
            else -> if (fullName) "" else " "
        }
    }

    fun principalFields( context: Context, regexFields: MutableMap<String, String> ){
        regexFields["^(CN)(=.*)"] = context.getString(R.string.resPrincipalCN)
        regexFields["^(OU)(=.*)"] = context.getString(R.string.resPrincipalOU)
        regexFields["^(O)(=.*)"] = context.getString(R.string.resPrincipalO)
        regexFields["^(L)(=.*)"] = context.getString(R.string.resPrincipalL)
        regexFields["^(ST)(=.*)"] = context.getString(R.string.resPrincipalST)
        regexFields["^(C)(=.*)"] = context.getString(R.string.resPrincipalC)
        regexFields["^(EMAILADDRESS)(=.*)"] = context.getString(R.string.resPrincipalEmail)
    }

    fun getDesc(regexFields: Map<String, String> ,  p: Principal): String{
        val slices = p.toString().split(", ")
        val keys = regexFields.keys.toTypedArray()
        val builder = StringBuilder()
        var matcher : Matcher
        var matched = false
        for (s in slices){
            for (j in 0 until regexFields.size) {
                if (keys[j].isEmpty()) continue
                matcher = Pattern.compile(keys[j]).matcher(s)
                if (matcher.matches()){
                    matched = true
                    builder.append(matcher.group(1))
                            .append('(')
                            .append(regexFields[keys[j]])
                            .append(')')
                            .append(matcher.group(2))
                            .append(", ")
                    keys[j] = ""
                    break
                }
            }
            if (!matched) builder.append(s).append(", ")
            matched = false
        }
        return builder.delete(builder.length-2, builder.length-1).toString()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getChangedPackageNames(context: Context): List<String> {
        val pref = com.madness.collision.util.P
        val prefSettings = context.getSharedPreferences(pref.PREF_SETTINGS, Context.MODE_PRIVATE)
        val bootCount = Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, 0)
        val sequenceNum = if (bootCount == prefSettings.getInt(pref.PACKAGE_CHANGED_BOOT_COUNT, 0))
            prefSettings.getInt(pref.PACKAGE_CHANGED_SEQUENCE_NO, 0)
        else 0
        val changedPackages = context.packageManager.getChangedPackages(sequenceNum)
        prefSettings.edit {
            putInt(pref.PACKAGE_CHANGED_BOOT_COUNT, bootCount)
            putInt(pref.PACKAGE_CHANGED_SEQUENCE_NO, changedPackages?.sequenceNumber ?: sequenceNum)
        }
        changedPackages ?: return emptyList()
        return changedPackages.packageNames
    }

    fun getChangedPackages(context: Context, lifecycleOwner: LifecycleOwner, timestamp: Long = 0)
    : Pair<List<ApiViewingApp>?, List<PackageInfo>> {
//        return if (X.aboveOn(X.O)) {
//            getChangedPackageNames(context).mapNotNull { getPackageInfo(context, it) }
//        } else {
//        }
        val shouldOverride = timestamp != 0L
        val pref = com.madness.collision.util.P
        val prefSettings = if (shouldOverride) null
        else context.getSharedPreferences(pref.PREF_SETTINGS, Context.MODE_PRIVATE)
        val finalTimestamp = if (shouldOverride) timestamp
        else prefSettings!!.getLong(pref.PACKAGE_CHANGED_TIMESTAMP, System.currentTimeMillis())

        val retriever = PackageRetriever(context)
        // get latest installed packages
        val allPackages = retriever.all
        // get changed packages
        val dao = DataMaintainer.get(context, lifecycleOwner)
        val re = getUpdatedPackages(allPackages, finalTimestamp, dao)
        // get past records
        val previous = when (dao.selectCount()) {
            null -> null
            0 -> emptyList()
            else -> dao.selectApps(re.map { it.packageName })
        }
        // update records
        RecordMaintainer(context, retriever, lifecycleOwner, dao).run {
            update(allPackages)
            checkRemoval(allPackages)
        }
        // Maintainer diff
        if (RecordMtn.shouldDiff(context)) {
            // run asynchronously
            CoroutineScope(Dispatchers.Default).launch {
                val dataDiff = RecordMtn.diff(context, allPackages, dao.selectAllApps())
                RecordMtn.apply(context, dataDiff, allPackages, dao)
            }
        }

        if (!shouldOverride) prefSettings!!.edit {
            putLong(pref.PACKAGE_CHANGED_TIMESTAMP, System.currentTimeMillis())
        }
        return previous to re
    }

    fun getNewPackages(changedPackages: List<PackageInfo>): List<PackageInfo> {
        return changedPackages.filter {
            it.lastUpdateTime == it.firstInstallTime
        }
    }

    private fun getUpdatedPackages(allPackages: List<PackageInfo>, timestamp: Long, dao: AppDao): List<PackageInfo> {
        val preinstalledPackages = ArrayList<PackageInfo>()
        val updatedPackages = ArrayList<PackageInfo>()
        for (info in allPackages) {
            when {
                // 0->1970.01.01, 1230768000000->2009.01.01
                info.lastUpdateTime <= 1230768000000L -> preinstalledPackages.add(info)
                info.lastUpdateTime >= timestamp -> updatedPackages.add(info)
                else -> Unit
            }
        }
        if (preinstalledPackages.isNotEmpty()) {
            val preinstalledRecords = dao.selectApps(preinstalledPackages.map { it.packageName })
                .associateBy { it.packageName }
            for (info in preinstalledPackages) {
                val record = preinstalledRecords[info.packageName] ?: continue
                val app = info.applicationInfo
                val isChanged = when {
                    PackageInfoCompat.getLongVersionCode(info) != record.verCode -> true
                    info.versionName != record.verName -> true
                    // apex has version in path
                    app.publicSourceDir.orEmpty() != record.appPackage.basePath -> true
                    else -> false
                }
                if (isChanged) updatedPackages.add(info)
            }
        }
        return updatedPackages
    }

    /**
     * Check if the [text] contains any known store link
     */
    fun checkStoreLink(text: String): String? {
        // this regex cannot match Android OS whose package is android
        val packageRegex = regexOf("([A-Za-z][A-Za-z\\d_]*\\.)+[A-Za-z][A-Za-z\\d_]*")
        val prefix = regexOf(".*(?:https?://)?(?:www\\.)?")
        val links = listOf(
                regexOf("play\\.google\\.com/store/apps/details\\?id="), // Google Play store
                regexOf("coolapk\\.com/apk/"), // Coolapk
                regexOf("appgallery\\.cloud\\.huawei\\.com/.*shareTo="), // Huawei
                regexOf("apps\\.galaxyappstore\\.com/detail/"), // Samsung
                regexOf("""apps\.samsung\.com/appquery/appDetail\.as\?appId="""),
        )
        for (link in links) {
            val realLink = "$prefix$link($packageRegex).*"
            val re = realLink.toRegex().find(text) ?: continue
            val (targetPackage) = re.destructured
            return targetPackage
        }
        return null
    }
}
