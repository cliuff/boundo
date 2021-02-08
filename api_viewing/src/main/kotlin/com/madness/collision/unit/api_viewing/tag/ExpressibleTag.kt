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
import com.madness.collision.unit.api_viewing.data.ApiViewingApp

/**
 * PackageTag is tag declaration, which should be immutable.
 * This is the mutable version.
 */
internal class ExpressibleTag(tag: PackageTag, var isAnti: Boolean = false): PackageTag(
        tag.id, tag.expressing, tag.relatives
) {
    var context: Context? = null
    var appPackage: ApiViewingApp? = null

    override fun express(): Boolean {
        val context = context ?: return false
        val app = appPackage ?: return false
        expressing ?: return false
        return expressing.invoke(this, context, app)
    }

    fun setContext(context: Context): ExpressibleTag {
        this.context = context
        return this
    }

    fun setPackage(appPackage: ApiViewingApp): ExpressibleTag {
        this.appPackage = appPackage
        return this
    }

    fun setRes(context: Context, appPackage: ApiViewingApp): ExpressibleTag {
        this.context = context
        this.appPackage = appPackage
        return this
    }

    fun anti(): ExpressibleTag {
        isAnti = true
        return this
    }

    fun relate(other: ExpressibleTag): TagRelation? {
        if (id == other.id) {
            if (isAnti == other.isAnti) return TagRelation(TagRelation.RELATION_SAME)
            return TagRelation(TagRelation.RELATION_OPPOSITE)
        }
        return TagRelation.getRelation(this, other)
    }

}

internal fun PackageTag.toExpressible(): ExpressibleTag {
    return ExpressibleTag(this)
}
