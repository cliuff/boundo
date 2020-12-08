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

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Handler
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.ChipGroup
import com.madness.collision.R
import com.madness.collision.diy.SandwichAdapter
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.data.EasyAccess
import com.madness.collision.unit.api_viewing.data.VerInfo
import com.madness.collision.unit.api_viewing.list.AppListAnimator
import com.madness.collision.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.min
import kotlin.math.roundToInt
import com.madness.collision.unit.api_viewing.R as MyR

internal class APIAdapter(context: Context, private val listener: Listener)
    : SandwichAdapter<APIAdapter.Holder>(context) {

    companion object {
        private val isSweet: Boolean
            get() = EasyAccess.isSweet
        val shouldShowDesserts: Boolean
            get() = isSweet

        var seals = HashMap<Char, Bitmap>().toMutableMap()
        var sealBack = HashMap<Char, Bitmap>().toMutableMap()

        init {
            GlobalScope.launch {
                val seal = File(F.valCachePubAvSeal(mainApplication))
                val files = seal.listFiles() ?: return@launch
                val regex = "(.+)-(.+)".toRegex()
                files@
                for (file in files){
                    val re = regex.find(file.nameWithoutExtension) ?: continue@files
                    val (cat, index) = re.destructured
                    val map = when(cat){
                        "seal" -> seals
                        "back" -> sealBack
                        else -> continue@files
                    }
                    val letter = index[0]
                    // may get deleted during cache clearing on app starts
                    if (map.containsKey(letter) || !file.exists()) continue@files
                    try {
                        val src = ImageUtil.getBitmap(file) ?: continue
                        map[letter] = src
                    } catch (e: OutOfMemoryError) {
                        e.printStackTrace()
                        continue@files
                    } catch (e: Exception) {
                        e.printStackTrace()
                        continue@files
                    }
//                    try {
//                    }catch (e: FileNotFoundException){
//                        e.printStackTrace()
//                    }
                }
            }
        }

        fun getAndroidCodenameImageRes(letter: Char): Int {
            if (!EasyAccess.isSweet) return 0
            return when (letter) {
                'r' -> MyR.drawable.seal_r_vector
                'q' -> MyR.drawable.seal_q_vector
                'p' -> MyR.drawable.seal_p_vector  // Pie
                'o' -> MyR.drawable.sdk_seal_o  // Oreo
                'n' -> MyR.drawable.sdk_seal_n  // Nougat
                'm' -> MyR.drawable.seal_m_vector  // Marshmallow
                'l' -> MyR.drawable.seal_l_vector  // Lollipop
                'k' -> MyR.drawable.seal_k_vector  // KitKat
                'j' -> MyR.drawable.seal_j_vector  // Jelly Bean
                'i' -> MyR.drawable.sdk_seal_i  // Ice Cream Sandwich
                'h' -> MyR.drawable.seal_h_vector  // Honeycomb
            /*10, 9 ->  // Gingerbread
            8 ->  // Froyo
            7, 6, 5 ->  // Eclair
            4 ->  // Donut
            3 ->  // Cupcake
            2 ->  // null
            1 ->  // null*/
                else -> 0
            }
        }

        fun getItemColorBack(context: Context, apiLevel: Int): Int {
            return itemColorInfo(context, apiLevel, false)
        }

        fun getItemColorAccent(context: Context, apiLevel: Int): Int {
            return itemColorInfo(context, apiLevel, true)
        }

        fun getItemColorForIllustration(context: Context, apiLevel: Int): Int {
            return itemColorInfo(context, apiLevel, isAccent = true, isForIllustration = true)
        }

        fun getItemColorText(apiLevel: Int) = when (apiLevel) {
            X.M -> Color.BLACK
            else -> Color.WHITE
        }

        private fun itemColorInfo(context: Context, apiLevel: Int, isAccent: Boolean, isForIllustration: Boolean = false): Int {
            if (!shouldShowDesserts && !isForIllustration) {
                val attrRes = if (isAccent) android.R.attr.textColor else R.attr.colorASurface
                return ThemeUtil.getColor(context, attrRes)
            }
            when (apiLevel) {
                X.R -> if (isAccent) "acd5c1" else "defbf0"
                X.Q -> if (isAccent) "c1d5ac" else "f0fbde"
                X.P -> if (isAccent) "e0c8b0" else "fff6d5"
                X.O, X.O_MR1 -> if (isAccent) "b0b0b0" else "eeeeee"
                X.N, X.N_MR1 -> if (isAccent) "ffb2a8" else "ffecf6"
                X.M -> if (isAccent) "b0c9c5" else "e0f3f0"
                X.L, X.L_MR1 -> if (isAccent) "ffb0b0" else "ffeeee"
                X.K, X.K_WATCH -> if (isAccent) "d0c7ba" else "fff3e0"
                X.J, X.J_MR1, X.J_MR2 -> if (isAccent) "baf5ba" else "eeffee"
                X.I, X.I_MR1 -> if (isAccent) "d8d0c0" else "f0f0f0"
                X.H, X.H_MR1, X.H_MR2 -> if (isAccent) "f0c8b4" else "fff5f0"
                else -> if (isAccent) "c5e8b0" else X.getColorHex(context, R.color.androidRobotGreenBack)
            }.run {
                val color = Color.parseColor("#$this")
                if (isAccent && !isForIllustration) return color
                return if (mainApplication.isDarkTheme) ColorUtil.darkenAs(color, if (isForIllustration) 0.7f else 0.15f) else color
            }
        }

        fun populate4Seal(context: Context, index: Char, itemLength: Int) {
            if (seals.containsKey(index)) return
            val res = getAndroidCodenameImageRes(index)
            if (res == 0) return
            val drawable: Drawable
            try {
                drawable = ContextCompat.getDrawable(context, res) ?: return
            } catch (e: Resources.NotFoundException) {
                e.printStackTrace()
                return
            }
            val bitmap = Bitmap.createBitmap(itemLength, itemLength, Bitmap.Config.ARGB_8888)
            drawable.setBounds(0, 0, itemLength, itemLength)
            drawable.draw(Canvas(bitmap))
            seals[index] = bitmap
            val path = F.createPath(F.valCachePubAvSeal(context), "seal-$index.png")
            if (F.prepare4(path)) X.savePNG(bitmap, path)
        }

        fun getSealForIllustration(context: Context, index: Char, size: Int): Bitmap? {
            if (seals.containsKey(index)) {
                seals[index]?.let {
                    return X.toMax(it, size)
                }
            }
            val res = getAndroidCodenameImageRes(index)
            if (res == 0) return null
            val drawable: Drawable
            try {
                drawable = ContextCompat.getDrawable(context, res) ?: return null
            } catch (e: Resources.NotFoundException) {
                e.printStackTrace()
                return null
            }
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            drawable.setBounds(0, 0, size, size)
            drawable.draw(Canvas(bitmap))
            return bitmap
        }
    }

    interface Listener {
        val click: (ApiViewingApp) -> Unit
        val longClick: (ApiViewingApp) -> Boolean
    }

    @SuppressLint("WrongViewCast")
    class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val logo: ImageView = itemView.findViewById(MyR.id.avAdapterInfoLogo)
        val name: AppCompatTextView = itemView.findViewById(MyR.id.avAdapterInfoName) as AppCompatTextView
        val updateTime: AppCompatTextView = itemView.findViewById(MyR.id.avAdapterInfoTime) as AppCompatTextView
        val tags: ChipGroup = itemView.findViewById(MyR.id.avAdapterInfoTags)
        val api: AppCompatTextView = itemView.findViewById(MyR.id.avAdapterInfoAPI) as AppCompatTextView
        val seal: ImageView = itemView.findViewById(MyR.id.avAdapterSeal)
        val card: MaterialCardView = itemView.findViewById(MyR.id.avAdapterCard)
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

    private lateinit var mRecyclerView: RecyclerView
    private lateinit var mLayoutManager: LinearLayoutManager
    private val inflater = LayoutInflater.from(context)
    private var sortMethod: Int = MyUnit.SORT_POSITION_API_LOW
    private val sweetMargin by lazy { X.size(context, 5f, X.DP).roundToInt() }
    private val plainMargin by lazy { X.size(context, 2f, X.DP).roundToInt() }
    private val margin: Int
        get() = if (shouldShowDesserts) sweetMargin else plainMargin
    private val shouldShowTime : Boolean
        get() = sortMethod == MyUnit.SORT_POSITION_API_TIME
    private val itemLength: Int = X.size(context, 70f, X.DP).roundToInt()
    private val _colorSurface by lazy { ThemeUtil.getColor(context, R.attr.colorASurface) }
    private val colorSurface: Int
        get() = if (shouldShowDesserts) _colorSurface else 0
    private val animator = AppListAnimator()

    fun setSortMethod(sortMethod: Int): APIAdapter {
        this.sortMethod = sortMethod
        return this
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        mRecyclerView = recyclerView
        mLayoutManager = mRecyclerView.layoutManager as LinearLayoutManager
    }

    override fun onCreateBodyItemViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val itemView = inflater.inflate(MyR.layout.adapter_av, parent, false)
        return Holder(itemView)
    }

    fun ensureItem(index: Int, refreshLayout: SwipeRefreshLayout? = null) {
        if (index >= listCount) return
        val app = apps[index]
        val canCeaseRefresh = refreshLayout != null
        val shouldCeaseRefresh = canCeaseRefresh && ((index == EasyAccess.loadAmount - 1) || (index == listCount - 1))
        if (shouldCeaseRefresh) {
            refreshLayout?.let {
                ApiTaskManager.now(Dispatchers.Main) {
                    it.isRefreshing = false
                }
            }
        }
        val isIconLoaded = !app.preload && !app.isLoadingIcon && app.hasIcon
        if (isIconLoaded) return
        GlobalScope.launch {
            app.setOnLoadedListener {
                if (shouldCeaseRefresh) {
                    launch(Dispatchers.Main) {
                        refreshLayout?.isRefreshing = false
                    }
                }
            }.load(context)
        }
    }

    private fun preloadAppIconsForward(index: Int) {
        ApiTaskManager.join {
            try{
                val endIndex = min(index + EasyAccess.preloadLimit, listCount)
                for (i in  index until endIndex) ensureItem(i)
            } catch (e: Exception){
                e.printStackTrace()
            }
        }
    }

    override fun onMakeBody(holder: Holder, index: Int) {
        // set surface color before setting the background color to avoid seen-through shadow of cards
//        if (shouldShowDesserts && !holder.card.cardBackgroundColor.isOpaque) {
//            holder.card.setCardBackgroundColor(colorSurface)
//        }
        holder.card.alterMargin(top = margin, bottom = margin)
        val appInfo = apps[index]
        holder.name.dartFuture(appInfo.name)
        holder.logo.setTag(R.bool.tagKeyAvAdapterItemId, appInfo)

        holder.tags.removeAllViews()
        val shouldWaitForIcon = !appInfo.hasIcon
        var taskIcon: Runnable? = null
        if (shouldWaitForIcon) {
            ensureItem(index)
            val logoView = holder.logo
            val checkerHandler = Handler()
            taskIcon = runnable {
                val iconApp = logoView.getTag(R.bool.tagKeyAvAdapterItemIconId) as ApiViewingApp?
                if (appInfo !== iconApp && appInfo.hasIcon) {
                    AppTag.tagAdaptiveIcon(context, appInfo, holder)
                    holder.logo.setTag(R.bool.tagKeyAvAdapterItemIconId, appInfo)
                    animator.animateLogo(logoView)
                    checkerHandler.removeCallbacks(this)
                } else {
                    checkerHandler.postDelayed(this, 400)
                }
            }
        } else {
            holder.logo.setTag(R.bool.tagKeyAvAdapterItemIconId, appInfo)
            holder.logo.setImageBitmap(appInfo.icon)
        }

        val loadLimitHalf = EasyAccess.loadLimitHalf
        val backIndex = index - loadLimitHalf - 5
        val shouldSpanFore = backIndex < 0
        val foreSpan: Int by lazy { - backIndex }
        val foreIndex = index + loadLimitHalf + 5
        val shouldSpanBack = foreIndex > listCount
        val backSpan: Int by lazy { foreIndex - listCount }
        val finalBackIndex = if (shouldSpanBack) backIndex - backSpan else backIndex
        val finalForeIndex = if (shouldSpanFore) foreIndex + foreSpan else foreIndex
        if (finalBackIndex >= 0 && !apps[finalBackIndex].preload) apps[finalBackIndex].clearIcons()
        if (finalForeIndex < listCount && !apps[finalForeIndex].preload) apps[finalForeIndex].clearIcons()

        // EasyAccess.preloadLimit may equal to zero
//        if (!appInfo.preload && EasyAccess.preloadLimit > 0 && EasyAccess.preloadLimit < listCount) {
//            val preloadIndex = index + EasyAccess.loadAmount
//            if (preloadIndex >= EasyAccess.preloadLimit && preloadIndex < listCount && preloadIndex % EasyAccess.preloadLimit == 0 && apps[preloadIndex].preload) {
//                preloadAppIconsForward(index)
//            }
//        }

        val verInfo = if (EasyAccess.isViewingTarget) VerInfo.targetDisplay(appInfo) else VerInfo.minDisplay(appInfo)
        holder.api.dartFuture(verInfo.displaySdk)

        populate4Seal(context, verInfo.letter, itemLength)
        if (shouldShowDesserts) {
            holder.api.setTextColor(getItemColorAccent(context, verInfo.api))
            val seal: Bitmap? = seals[verInfo.letter]
            if (seal == null) {
                holder.seal.visibility = View.GONE
            } else {
                holder.seal.visibility = View.VISIBLE
                holder.seal.setImageBitmap(X.toMin(seal, itemLength))
            }
            val itemBack = getItemColorBack(context, verInfo.api)
            holder.card.setCardBackgroundColor(itemBack)
        } else {
            holder.api.setTextColor(holder.name.textColors)
            holder.seal.visibility = View.GONE
            holder.card.setCardBackgroundColor(colorSurface)
        }

        if (shouldShowTime && appInfo.isNotArchive) {
            holder.updateTime.dartFuture(DateUtils.getRelativeTimeSpanString(appInfo.updateTime, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS))
            holder.updateTime.visibility = View.VISIBLE
        } else {
            holder.updateTime.visibility = View.GONE
        }

        GlobalScope.launch {
            val installer = AppTag.ensureResources(context, appInfo)
            launch(Dispatchers.Main) {
                AppTag.inflateTags(context, appInfo, holder, installer, !shouldWaitForIcon)
                taskIcon?.run()
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

}