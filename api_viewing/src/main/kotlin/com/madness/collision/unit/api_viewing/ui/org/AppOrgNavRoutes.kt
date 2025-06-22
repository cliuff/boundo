/*
 * Copyright 2025 Clifford Liu
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

package com.madness.collision.unit.api_viewing.ui.org

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import com.madness.collision.chief.app.ComposePageRoute
import com.madness.collision.unit.api_viewing.ui.org.coll.CollAppListPage
import com.madness.collision.unit.api_viewing.ui.org.group.GroupEditorPage
import com.madness.collision.unit.api_viewing.ui.org.group.GroupInfoPage
import io.cliuff.boundo.org.model.CompColl
import io.cliuff.boundo.org.model.OrgGroup
import kotlinx.parcelize.Parcelize

@Parcelize
class AppOrgNavRoute(private val routeId: OrgRouteId) : ComposePageRoute {

    @Composable
    @NonRestartableComposable
    override fun content() = routeId.RouteContent()
}

interface RouteId<R : ComposePageRoute> {
    fun asRoute(): R
}

sealed interface OrgRouteId : RouteId<AppOrgNavRoute>, Parcelable {

    override fun asRoute(): AppOrgNavRoute = AppOrgNavRoute(this)

    /** @param collId the collection to add the group to, or create a new collection. */
    @Parcelize
    class NewGroup(val collId: Int = -1) : OrgRouteId

    @Parcelize
    class GroupEditor(val collId: Int, val groupId: Int) : OrgRouteId

    @Parcelize
    class GroupInfo(val group: OrgGroup, val collId: Int, val groupId: Int) : OrgRouteId

    @Parcelize
    class CollAppList(val coll: CompColl, val collId: Int = coll.id) : OrgRouteId
}

/** @param collGroupId Group ID to modify, a string formatted as "collId:groupId". */
private fun resolveCollGroupId(collGroupId: String): Pair<Int, Int> {
    val match = collGroupId.let("""(\d+):(\d+)""".toRegex()::matchEntire)
    val collGroupIdPair = match?.run { groupValues[1].toInt() to groupValues[2].toInt() }
    return collGroupIdPair ?: (-1 to -1)
}

@Composable
private fun OrgRouteId.RouteContent(): Unit =
    when (this) {
        is OrgRouteId.NewGroup -> {
            GroupEditorPage(modCollId = collId)
        }
        is OrgRouteId.GroupEditor -> {
            GroupEditorPage(modCollId = collId, modGroupId = groupId)
        }
        is OrgRouteId.GroupInfo -> {
            GroupInfoPage(group = group, modCollId = collId, modGroupId = groupId)
        }
        is OrgRouteId.CollAppList -> {
            CollAppListPage(coll = coll, modCollId = collId)
        }
    }
