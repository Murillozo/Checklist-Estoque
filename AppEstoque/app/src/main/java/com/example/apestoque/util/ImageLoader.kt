package com.example.apestoque.util

import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import com.bumptech.glide.request.RequestOptions

object ImageLoader {
    private val listOptions = RequestOptions()
        .downsample(DownsampleStrategy.AT_MOST)
        .format(DecodeFormat.PREFER_RGB_565)
        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
        .dontAnimate()
        .dontTransform()

    fun loadThumbnail(iv: ImageView, uri: Any, targetW: Int, targetH: Int) {
        Glide.with(iv.context)
            .load(uri)
            .apply(listOptions)
            .override(targetW, targetH)
            .centerCrop()
            .thumbnail(0.25f)
            .into(iv)
    }

    fun loadFullScreen(iv: ImageView, uri: Any, max: Int = 2160) {
        Glide.with(iv.context)
            .load(uri)
            .apply(
                RequestOptions()
                    .downsample(DownsampleStrategy.AT_MOST)
                    .format(DecodeFormat.PREFER_RGB_565)
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            )
            .override(max)
            .fitCenter()
            .into(iv)
    }
}

