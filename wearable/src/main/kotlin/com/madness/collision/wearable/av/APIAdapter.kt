package com.madness.collision.wearable.av

import android.content.Context
import android.graphics.Color
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Space
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import com.madness.collision.wearable.av.data.ApiViewingApp
import com.madness.collision.wearable.av.data.EasyAccess
import com.madness.collision.wearable.av.data.VerInfo
import com.madness.collision.wearable.databinding.AdapterAvBinding
import com.madness.collision.wearable.diy.SandwichAdapter
import com.madness.collision.wearable.util.X
import com.madness.collision.wearable.util.dartFuture

internal class APIAdapter(private val context: Context) : SandwichAdapter<APIAdapter.Holder>() {

    companion object {
        fun getItemColorAccent(apiLevel: Int): Int {
            when (apiLevel) {
                X.P -> "e0c8b0"
                X.O, X.O_MR1 -> "b0b0b0"
                X.N, X.N_MR1 -> "ffb2a8"
                X.M -> "b0c9c5"
                X.L, X.L_MR1 -> "ffb0b0"
                X.K, X.K_WATCH -> "d0c7ba"
                X.J, X.J_MR1, X.J_MR2 -> "baf5ba"
                X.I, X.I_MR1 -> "d8d0c0"
                X.H, X.H_MR1, X.H_MR2 -> "f0c8b4"
                else -> "c5e8b0"
            }.run {
                return Color.parseColor("#$this")
            }
        }
    }

    class Holder(binding: AdapterAvBinding) : RecyclerView.ViewHolder(binding.root) {
        val logo: ImageView = binding.avAdapterInfoLogo
        val name: AppCompatTextView = binding.avAdapterInfoName as AppCompatTextView
        val updateTime: AppCompatTextView = binding.avAdapterInfoTime as AppCompatTextView
        val api: AppCompatTextView = binding.avAdapterInfoAPI as AppCompatTextView
        val info: LinearLayout = binding.avAdapterInfo
        val space: Space = binding.avAdapterSpace
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
    val indexOffset: Int
        get() = spanCount

    private lateinit var rv: RecyclerView
    private val inflater = LayoutInflater.from(context)
    private var sortMethod: Int = ApiFragment.SORT_POSITION_API_LOW
    var listInsetBottom = 0
    private val shouldShowTime : Boolean
        get() = sortMethod == ApiFragment.SORT_POSITION_API_TIME

    fun setSortMethod(sortMethod: Int): APIAdapter {
        this.sortMethod = sortMethod
        return this
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        rv = recyclerView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(AdapterAvBinding.inflate(inflater, parent, false))
    }

    private fun makeSpace(holder: Holder, height: Int){
        holder.space.visibility = View.VISIBLE
        holder.info.visibility = View.GONE
        holder.space.minimumHeight = height
    }

    override fun onMakeTopCover(holder: Holder) {
        makeSpace(holder, 0)
    }

    override fun onMakeFillIn(holder: Holder) {
        makeSpace(holder, 0)
    }

    override fun onMakeBottomCover(holder: Holder) {
        makeSpace(holder, 0)
    }

    override fun onMakeBody(holder: Holder, index: Int) {
        holder.space.visibility = View.GONE
        holder.info.visibility = View.VISIBLE

        holder.logo.visibility = if (EasyAccess.isInAmbientMode) View.INVISIBLE else View.VISIBLE

        val appInfo = apps[index]
        holder.name.dartFuture(appInfo.name)
        if (appInfo.preload) return

        holder.logo.setImageBitmap(appInfo.logo)

        val verInfo = VerInfo.targetDisplay(appInfo)
        holder.api.dartFuture(verInfo.sdk)

        holder.api.setTextColor(if (!EasyAccess.isInAmbientMode && EasyAccess.isSweet) getItemColorAccent(verInfo.api)
        else holder.name.currentTextColor)

        if (shouldShowTime) {
            holder.updateTime.dartFuture(DateUtils.getRelativeTimeSpanString(appInfo.updateTime, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS))
            holder.updateTime.visibility = View.VISIBLE
        } else {
            holder.updateTime.visibility = View.GONE
        }
    }
}