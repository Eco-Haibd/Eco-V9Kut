package com.ecomobile.v9kut.screens.photo_gallery

import android.app.Activity
import android.content.Intent
import com.ecomobile.base.recyclerview.GridSpacingItemDecoration
import com.ecomobile.v9kut.model.ImageModel
import kotlin.math.roundToInt

fun PhotoGalleryActivity.setupRecyclerView() {
    imageViewModel.queryAllImages(this)
    binding.rcvImages.apply {
        adapter = imageAdapter
        addItemDecoration(
            GridSpacingItemDecoration(
                3,
                resources.getDimension(com.intuit.sdp.R.dimen._5sdp).roundToInt(),
                true
            )
        )
    }
}

fun PhotoGalleryActivity.listener() {
    binding.apply {
        imgBack.setOnClickListener {
            finish()
        }
    }
}

fun PhotoGalleryActivity.observerData() {
    imageViewModel.folders.observe(this) {
        if (it.size > 0) {
            imageViewModel.getAllImageInCurrentFolder(0)
        }
    }

    imageViewModel.folderImages.observe(this) {
        imageAdapter.update(it)
    }
}

fun PhotoGalleryActivity.onChooseImage(imageModel: ImageModel) {
    val intent = Intent().apply {
        putExtra(PhotoGalleryActivity.IMAGE_PATH, imageModel.path)
    }
    setResult(Activity.RESULT_OK, intent)
    finish()
}