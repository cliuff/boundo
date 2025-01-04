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

package com.madness.collision.chief.app

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.os.BundleCompat
import androidx.core.view.ViewCompat
import com.madness.collision.chief.chiefContext
import com.madness.collision.util.os.ActivitySystemBarMaintainer
import com.madness.collision.util.os.SystemBarMaintainer
import com.madness.collision.util.os.SystemBarMaintainerOwner
import com.madness.collision.util.os.checkInsets
import com.madness.collision.util.os.edgeToEdge
import com.madness.collision.util.os.enableEdgeToEdge

fun ComposePageActivityIntent(route: ComposePageRoute): Intent =
    Intent(chiefContext, ComposePageActivity::class.java)
        .putExtra(ComposePageActivity.EXTRA_ROUTE, route)

@Suppress("FunctionName")
@JvmName("ComposePageActivityTypedIntent")
inline fun <reified T : ComposePageActivity> ComposePageActivityIntent(route: ComposePageRoute) =
    Intent(chiefContext, T::class.java)
        .putExtra(ComposePageActivity.EXTRA_ROUTE, route)


/** Wrapper activity to show a [ComposePageRoute]. */
open class ComposePageActivity : BaseActivity(), SystemBarMaintainerOwner {
    companion object {
        /** The specific [ComposePageRoute] to show. */
        const val EXTRA_ROUTE: String = "com.madness.collision.chief.app.extra.Route"
    }

    override val systemBarMaintainer: SystemBarMaintainer = ActivitySystemBarMaintainer(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (this as SystemBarMaintainerOwner).enableEdgeToEdge()
        // query window insets to change system bar styles accordingly
        window?.decorView?.findViewById<ViewGroup>(android.R.id.content)?.let { contentView ->
            ViewCompat.setOnApplyWindowInsetsListener(contentView) { _, insets ->
                val platInsets = insets.toWindowInsets()?.takeIf(::checkInsets)
                if (platInsets != null) edgeToEdge(platInsets, false)
                // return the insets as they are, as we only query not consume
                insets
            }
        }

        val route = intent?.extras?.let { extras ->
            BundleCompat.getParcelable(extras, EXTRA_ROUTE, ComposePageRoute::class.java)
        }
        if (route != null) {
            val navController = ActivityPageNavController(this)
            setContent {
                CompositionLocalProvider(
                    LocalPageNavController provides navController,
                    content = { route.content() },
                )
            }
        }
    }
}