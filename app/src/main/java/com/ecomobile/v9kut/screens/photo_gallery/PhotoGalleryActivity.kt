package com.ecomobile.v9kut.screens.photo_gallery

import android.view.View
import com.ecomobile.base.BaseActivity
import com.ecomobile.v9kut.R
import com.ecomobile.v9kut.databinding.ActivityPhotoLibraryBinding
import com.ecomobile.v9kut.screens.photo_gallery.adapter.ImageAdapter
import com.ecomobile.v9kut.viewmodel.ImageViewModel
import org.koin.android.ext.android.inject

class PhotoGalleryActivity: BaseActivity<ActivityPhotoLibraryBinding>() {

    companion object {
        const val IMAGE_PATH = "image_path"
    }

    val imageViewModel: ImageViewModel by inject()

    override val layoutResId: Int
        get() = R.layout.activity_photo_library

    override fun getTopView(): View = binding.header

    val imageAdapter by lazy {
        ImageAdapter(this, mutableListOf()).apply {
            onItemClick = {
                onChooseImage(it)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        setupRecyclerView()
        listener()
        observerData()
    }
}