package com.ecomobile.base.extension

import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatImageView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target

fun ImageView.load(@DrawableRes drawableRes: Int) {
    Glide.with(context.applicationContext)
        .load(drawableRes).into(this)
}

@BindingAdapter("imageUrl")
fun loadImage(view: ImageView, url: String?) {
    if (!url.isNullOrEmpty()) {
        Glide.with(view.context)
            .load(url)
            .into(view)
    }
}


fun ImageView.load(data: Any?, callback: ((Boolean) -> Unit)? = null) {
    Glide.with(context.applicationContext).load(data)
        .listener(object : RequestListener<Drawable> {
        override fun onLoadFailed(
            e: GlideException?,
            model: Any?,
            target: Target<Drawable>,
            isFirstResource: Boolean
        ): Boolean {
            callback?.invoke(false)
            return false
        }

        override fun onResourceReady(
            resource: Drawable,
            model: Any,
            target: Target<Drawable>?,
            dataSource: DataSource,
            isFirstResource: Boolean
        ): Boolean {
            callback?.invoke(true)
            return false
        }
    }).into(this)
}

fun ImageView.load(data: Any?, @DrawableRes errorRes: Int, callback: ((Boolean) -> Unit)? = null) {
    Glide.with(context.applicationContext)
        .load(data)
        .listener(object : RequestListener<Drawable> {
        override fun onLoadFailed(
            e: GlideException?,
            model: Any?,
            target: Target<Drawable>,
            isFirstResource: Boolean
        ): Boolean {
            callback?.invoke(false)
            return false
        }

        override fun onResourceReady(
            resource: Drawable,
            model: Any,
            target: Target<Drawable>?,
            dataSource: DataSource,
            isFirstResource: Boolean
        ): Boolean {
            callback?.invoke(true)
            return false
        }
    }).error(errorRes).into(this)
}