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

package com.madness.collision.unit.api_viewing.ui.list

import android.app.Activity
import android.content.ClipData
import android.content.ClipDescription
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.core.app.ActivityCompat
import androidx.core.view.DragAndDropPermissionsCompat
import java.lang.ref.WeakReference

@Composable
fun rememberInterAppDragAndDropTarget(
    viewModel: AppListViewModel,
    verifier: DragAndDropVerifier = LocalDragAndDropVerifier.current,
): DragAndDropTarget {

    fun ClipData.toQueryOrUris() = run {
        if (description.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)
            || description.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML)) {
            if (itemCount > 0 && getItemAt(0).text.isNullOrEmpty().not()) {
                return@run getItemAt(0).text to null
            }
        }
        null to (0..<itemCount).mapNotNull { i -> getItemAt(i).uri }
    }

    return remember {
        object : DragAndDropTarget {
            override fun onEntered(event: DragAndDropEvent) {
                // todo notify R.string.apiDragDropHint
            }

            override fun onDrop(event: DragAndDropEvent): Boolean {
                val (query, uriList) = event.toAndroidDragEvent().clipData.toQueryOrUris()
                if (query != null) {
                    viewModel.setQueryFilter(query)
                } else if (uriList != null) {
                    verifier.requestPermissions(event)
                    viewModel.toggleListSrc(AppListSrc.DragAndDrop(uriList)) {
                        verifier.releasePermissions(event)
                    }
                }
                return true
            }
        }
    }
}

@Stable
interface DragAndDropVerifier {
    fun requestPermissions(event: DragAndDropEvent)
    fun releasePermissions(event: DragAndDropEvent)
}

val LocalDragAndDropVerifier = compositionLocalOf<DragAndDropVerifier> {
    error("DragAndDropVerifier not provided!")
}

@Stable
class ActivityDragAndDropVerifier(activity: Activity) : DragAndDropVerifier {
    private val activityRef = WeakReference(activity)
    private val permissionMap = mutableMapOf<DragAndDropEvent, DragAndDropPermissionsCompat>()

    override fun requestPermissions(event: DragAndDropEvent) {
        val activity = activityRef.get() ?: return
        val p = ActivityCompat.requestDragAndDropPermissions(activity, event.toAndroidDragEvent())
        if (p != null) permissionMap[event] = p
    }

    override fun releasePermissions(event: DragAndDropEvent) {
        permissionMap.remove(event)?.release()
    }
}
