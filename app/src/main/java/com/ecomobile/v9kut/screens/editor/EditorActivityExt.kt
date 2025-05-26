package com.ecomobile.v9kut.screens.editor

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.text.Layout
import android.util.Log
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.ActivityResult
import androidx.appcompat.app.AppCompatActivity.RESULT_OK
import androidx.core.net.toUri
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import com.ecomobile.base.extension.click
import com.ecomobile.base.extension.gone
import com.ecomobile.base.extension.invisible
import com.ecomobile.base.extension.isActive
import com.ecomobile.base.extension.slideDown
import com.ecomobile.base.extension.slideUp
import com.ecomobile.base.extension.visible
import com.ecomobile.base.recyclerview.HorizontalSpacingItemDecoration
import com.ecomobile.base.recyclerview.PercentWidthItemDecoration
import com.ecomobile.sticker.TextSticker
import com.ecomobile.v9kut.R
import com.ecomobile.v9kut.screens.editor.EditorActivity.ActionBottomType
import com.ecomobile.v9kut.screens.editor.adapter.CropOptionAdapter
import com.ecomobile.v9kut.screens.editor.model.CropOption
import com.ecomobile.v9kut.screens.editor.adapter.EditorFeatureAdapter
import com.ecomobile.v9kut.screens.editor.adapter.TextColorAdapter
import com.ecomobile.v9kut.screens.editor.adapter.TextOptionAdapter
import com.ecomobile.v9kut.screens.editor.model.EditorOption
import com.ecomobile.v9kut.screens.editor.model.TextColorOption
import com.ecomobile.v9kut.screens.editor.model.TextEditorOption
import com.ecomobile.v9kut.screens.editor.view.ActionHeaderView
import com.ecomobile.v9kut.screens.photo_cutter.PhotoCutterActivity
import com.ecomobile.v9kut.screens.photo_gallery.PhotoGalleryActivity
import org.checkerframework.checker.units.qual.s
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

        editorAdapter = EditorFeatureAdapter(this@setupRecyclerViewFeature, items).apply {
            adapter = this
        }
    }
    binding.rcvColor.apply {
        textColorOptionAdapter = TextColorAdapter(getColor())
        adapter = textColorOptionAdapter
    }
}

fun EditorActivity.listener() {
    binding.apply {
        imgBack.click {
            finish()
        }
        editorAdapter?.onItemClick = { feature ->
            when (feature) {
                EditorOption.IMAGE -> {}
                EditorOption.REPLACE -> openGallery()
                EditorOption.STICKER -> {}
                EditorOption.TEXT -> {
                    addDefaultText()
                    showKeyboard()
                    showTextOption(true)
                }
                EditorOption.CROP -> showCropOption(true)
            }
        }
        cropOptionAdapter?.onItemClick = { cropOption ->
            setCropOption(cropOption)
        }
        cropActionHeader.setCallback(object : ActionHeaderView.Callback {
            override fun onClose() {
                showCropOption(false)
            }

            override fun onDone() {
                binding.image.setImageBitmap(binding.cropView.croppedImage)
                showCropOption(false)
            }
        })
        textOptionAdapter?.onItemClick = { textOption ->
            when (textOption) {
                TextEditorOption.COLOR -> {
                    showColorOption(true)
                }
                TextEditorOption.FONT -> {
                    showColorOption(false)
                }
                TextEditorOption.BOLD -> {
                    showColorOption(false)
                }
                TextEditorOption.ITALIC -> {
                    showColorOption(false)
                }
                TextEditorOption.UNDERLINE -> {
                    showColorOption(false)
                }
                null -> {
                    showColorOption(false)
                }
            }
        }
        textActionHeader.setCallback(object : ActionHeaderView.Callback {
            override fun onClose() {
                showTextOption(false)
            }

            override fun onDone() {
                showTextOption(false)
            }
        })
        stickerView.setOnStickerOperationListener(StickerOperationListener())
        insetsLiveData.observe(this@listener) { insets ->
            showEditTextView(insets.getInsets(WindowInsetsCompat.Type.ime()).bottom)
        }
        edtText.addTextChangedListener { editable ->
            editable?.let {
                val text = if (it.isEmpty()) " " else it.toString()
                changeCurrentTextSticker(text)
            }
        }
        imgChangeTextDone.click {
            hideKeyboard()
        }
        textColorOptionAdapter?.onItemClick = {
            changeCurrentColorTextSticker(it.mainColor)
        }
    }
}

fun EditorActivity.openGallery() {
    replaceImageLauncher.launch(Intent(this, PhotoGalleryActivity::class.java))
}

fun EditorActivity.replaceBackground(result: ActivityResult) {
    if (!isActive() || result.resultCode != RESULT_OK) return
    result.data?.getStringExtra(PhotoGalleryActivity.IMAGE_PATH)?.let { path ->
        changeBackgroundImage(path)
    }
}

fun EditorActivity.addDefaultText() {
    val sticker = TextSticker(this).apply {
        text = " "
        setTextColor(getColor(R.color.white))
        setTextAlign(Layout.Alignment.ALIGN_CENTER)
        resizeText()
    }
    binding.edtText.setText("")
    binding.stickerView.addSticker(sticker)
}

fun EditorActivity.showKeyboard() {
    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
}

fun EditorActivity.hideKeyboard() {
    imm.hideSoftInputFromWindow(binding.edtText.windowToken, 0)
}

fun EditorActivity.showEditTextView(height: Int) {
    binding.apply {
        if (height > 0 && !layoutInputText.isVisible) {
            keyboardFake.apply {
                layoutParams.height = height
            }
            layoutInputText.visible()
            edtText.requestFocus()
        } else if (height == 0 && layoutInputText.isVisible) {
            layoutInputText.gone()
        }
    }
}

fun EditorActivity.setShowActionView(action: ActionBottomType, isShow: Boolean) {
    val top = when(action) {
        ActionBottomType.CROP -> binding.cropActionHeader
        ActionBottomType.TEXT -> binding.textActionHeader
        ActionBottomType.STICKER -> binding.cropActionHeader
        ActionBottomType.NORMAL -> binding.headerBase
    }
    val bottom = when(action) {
        ActionBottomType.CROP -> binding.cropActionBottom
        ActionBottomType.TEXT -> binding.textActionBottom
        ActionBottomType.STICKER -> binding.cropActionBottom
        ActionBottomType.NORMAL -> binding.rcvFeature
    }
    binding.apply {
        if (isShow && actionBottomType == ActionBottomType.NORMAL) {
            actionBottomType = action
            top.slideDown(true)
            bottom.slideUp(true)
            headerBase.slideUp { headerBase.invisible() }
            rcvFeature.slideDown { rcvFeature.invisible() }
        } else if (!isShow && actionBottomType == action) {
            actionBottomType = ActionBottomType.NORMAL
            top.slideUp { top.invisible() }
            bottom.slideDown { bottom.invisible() }
            headerBase.slideDown(true)
            rcvFeature.slideUp(true)
        }
    }
}

fun EditorActivity.showCropOption(isShow: Boolean) {
    setShowActionView(ActionBottomType.CROP, isShow)
    binding.apply {
        if (isShow) {
            cropView.visible()
            image.gone()
        } else {
            cropView.gone()
            image.visible()
        }
    }
}

fun EditorActivity.showTextOption(isShow: Boolean) {
    setShowActionView(ActionBottomType.TEXT, isShow)
    if (isShow) {
        if (textOptionPosition == -1) textOptionPosition = 0
    } else {
        textOptionPosition = -1
    }
    textOptionAdapter?.setPosition(textOptionPosition)
}

fun EditorActivity.showStickerOption(isShow: Boolean) {

}

fun EditorActivity.showColorOption(isShow: Boolean) {
    binding.apply {
        if (isShow && !rcvColor.isVisible) {
            rcvColor.slideUp(true)
        } else if (!isShow && rcvColor.isVisible) {
            rcvColor.slideDown { rcvColor.invisible() }
        }
        if (isShow) {
            binding.stickerView.currentSticker?.apply {
                if (this !is TextSticker) return
                colorOptionPosition = textColorList.map {
                    Color.parseColor(it.mainColor)
                }.indexOf(this.textColor)
            }
            if (colorOptionPosition == -1) colorOptionPosition = 0
        } else {
            colorOptionPosition = -1
        }
        textColorOptionAdapter?.setPosition(colorOptionPosition)
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

fun EditorActivity.changeCurrentTextSticker(text: String) {
    binding.stickerView.currentSticker?.apply {
        if (this !is TextSticker) return
        setText(text)
        resizeText()
        binding.stickerView.invalidate()
    }
}

fun EditorActivity.changeCurrentColorTextSticker(color: String) {
    binding.stickerView.currentSticker?.apply {
        if (this !is TextSticker) return
        setTextColor(Color.parseColor(color))
        binding.stickerView.invalidate()
    }
}

fun EditorActivity.getColor(): List<TextColorOption> {
    textColorList = listOf(
        TextColorOption("#FFFFFF", "#D3D3D3", "#121212"),
        TextColorOption("#000000", "#6D6D6D", "#FFFFFF"),
        TextColorOption("#4397F0", "#006EE3", "#FFFFFF"),
        TextColorOption("#70C050", "#008F0A", "#FFFFFF"),
        TextColorOption("#F7CA5B", "#D19D03", "#FFFFFF"),
        TextColorOption("#F08C34", "#A95500", "#FFFFFF"),
        TextColorOption("#EB4856", "#8A000C", "#FFFFFF"),
        TextColorOption("#D12C69", "#680027", "#FFFFFF"),
        TextColorOption("#A320BA", "#4A0057", "#FFFFFF"),
        TextColorOption("#EA3323", "#870B00", "#FFFFFF"),
        TextColorOption("#ED858E", "#C73844", "#FFFFFF"),
        TextColorOption("#F8D2D3", "#FFAAAD", "#FFFFFF"),
        TextColorOption("#F9DBB4", "#FFBD66", "#FFFFFF"),
        TextColorOption("#F6C281", "#FFA12B", "#FFFFFF"),
        TextColorOption("#D28E46", "#AC5800", "#FFFFFF"),
        TextColorOption("#996438", "#642D00", "#FFFFFF"),
        TextColorOption("#432323", "#BE5010", "#FFFFFF"),
        TextColorOption("#1D4A29", "#179700", "#FFFFFF"),
        TextColorOption("#262626", "#818181", "#FFFFFF"),
        TextColorOption("#363636", "#ABABAB", "#FFFFFF"),
        TextColorOption("#555555", "#AFAFAF", "#FFFFFF"),
        TextColorOption("#737373", "#414141", "#FFFFFF"),
        TextColorOption("#999999", "#4C4C4C", "#FFFFFF"),
        TextColorOption("#DBDBDB", "#AFAFAF", "#FFFFFF")
    )
    return textColorList
}