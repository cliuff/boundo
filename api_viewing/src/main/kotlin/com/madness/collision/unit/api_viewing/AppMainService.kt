/*
 * Copyright 2020 Clifford Liu
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
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.util.SheetUtil
import com.madness.collision.util.F
import java.io.File

internal class AppMainService {

    fun exportList(context: Context, apps: List<ApiViewingApp>): File? {
        val name = "AppList"
        val path = F.createPath(F.cachePublicPath(context), "Temp", "AV", "$name.csv")
        val file = File(path)
        if (!F.prepare4(file)) return null
        SheetUtil.csvWriterAll(getList(context, apps), file)
        return file
    }

    private fun getList(context: Context, apps: List<ApiViewingApp>): List<Array<String>> {
//        val list = apps
//        val re = ArrayList<Array<String>>(list.size + 1)
//        re.add(arrayOf(context.getString(R.string.apiDetailsPackageName)))
//        list.forEach {
//            re.add(arrayOf(it.name))
//        }
//        return re
        return apps.map {
            arrayOf(it.name)
        }
    }
}