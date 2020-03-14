package com.madness.collision.wearable.diy

import androidx.recyclerview.widget.RecyclerView

/**
 * adapter that allow easy blank space at both the top and the bottom of the list
 */
internal abstract class SandwichAdapter<VH: RecyclerView.ViewHolder>: RecyclerView.Adapter<VH>(){

    abstract var spanCount: Int
    abstract val listCount: Int
    /**
     * count of items to fill the span
     */
    private val rearFillCount: Int
        get() = if (spanCount == 1) 0 else ((spanCount - (listCount % spanCount)) % spanCount)
    private val rearCount: Int
        get() = rearFillCount + 1
    private val frontCount: Int
        get() = spanCount
    private val indexBodyStart: Int
        get() = frontCount
    private val indexFillInStart: Int
        get() = frontCount + listCount
    private val indexBottom: Int
        get() = frontCount + listCount + rearFillCount

    protected open fun onMakeTopCover(holder: VH){}

    protected open fun onMakeBody(holder: VH, index: Int){}

    protected open fun onMakeFillIn(holder: VH){}

    protected open fun onMakeBottomCover(holder: VH){}

    override fun getItemCount(): Int {
        if (listCount == 0) return 0
        return listCount + frontCount + rearCount
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        when (position) {
            in 0 until indexBodyStart -> onMakeTopCover(holder)
            in indexBodyStart until indexFillInStart -> onMakeBody(holder, position - frontCount)
            in indexFillInStart until indexBottom -> onMakeFillIn(holder)
            in indexBottom until itemCount -> onMakeBottomCover(holder)
        }
    }

}
