package com.madness.collision.unit.api_viewing

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.util.SparseIntArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.madness.collision.diy.SandwichAdapter
import com.madness.collision.unit.api_viewing.data.EasyAccess
import com.madness.collision.unit.api_viewing.data.VerInfo
import com.madness.collision.util.X
import com.madness.collision.util.dartFuture
import com.madness.collision.util.mainApplication
import kotlin.math.roundToInt

internal class StatsAdapter(context: Context) : SandwichAdapter<StatsAdapter.Holder>(context) {

    @SuppressLint("WrongViewCast")
    class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val card: MaterialCardView = itemView.findViewById(R.id.avStatsAdapterCard)
        val logoBack: ImageView = itemView.findViewById(R.id.avStatsAdapterLogoBack)
        val logoText: AppCompatTextView = itemView.findViewById(R.id.avStatsAdapterLogoText) as AppCompatTextView
        val version: AppCompatTextView = itemView.findViewById(R.id.avStatsAdapterVersion) as AppCompatTextView
        val codeName: AppCompatTextView = itemView.findViewById(R.id.avStatsAdapterName) as AppCompatTextView
        val count: AppCompatTextView = itemView.findViewById(R.id.avStatsAdapterCount) as AppCompatTextView
        val seal: ImageView = itemView.findViewById(R.id.avStatsAdapterSeal)

        constructor(itemView: View, sweetMargin: Int): this(itemView) {
//            mViews.avStatsAdapterCard.cardElevation = sweetElevation
//            mViews.avStatsAdapterCard.stateListAnimator = AnimatorInflater.loadStateListAnimator(context, R.animator.res_lift_card_on_touch)
            (card.layoutParams as RecyclerView.LayoutParams).run {
                topMargin = sweetMargin
                bottomMargin = sweetMargin
            }
        }

    }

    var stats: SparseIntArray = SparseIntArray(0)
        set(value) {
            field = value
            notifyDataSetChanged()
        }
    override var spanCount: Int = 1
        set(value) {
            if (value > 0) field = value
        }
    override val listCount: Int
        get() = stats.size()
    val indexOffset: Int
        get() = spanCount

    private val isExterior = mainApplication.exterior
    private val isSweet = EasyAccess.isSweet
    private val shouldShowDesserts = !isExterior && isSweet && (mainApplication.isPaleTheme || mainApplication.isDarkTheme)
//    private val sweetElevation = if (shouldShowDesserts) X.size(context, 1f, X.DP) else 0f
    private val sweetMargin = if (shouldShowDesserts) X.size(context, 5f, X.DP).roundToInt() else 0
    private val itemLength: Int = X.size(context, 70f, X.DP).roundToInt()

    private lateinit var rv: RecyclerView
    private val inflater = LayoutInflater.from(context)

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        rv = recyclerView
    }

    override fun onCreateBodyItemViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val itemView = inflater.inflate(R.layout.adapter_av_stats, parent, false)
        return if (shouldShowDesserts) Holder(itemView, sweetMargin) else Holder(itemView)
    }

    override fun onMakeBody(holder: Holder, index: Int) {
        val statsCount = stats.valueAt(index)
        val verInfo = VerInfo(stats.keyAt(index), true)

        holder.version.dartFuture(verInfo.sdk)
        holder.count.dartFuture(statsCount.toString())

        holder.logoText.dartFuture(verInfo.api.toString())
        disposeAPIInfo(verInfo, holder.logoText, holder.logoBack)

        APIAdapter.populate4Seal(context, verInfo.letter, itemLength)
        if (shouldShowDesserts){
            holder.count.setTextColor(APIAdapter.getItemColorAccent(context, verInfo.api))
            val seal: Bitmap? = APIAdapter.seals[verInfo.letter]
            if (seal == null) {
                holder.seal.visibility = View.GONE
            } else {
                holder.seal.visibility = View.VISIBLE
                holder.seal.setImageBitmap(X.toMin(seal, itemLength))
            }
            val itemBack = APIAdapter.getItemColorBack(context, verInfo.api)
            holder.card.setCardBackgroundColor(itemBack)
        }

        if (isSweet) {
            holder.codeName.dartFuture(verInfo.codeName(context))
            holder.codeName.visibility = View.VISIBLE
        } else {
            holder.codeName.visibility = View.GONE
        }
    }

    private fun disposeAPIInfo(ver: VerInfo, logoText: TextView, logoBack: ImageView) {
        logoText.text = ver.sdk
        if (EasyAccess.isSweet){
            val colorText = APIAdapter.getItemColorText(ver.api)
            logoText.setTextColor(colorText)
        }

        val bitmap: Bitmap = ApiInfoPop.disposeSealBack(context, ver.letter, itemLength)
        logoBack.setImageBitmap(X.circularBitmap(bitmap))
    }
}
