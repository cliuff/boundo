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

package com.madness.collision.unit.api_viewing.tag

import android.content.Context
import android.content.pm.PackageInfo
import android.util.Log
import com.madness.collision.unit.api_viewing.AppTag
import com.madness.collision.unit.api_viewing.data.ApiUnit
import com.madness.collision.unit.api_viewing.data.ApiViewingApp

internal class TagRelation(val value: Int) {
    val isNone = value == RELATION_NONE
    val isComplimentary = value == RELATION_COMPLIMENTARY
    val isIntersect = value == RELATION_INTERSECT
    val isSame = value == RELATION_SAME
    val isOpposite = value == RELATION_OPPOSITE

    companion object {
        const val RELATION_NONE = 0
        const val RELATION_COMPLIMENTARY = 2
        const val RELATION_INTERSECT = 3
        const val RELATION_SAME = 1
        const val RELATION_OPPOSITE = -1

        val TAGS: Map<Int, PackageTag>
        init {
            val mTags: List<Pair<Int, ExpressibleTag.(Context, ApiViewingApp) -> Boolean>> = listOf(
                    PackageTag.TAG_ID_FLU to getCommonExpressing { it.nativeLibraries[4] },
                    PackageTag.TAG_ID_RN to getCommonExpressing { it.nativeLibraries[5] },
                    PackageTag.TAG_ID_XAM to getCommonExpressing { it.nativeLibraries[6] },
                    PackageTag.TAG_ID_KOT to getCommonExpressing { it.nativeLibraries[7] },
                    PackageTag.TAG_ID_64B to getCommonExpressing {
                        it.nativeLibraries.let { n -> (!n[0] || n[1]) && (!n[2] || n[3]) }
                    },
                    PackageTag.TAG_ID_ARM to getCommonExpressing {
                        it.nativeLibraries.let { n -> n[0] || n[1] } },
                    PackageTag.TAG_ID_X86 to getCommonExpressing {
                        it.nativeLibraries.let { n -> n[2] || n[3] } },
                    PackageTag.TAG_ID_HID to getCommonExpressing { !it.isLaunchable },
                    PackageTag.TAG_ID_SYS to getCommonExpressing { it.apiUnit == ApiUnit.SYS },
                    PackageTag.TAG_ID_SPL to getCommonExpressing { it.appPackage.hasSplits },
                    PackageTag.TAG_ID_AI to { context, app ->
                        if (!app.hasIcon) app.run {
                            retrieveAppIconInfo(getOriginalIconDrawable(context)!!.mutate())
                        }
                        getCommonExpressing { it.adaptiveIcon }.invoke(this, context, app)
                    },
                    PackageTag.TAG_ID_FCM to getServiceExpressing(
                            "com.google.firebase.messaging.FirebaseMessagingService"),
                    PackageTag.TAG_ID_HWP to getServiceExpressing(
                            "com.huawei.hms.support.api.push.service.HmsMsgService"),
                    PackageTag.TAG_ID_MIP to expressing@{ context, app ->
                        if (app !is TagCheckerApp) return@expressing false
                        // "com.xiaomi.mipush.sdk.ManifestChecker" to "checkServices"
                        val method = "com.xiaomi.mipush.sdk.u" to "d"
                        val args = Context.CONTEXT_INCLUDE_CODE or Context.CONTEXT_IGNORE_SECURITY
                        val loaderContext = context.createPackageContext(context.packageName, args)
                        val checkerMethod = try {
                            loaderContext.classLoader.loadClass(method.first).getDeclaredMethod(
                                    method.second, Context::class.java, PackageInfo::class.java).apply {
                                isAccessible = true
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            null
                        }
                        if (checkerMethod != null) {
                            getCommonExpressing {
                                if (it !is TagCheckerApp) return@getCommonExpressing false
                                try {
                                    checkerMethod.invoke(null, context, it.packageInfo)
                                    true
                                } catch (e: Throwable) {
                                    val message = e.cause?.message
                                    val appName = it.app.name
                                    val appPackage = it.app.packageName
                                    val appVer = it.app.verName
                                    Log.w("av.main.tag", "$message ($appPackage, $appName $appVer)")
                                    false
                                }
                            }.invoke(this, context, app)
                        } else {
                            getServiceExpressing("com.xiaomi.mipush.sdk.MessageHandleService")
                                    .invoke(this, context, app)
                        }
                    },
                    PackageTag.TAG_ID_MZP to getServiceExpressing(
                            "com.meizu.cloud.pushsdk.NotificationService"),
                    PackageTag.TAG_ID_OOP to getServiceExpressing(
                            "com.heytap.mcssdk.AppPushService"),
                    PackageTag.TAG_ID_VVP to getServiceExpressing(
                            "com.vivo.push.sdk.service.CommandClientService"),
                    PackageTag.TAG_ID_J_P to getServiceExpressing(
                            "cn.jpush.android.service.PushService"),
                    PackageTag.TAG_ID_U_P to getServiceExpressing(
                            "com.umeng.message.UmengIntentService"),
                    PackageTag.TAG_ID_TCP to getServiceExpressing(
                            "com.tencent.android.tpush.service.XGVipPushService"),
                    PackageTag.TAG_ID_ALP to getServiceExpressing(
                            "org.android.agoo.accs.AgooService"),
                    PackageTag.TAG_ID_DUP to getServiceExpressing(
                            "com.baidu.android.pushservice.PushService"),
                    PackageTag.TAG_ID_GTP to getServiceExpressing(
                            "com.igexin.sdk.PushService"),
            )
            val mRelations: List<Pair<
                    Pair<Int, (ExpressibleTag, Context, ApiViewingApp) -> Boolean>,
                    List<Pair<Int, Int>>>> = listOf(
                    (PackageTag.TAG_ID_GP to { tag: ExpressibleTag, context: Context, app: ApiViewingApp ->
                        val installer = AppTag.ensureInstaller(context, app)
                        val hasIt = installer == ApiViewingApp.packagePlayStore
                        (!tag.isAnti && hasIt) || (tag.isAnti && !hasIt)
                    }) to listOf(PackageTag.TAG_ID_PI to RELATION_COMPLIMENTARY),
                    (PackageTag.TAG_ID_PI to { tag: ExpressibleTag, context: Context, app: ApiViewingApp ->
                        val installer = AppTag.ensureInstaller(context, app)
                        val hasIt = installer != null && installer != ApiViewingApp.packagePlayStore
                        (!tag.isAnti && hasIt) || (tag.isAnti && !hasIt)
                    }) to listOf(PackageTag.TAG_ID_GP to RELATION_COMPLIMENTARY),
            )
            TAGS = mTags.associate { (id, app) ->
                id to PackageTag(id, app)
            } + mRelations.associate { (tag, relatives) ->
                val (id, app) = tag
                id to PackageTag(id, app, relatives.map { it.first to TagRelation(it.second) })
            }
        }

        private fun getCommonExpressing(checker: (ApiViewingApp) -> Boolean)
                : ExpressibleTag.(Context, ApiViewingApp) -> Boolean {
            return { _, app ->
                val hasIt = checker.invoke(app)
                (!isAnti && hasIt) || (isAnti && !hasIt)
            }
        }

        private fun getServiceExpressing(comp: String)
                : ExpressibleTag.(Context, ApiViewingApp) -> Boolean {
            return getCommonExpressing {
                it is TagCheckerApp && it.packageInfo?.services?.any { service ->
                    service.name == comp
                } == true
            }
        }

        fun getRelation(tag1: PackageTag, tag2: PackageTag): TagRelation? {
            return TAGS[tag1.id]?.relatives?.find {
                it.first == tag2.id
            }?.second
        }
    }
}
