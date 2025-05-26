package com.ecomobile.v9kut.screens.editor

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.result.ActivityResult
import androidx.appcompat.app.AppCompatActivity.RESULT_OK
import androidx.core.net.toUri
import com.ecomobile.base.extension.click
import com.ecomobile.base.extension.gone
import com.ecomobile.base.extension.invisible
import com.ecomobile.base.extension.isActive
import com.ecomobile.base.extension.slideDown
import com.ecomobile.base.extension.slideUp
import com.ecomobile.base.extension.visible
import com.ecomobile.base.recyclerview.PercentWidthItemDecoration
import com.ecomobile.v9kut.R
import com.ecomobile.v9kut.screens.editor.adapter.CropOptionAdapter
import com.ecomobile.v9kut.screens.editor.model.CropOption
import com.ecomobile.v9kut.screens.editor.adapter.EditorFeatureAdapter
import com.ecomobile.v9kut.screens.editor.adapter.TextOptionAdapter
import com.ecomobile.v9kut.screens.editor.model.EditorOption
import com.ecomobile.v9kut.screens.editor.model.TextEditorOption
import com.ecomobile.v9kut.screens.editor.view.ActionHeaderView
import com.ecomobile.v9kut.screens.photo_cutter.PhotoCutterActivity
import com.ecomobile.v9kut.screens.photo_gallery.PhotoGalleryActivity
import kotlin.math.roundToInt

fun EditorActivity.setupView() {
    intent.getStringExtra(PhotoCutterActivity.IMAGE_PATH)?.let {
        changeBackgroundImage(it)
    }
    binding.apply {
        textActionHeader.setTitle(getString(R.string.add_text))
        textActionBottom.apply {
            val items = listOf(
                TextEditorOption.COLOR,
                TextEditorOption.FONT,
                TextEditorOption.BOLD,
                TextEditorOption.ITALIC,
                TextEditorOption.UNDERLINE
            )
            textOptionAdapter = TextOptionAdapter(this@setupView, items).apply {
                setAdapter(this)
            }
        }
        cropActionHeader.setTitle(getString(R.string.crop))
        cropActionBottom.apply {
            val listOption = listOf(
                CropOption.Original,
                CropOption.FreeStyle,
                CropOption.RATIO_1_1,
                CropOption.RATIO_4_5,
                CropOption.RATIO_9_16,
                CropOption.RATIO_9_20,
                CropOption.RATIO_1_2,
                CropOption.RATIO_2_3,
                CropOption.RATIO_3_4,
                CropOption.RATIO_16_9,
                CropOption.RATIO_3_2,
                CropOption.RATIO_4_3,
                CropOption.RATIO_5_4
            )
            cropOptionAdapter = CropOptionAdapter(this@setupView, listOption).apply {
                setAdapter(this)
            }
        }
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
    val itemDecoration =  PercentWidthItemDecoration(
        context = this@setupRecyclerViewFeature,
        resources.getDimension(com.intuit.sdp.R.dimen._4sdp).roundToInt(),
        resources.getDimension(com.intuit.sdp.R.dimen._12sdp).roundToInt(),
        widthRatio = 0.25f
    )
    binding.rcvFeature.apply {
        addItemDecoration(itemDecoration)
        val items = listOf(
            EditorOption.IMAGE,
            EditorOption.REPLACE,
            EditorOption.STICKER,
            EditorOption.TEXT,
            EditorOption.CROP
        )

        adapter = EditorFeatureAdapter(this@setupRecyclerViewFeature, items).apply {
            onItemClick = { feature ->
                when(feature) {
                    EditorOption.IMAGE -> {}
                    EditorOption.REPLACE -> openGallery()
                    EditorOption.STICKER -> {}
                    EditorOption.TEXT -> setShowActionView(EditorActivity.ActionBottomType.TEXT, true)
                    EditorOption.CROP -> setShowActionView(EditorActivity.ActionBottomType.CROP, true)
                }
            }
        }
    }
}

fun EditorActivity.listener() {
    binding.apply {
        imgBack.click {
            finish()
        }
        cropOptionAdapter?.onItemClick = { cropOption ->
            setCropOption(cropOption)
        }
        cropActionHeader.setCallback(object : ActionHeaderView.Callback {
            override fun onClose() {
                setShowActionView(EditorActivity.ActionBottomType.CROP, false)
            }

            override fun onDone() {
                binding.image.setImageBitmap(binding.cropView.croppedImage)
                setShowActionView(EditorActivity.ActionBottomType.CROP, false)
            }
        })
        textOptionAdapter?.onItemClick = { textOption ->
            when(textOption) {
                TextEditorOption.COLOR -> {}
                TextEditorOption.FONT -> {}
                TextEditorOption.BOLD -> {}
                TextEditorOption.ITALIC -> {}
                TextEditorOption.UNDERLINE -> {}
            }
        }
        textActionHeader.setCallback(object : ActionHeaderView.Callback {
            override fun onClose() {
                setShowActionView(EditorActivity.ActionBottomType.TEXT, false)
            }

            override fun onDone() {
                setShowActionView(EditorActivity.ActionBottomType.TEXT, false)
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

fun EditorActivity.setShowActionView(action: EditorActivity.ActionBottomType, isShow: Boolean) {
    val top = when(action) {
        EditorActivity.ActionBottomType.CROP -> binding.cropActionHeader
        EditorActivity.ActionBottomType.TEXT -> binding.textActionHeader
        EditorActivity.ActionBottomType.STICKER -> binding.cropActionHeader
    }
    val bottom = when(action) {
        EditorActivity.ActionBottomType.CROP -> binding.cropActionBottom
        EditorActivity.ActionBottomType.TEXT -> binding.textActionBottom
        EditorActivity.ActionBottomType.STICKER -> binding.cropActionBottom
    }
    binding.apply {
        if (isShow) {
            top.slideDown(true)
            bottom.slideUp(true)
            headerBase.slideUp { headerBase.invisible() }
            rcvFeature.slideDown { rcvFeature.invisible() }
            if (action == EditorActivity.ActionBottomType.CROP) {
                cropView.visible()
                image.gone()
            }
        } else {
            top.slideUp { top.invisible() }
            bottom.slideDown { bottom.invisible() }
            headerBase.slideDown(true)
            rcvFeature.slideUp(true)
            if (action == EditorActivity.ActionBottomType.CROP) {
                cropView.gone()
                image.visible()
            }
        }
    }
}

fun EditorActivity.setCropOption(cropOption: CropOption) {
    binding.cropView.apply {
        when (cropOption) {
            CropOption.Original -> setOriginRatio()
            CropOption.FreeStyle -> setFixedAspectRatio(false)
            CropOption.RATIO_1_1 -> setAspectRatio(1, 1)
            CropOption.RATIO_4_5 -> setAspectRatio(4, 5)
            CropOption.RATIO_9_16 -> setAspectRatio(9, 16)
            CropOption.RATIO_9_20 -> setAspectRatio(9, 20)
            CropOption.RATIO_1_2 -> setAspectRatio(1, 2)
            CropOption.RATIO_2_3 -> setAspectRatio(2, 3)
            CropOption.RATIO_3_4 -> setAspectRatio(3, 4)
            CropOption.RATIO_16_9 -> setAspectRatio(16, 9)
            CropOption.RATIO_3_2 -> setAspectRatio(3, 2)
            CropOption.RATIO_4_3 -> setAspectRatio(4, 3)
            CropOption.RATIO_5_4 -> setAspectRatio(5, 4)
        }
    }
}