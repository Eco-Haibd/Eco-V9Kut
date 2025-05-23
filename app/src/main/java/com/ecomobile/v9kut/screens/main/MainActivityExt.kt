package com.ecomobile.v9kut.screens.main

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.ActivityResult
import androidx.appcompat.app.AppCompatActivity.RESULT_OK
import com.ecomobile.base.extension.click
import com.ecomobile.base.extension.isActive
import com.ecomobile.base.extension.putExtra
import com.ecomobile.base.permission.PermissionUtil
import com.ecomobile.v9kut.screens.background.BackgroundActivity
import com.ecomobile.v9kut.screens.photo_cutter.PhotoCutterActivity
import com.ecomobile.v9kut.screens.photo_gallery.PhotoGalleryActivity

fun MainActivity.setupListener() {
    binding.apply {
        cardCut.click {
            if (PermissionUtil.hasPhotoPermission(this@setupListener)) {
                goToLibrary()
            } else {
                requestImagePermission()
            }
        }
        cardBackground.click {
            goToBackgroundActivity()
        }
    }
}

fun MainActivity.requestImagePermission() {
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

fun MainActivity.goToLibrary() {
    chooseImageLauncher.launch(Intent(this, PhotoGalleryActivity::class.java))
}

fun MainActivity.goToBackgroundActivity() {
    startActivity(Intent(this, BackgroundActivity::class.java))
}

fun MainActivity.onHaveImage(result: ActivityResult) {
    if (!isActive() || result.resultCode != RESULT_OK) return
    result.data?.getStringExtra(PhotoGalleryActivity.IMAGE_PATH)?.let {
        Intent(this, PhotoCutterActivity::class.java).apply {
            putExtra(PhotoCutterActivity.IMAGE_PATH, it)
            startActivity(this)
        }
    }
}