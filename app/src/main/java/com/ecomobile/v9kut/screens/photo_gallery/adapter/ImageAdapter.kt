package com.ecomobile.v9kut.screens.photo_gallery.adapter

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.ecomobile.base.BaseAdapter
import com.ecomobile.base.extension.click
import com.ecomobile.v9kut.R
import com.ecomobile.v9kut.databinding.ItemImageBinding
import com.ecomobile.v9kut.model.ImageModel

class ImageAdapter(
    private val context: Context,
    list: List<ImageModel>
): BaseAdapter<ImageModel, ItemImageBinding>(list) {

    var onItemClick : ((ImageModel) -> Unit)? = null

    private val requestOptions = RequestOptions()
        .override(300, 300)
        .fitCenter()

    override fun getLayoutResId() = R.layout.item_image

    override fun bind(binding: ItemImageBinding, item: ImageModel, position: Int) {
        Glide.with(context)
            .load(item.uri)
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
            .apply(requestOptions)
            .into(binding.imgImage)
        binding.root.click {
            onItemClick?.invoke(item)
        }
    }
}

