package com.madness.collision.unit.no_media

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.madness.collision.R
import com.madness.collision.diy.SandwichAdapter
import com.madness.collision.unit.no_media.data.Media

internal class ItemAdapter(
        context: Context,
        private val images: List<Media>,
        private val itemWidth: Int,
        private val itemHeight: Int
) : SandwichAdapter<ItemAdapter.ItemHolder>(context) {

    class ItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.nmItemAdapterCover)
        val container: ConstraintLayout = itemView.findViewById(R.id.nmItemAdapterContainer)
    }

    override var spanCount: Int = 1
        set(value) {
            if (value > 0) field = value
        }
    override val listCount: Int
        get() = images.size

    override fun onCreateBodyItemViewHolder(parent: ViewGroup, viewType: Int): ItemHolder {
        return ItemHolder(LayoutInflater.from(context).inflate(R.layout.adapter_nm_item, parent, false))
    }

    override fun onMakeBody(holder: ItemHolder, index: Int) {
        holder.container.maxHeight = itemHeight

        val requestOptions = RequestOptions()
                .placeholder(R.drawable.img_gallery)
                .centerCrop().override(itemWidth, itemHeight)
        Glide.with(context).load(images[index].path).apply(requestOptions).into(holder.imageView)
    }

}
