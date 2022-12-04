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
import com.madness.collision.unit.api_viewing.ApiTaskManager
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
import kotlin.math.min
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
    private val sweetMargin by lazy { X.size(context, 5f, X.DP).roundToInt() }
    private val plainMargin by lazy { X.size(context, 2f, X.DP).roundToInt() }
    protected val loadPref: EasyAccess = EasyAccess
    private val margin: Int
        get() = if (loadPref.shouldShowDesserts) sweetMargin else plainMargin
    private val innerMargin: Float
        get() = if (loadPref.shouldShowDesserts) 5f else 2f
    protected open val shouldShowTime: Boolean
        get() = sortMethod == MyUnit.SORT_POSITION_API_TIME
    protected val itemLength: Int = X.size(context, 45f, X.DP).roundToInt()
    private val _colorSurface by lazy { ThemeUtil.getColor(context, R.attr.colorASurface) }
    protected val colorSurface: Int
        get() = if (loadPref.shouldShowDesserts) _colorSurface else 0
    private val animator = AppListAnimator()

    fun setSortMethod(sortMethod: Int): APIAdapter {
        this.sortMethod = sortMethod
        return this
    }

    fun getSortMethod() = sortMethod

    override fun onCreateBodyItemViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = AdapterAvBinding.inflate(inflater, parent, false)
        return Holder(binding)
    }

    fun ensureItem(index: Int): Boolean {
        if (index >= listCount) return true
        val app = apps[index]
        val isIconLoaded = !app.preload && !app.isLoadingIcon && app.hasIcon
        if (isIconLoaded) return true
        app.load(context)
        return false
    }

    private fun preloadAppIconsForward(index: Int) {
        ApiTaskManager.join {
            try {
                val endIndex = min(index + loadPref.preloadLimit, listCount)
                for (i in index until endIndex) ensureItem(i)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun ensureAppIcon(index: Int, logoView: ImageView) {
        ensureItem(index)
        val iconApp = logoView.getTag(R.bool.tagKeyAvAdapterItemIconId) as ApiViewingApp?
        if (index >= listCount) return
        val appInfo = apps[index]
        // logo was set with the same app icon
        if (appInfo === iconApp) return
        val finalBlock: suspend () -> Unit = {
            withContext(Dispatchers.Main) {
                logoView.setTag(R.bool.tagKeyAvAdapterItemIconId, appInfo)
                animator.animateLogo(logoView)
            }
        }
        if (appInfo.hasIcon) {
            finalBlock()
            return
        }
        // wait for icon otherwise (for unknown reason) there will be blank icons
        // once jumping from the bottom of the list directly to the top
        val elapsingTime = ElapsingTime()
        while (true) {
            if (appInfo.hasIcon) {
                finalBlock()
                break
            }
            if (elapsingTime.elapsed() >= 6000) break
            delay(200)
        }
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
        optimizeSideMargin(index, 15f, innerMargin, holder.card)
        holder.card.alterMargin(top = margin, bottom = margin)
        val appInfo = apps[index]
        holder.name.text = appInfo.name
        holder.logo.setTag(R.bool.tagKeyAvAdapterItemId, appInfo)

//        if (!appInfo.hasIcon) {
//            scope.launch(Dispatchers.Default) {
//                ensureAppIcon(index, holder.logo)
//            }
//        } else {
//            holder.logo.setTag(R.bool.tagKeyAvAdapterItemIconId, appInfo)
//            holder.logo.setImageBitmap(appInfo.icon)
//        }
        scope.launch(Dispatchers.IO) {
            holder.logo.load(AppPackageInfo(context, appInfo))
        }

        val loadLimitHalf = loadPref.loadLimitHalf
        val backIndex = index - loadLimitHalf - 5
        val shouldSpanFore = backIndex < 0
        val foreSpan: Int by lazy { -backIndex }
        val foreIndex = index + loadLimitHalf + 5
        val shouldSpanBack = foreIndex > listCount
        val backSpan: Int by lazy { foreIndex - listCount }
        val finalBackIndex = if (shouldSpanBack) backIndex - backSpan else backIndex
        val finalForeIndex = if (shouldSpanFore) foreIndex + foreSpan else foreIndex
        if (finalBackIndex >= 0 && !apps[finalBackIndex].preload) apps[finalBackIndex].clearIcons()
        if (finalForeIndex < listCount && !apps[finalForeIndex].preload) apps[finalForeIndex].clearIcons()

        // loadPref.preloadLimit may equal to zero
//        if (!appInfo.preload && loadPref.preloadLimit > 0 && loadPref.preloadLimit < listCount) {
//            val preloadIndex = index + loadPref.loadAmount
//            if (preloadIndex >= loadPref.preloadLimit && preloadIndex < listCount &&
//                    preloadIndex % loadPref.preloadLimit == 0 && apps[preloadIndex].preload) {
//                preloadAppIconsForward(index)
//            }
//        }

        val verInfo = if (loadPref.isViewingTarget) VerInfo.targetDisplay(appInfo)
        else VerInfo.minDisplay(appInfo)
        holder.api.text = verInfo.displaySdk

        if (loadPref.shouldShowDesserts) {
            holder.api.setTextColor(SealManager.getItemColorAccent(context, verInfo.api))
            val itemBack = SealManager.getItemColorBack(context, verInfo.api)
            holder.card.setCardBackgroundColor(itemBack)

            scope.launch(Dispatchers.Main) {
                val seal = SealMaker.getSealFile(context, verInfo.letter, itemLength)
                holder.seal.isVisible = seal != null
                seal?.let { holder.seal.load(it) }
            }
        } else {
            holder.api.setTextColor(holder.name.textColors)
            holder.seal.visibility = View.GONE
            holder.card.setCardBackgroundColor(colorSurface)
        }

        bindUpdateTime(holder, appInfo)

        holder.tags.removeAllViews()
        scope.launch(Dispatchers.Default) {
            AppTag.inflateAllTagsAsync(context, holder.tags, appInfo)
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