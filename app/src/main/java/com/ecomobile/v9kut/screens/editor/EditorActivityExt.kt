package com.ecomobile.v9kut.screens.editor

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.result.ActivityResult
import androidx.appcompat.app.AppCompatActivity.RESULT_OK
import androidx.core.net.toUri
import com.ecomobile.base.extension.gone
import com.ecomobile.base.extension.invisible
import com.ecomobile.base.extension.isActive
import com.ecomobile.base.extension.slideDown
import com.ecomobile.base.extension.slideUp
import com.ecomobile.base.extension.visible
import com.ecomobile.base.recyclerview.PercentWidthItemDecoration
import com.ecomobile.v9kut.screens.crop.CropHeaderView
import com.ecomobile.v9kut.screens.editor.adapter.EditorFeatureAdapter
import com.ecomobile.v9kut.screens.editor.model.EditorType
import com.ecomobile.v9kut.screens.photo_cutter.PhotoCutterActivity
import com.ecomobile.v9kut.screens.photo_gallery.PhotoGalleryActivity
import kotlin.math.roundToInt

fun EditorActivity.setupView() {
    intent.getStringExtra(PhotoCutterActivity.IMAGE_PATH)?.let {
        changeBackgroundImage(it)
    }
}

fun EditorActivity.changeBackgroundImage(path: String) {
    binding.image.setImageURI(path.toUri())
    BitmapFactory.decodeFile(path)?.let { bm ->
        binding.cropView.setImageBitmap(bm.copy(Bitmap.Config.ARGB_8888, true))
        bm.recycle()
    }
}

fun EditorActivity.setupRecyclerViewFeature() {
    binding.rcvFeature.apply {
        addItemDecoration(
            PercentWidthItemDecoration(
                context = this@setupRecyclerViewFeature,
                resources.getDimension(com.intuit.sdp.R.dimen._4sdp).roundToInt(),
                resources.getDimension(com.intuit.sdp.R.dimen._12sdp).roundToInt(),
                widthRatio = 0.25f
            )
        )
        adapter = editorFeatureAdapter
    }
}

fun EditorActivity.listener() {
    binding.apply {
        cropHeader.setCallback(object : CropHeaderView.Callback {
            override fun onClose() {
                setShowCropView(false)
            }

            override fun onSave() {
                binding.image.setImageBitmap(binding.cropView.croppedImage)
                setShowCropView(false)
            }
        })
    }
}

fun EditorActivity.openGallery() {
    chooseImageLauncher.launch(Intent(this, PhotoGalleryActivity::class.java))
}

fun EditorActivity.replaceBackground(result: ActivityResult) {
    if (!isActive() || result.resultCode != RESULT_OK) return
    result.data?.getStringExtra(PhotoGalleryActivity.IMAGE_PATH)?.let { path ->
        changeBackgroundImage(path)
    }
}

fun EditorActivity.setShowCropView(isShow: Boolean) {
    binding.apply {
        if (isShow) {
            cropHeader.slideDown(true)
            cropOption.slideUp(true)
            cropView.visible()
            headerBase.slideUp { headerBase.invisible() }
            image.gone()
            rcvFeature.slideDown { rcvFeature.invisible() }
        } else {
            cropHeader.slideUp { cropHeader.invisible() }
            cropOption.slideDown { cropOption.invisible() }
            cropView.gone()
            headerBase.slideDown(true)
            image.visible()
            rcvFeature.slideUp(true)
        }
    }
}