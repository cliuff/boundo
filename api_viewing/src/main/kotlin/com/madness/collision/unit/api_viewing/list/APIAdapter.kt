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
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.ChipGroup
import com.madness.collision.R
import com.madness.collision.diy.SandwichAdapter
import com.madness.collision.unit.api_viewing.AppTag
import com.madness.collision.unit.api_viewing.MyUnit
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.data.AppPackageInfo
import com.madness.collision.unit.api_viewing.data.EasyAccess
import com.madness.collision.unit.api_viewing.data.VerInfo
import com.madness.collision.unit.api_viewing.databinding.AdapterAvBinding
import com.madness.collision.unit.api_viewing.seal.SealMaker
import com.madness.collision.unit.api_viewing.seal.SealManager
import com.madness.collision.util.*
import kotlinx.coroutines.*
import java.lang.ref.WeakReference
import kotlin.math.roundToInt

internal open class APIAdapter(context: Context, private val listener: Listener,
                               private val scope: CoroutineScope)
    : SandwichAdapter<APIAdapter.Holder>(context) {

    interface Listener {
        val click: (ApiViewingApp) -> Unit
        val longClick: (ApiViewingApp) -> Boolean
    }

    open class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        lateinit var logo: ImageView
        lateinit var name: AppCompatTextView
        lateinit var updateTime: AppCompatTextView
        lateinit var tags: ChipGroup
        lateinit var api: AppCompatTextView
        lateinit var seal: ImageView
        lateinit var card: MaterialCardView

        constructor(binding: AdapterAvBinding): this(binding.root) {
            logo = binding.avAdapterInfoLogo
            name = binding.avAdapterInfoName as AppCompatTextView
            updateTime = binding.avAdapterInfoTime as AppCompatTextView
            tags = binding.avAdapterInfoTags
            api = binding.avAdapterInfoAPI as AppCompatTextView
            seal = binding.avAdapterSeal
            card = binding.avAdapterCard
        }
    }

    enum class Payload { UpdateTime }

    @Suppress("unchecked_cast")
    open var appList: List<*>
        get() = apps
        set(value) {
            apps = if (value.isNotEmpty() && value[0] is ApiViewingApp) value as List<ApiViewingApp> else emptyList()
        }
    var apps: List<ApiViewingApp> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }
    override var spanCount: Int = 1
        set(value) {
            if (value > 0) field = value
        }
    override val listCount: Int
        get() = apps.size

    protected val inflater: LayoutInflater = LayoutInflater.from(context)
    private var sortMethod: Int = MyUnit.SORT_POSITION_API_LOW
    protected val loadPref: EasyAccess = EasyAccess
    private val margin: Int = X.size(context, 5f, X.DP).roundToInt()
    protected open val shouldShowTime: Boolean
        get() = sortMethod == MyUnit.SORT_POSITION_API_TIME
    protected val itemLength: Int = X.size(context, 45f, X.DP).roundToInt()

    fun setSortMethod(sortMethod: Int): APIAdapter {
        this.sortMethod = sortMethod
        return this
    }

    fun getSortMethod() = sortMethod

    override fun onCreateBodyItemViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = AdapterAvBinding.inflate(inflater, parent, false)
        return Holder(binding)
    }

    override fun onMakeBody(holder: Holder, index: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            onMakeBody(holder, index)
            return
        }
        payloads.forEach p@{ p ->
            if (p !is Payload) return@p  // continue
            when (p) {
                Payload.UpdateTime -> bindUpdateTime(holder, apps[index])
            }
        }
    }

    override fun onMakeBody(holder: Holder, index: Int) {
        // set surface color before setting the background color to avoid seen-through shadow of cards
//        if (shouldShowDesserts && !holder.card.cardBackgroundColor.isOpaque) {
//            holder.card.setCardBackgroundColor(colorSurface)
//        }
        optimizeSideMargin(index, 15f, 5f, holder.card)
        holder.card.alterMargin(top = margin, bottom = margin)
        val appInfo = apps[index]
        holder.name.text = appInfo.name
        holder.logo.setTag(R.bool.tagKeyAvAdapterItemId, appInfo)

        scope.launch(Dispatchers.IO) {
            holder.logo.load(AppPackageInfo(context, appInfo))
        }

        val verInfo = if (loadPref.isViewingTarget) VerInfo.targetDisplay(appInfo)
        else VerInfo.minDisplay(appInfo)
        holder.api.text = verInfo.displaySdk

        holder.api.setTextColor(SealManager.getItemColorAccent(context, verInfo.api))
        val itemBack = SealManager.getItemColorBack(context, verInfo.api)
        holder.card.setCardBackgroundColor(itemBack)

        scope.launch(Dispatchers.Main) {
            val seal = SealMaker.getSealFile(context, verInfo.letterOrDev, itemLength)
            holder.seal.isVisible = seal != null
            seal?.let { holder.seal.load(it) }
        }

        bindUpdateTime(holder, appInfo)

        // use apk path, which is unique among apps and apks, instead of package name
        managedTagLoading(appInfo.appPackage.basePath, holder.tags) {
            holder.tags.removeAllViews()
            scope.launch(Dispatchers.Default) {
                AppTag.inflateAllTagsAsync(context, holder.tags, appInfo)
            }
        }

        holder.card.setOnClickListener {
            listener.click.invoke(appInfo)
        }

        holder.tags.setOnClickListener {
            holder.card.performClick()
        }

        holder.card.setOnLongClickListener {
            listener.longClick.invoke(appInfo)
        }
    }

    /** pkgName to Job/tagContainer */
    private val tagLoadingJobs = HashMap<String, Pair<Job, WeakReference<View>>>()

    private inline fun managedTagLoading(newTagPkg: String, tagsView: ViewGroup, newJob: () -> Job) {
        // retrieve last pkgName from tags view because of view recycling
        val lastTagPkg = tagsView.getTag(R.bool.tagKeyAvAdapterTags) as? String?
        tagsView.setTag(R.bool.tagKeyAvAdapterTags, newTagPkg)
        // cancel last loading job to fix wrong tags issue right after recycling
        // (tags are loaded for the old pkg before recycling, instead of the new one after recycling)
        if (lastTagPkg != null && lastTagPkg != newTagPkg) {
            tagLoadingJobs[lastTagPkg]?.let { (lastJob, lastViewRef) ->
                // match view ref in case job was overridden/taken over by another binding view
                if (lastViewRef.get() === tagsView && lastJob.isActive) lastJob.cancel()
            }
        }
        // wait for old job to finish or start a new one
        val wait = tagLoadingJobs[newTagPkg]?.let { (j, v) -> v.get() === tagsView && j.isActive }
        if (wait != true) tagLoadingJobs[newTagPkg] = newJob() to WeakReference(tagsView)
    }

    private fun bindUpdateTime(holder: Holder, appInfo: ApiViewingApp) {
        if (shouldShowTime && appInfo.isNotArchive) {
            val updateTime = DateUtils.getRelativeTimeSpanString(appInfo.updateTime,
                System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS)
            holder.updateTime.text = updateTime
            holder.updateTime.visibility = View.VISIBLE
        } else {
            holder.updateTime.visibility = View.GONE
        }
    }

}