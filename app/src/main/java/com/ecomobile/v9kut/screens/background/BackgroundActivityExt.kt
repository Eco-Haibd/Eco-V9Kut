package com.ecomobile.v9kut.screens.background

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.ecomobile.base.extension.click
import com.ecomobile.base.permission.PermissionUtil
import com.ecomobile.v9kut.screens.main.MainActivity
import com.ecomobile.v9kut.screens.main.goToLibrary
import com.ecomobile.v9kut.screens.main.requestImagePermission
import com.ecomobile.v9kut.screens.main.setupListener
import com.ecomobile.v9kut.screens.photo_gallery.PhotoGalleryActivity

fun BackgroundActivity.setupListener() {
    binding.apply {
        imgBack.setOnClickListener {
            finish()
        }
        cardChooseImage.click {
            if (PermissionUtil.hasPhotoPermission(this@setupListener)) {
                goToLibrary()
            } else {
                requestImagePermission()
            }
        }
    }
}

fun BackgroundActivity.requestImagePermission() {
    if (PermissionUtil.neverAskAgainSelected(
            this,
            PermissionUtil.imagePermission
        )
    ) {
        imagePermissionResult.launch(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
        })
    } else {
        imagePermissionLauncher.launch(PermissionUtil.imagePermission)
    }
}

fun BackgroundActivity.goToLibrary() {
    startActivity(Intent(this, PhotoGalleryActivity::class.java))
}