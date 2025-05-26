package com.ecomobile.v9kut.screens.editor

import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import com.ecomobile.base.BaseActivity
import com.ecomobile.sticker.Sticker
import com.ecomobile.sticker.StickerView
import com.ecomobile.sticker.TextSticker
import com.ecomobile.v9kut.R
import com.ecomobile.v9kut.databinding.ActivityEditorBinding
import com.ecomobile.v9kut.screens.editor.adapter.CropOptionAdapter
import com.ecomobile.v9kut.screens.editor.adapter.EditorFeatureAdapter
import com.ecomobile.v9kut.screens.editor.adapter.TextColorAdapter
import com.ecomobile.v9kut.screens.editor.adapter.TextOptionAdapter
import com.ecomobile.v9kut.screens.editor.model.TextColorOption

class EditorActivity: BaseActivity<ActivityEditorBinding>() {

    companion object {
        const val IMAGE_PATH = "image_path"
    }

    val replaceImageLauncher = registerForActivityResult(StartActivityForResult()) { result ->
        replaceBackground(result)
    }

    var editorAdapter: EditorFeatureAdapter? = null
    var textOptionAdapter: TextOptionAdapter? = null
    var cropOptionAdapter: CropOptionAdapter? = null
    var textColorOptionAdapter: TextColorAdapter? = null

    val imm by lazy { getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager }

    var textOptionPosition = -1
    var colorOptionPosition = -1

    var textColorList: List<TextColorOption> = emptyList()
    var actionBottomType: ActionBottomType = ActionBottomType.NORMAL

    override val layoutResId: Int
        get() = R.layout.activity_editor

    override fun getTopView(): View = binding.header

    override fun onCreate() {
        super.onCreate()
        setupRecyclerViewFeature()
    }

    override fun onView() {
        super.onView()
        setupView()
        listener()
    }

    enum class ActionBottomType {
        NORMAL, CROP, TEXT, STICKER
    }

    inner class StickerOperationListener: StickerView.OnStickerOperationListener {
        override fun onStickerAdded(sticker: Sticker) {
        }

        override fun onStickerClicked(sticker: Sticker) {
            when (sticker) {
                is TextSticker -> {
                    showTextOption(true)
                }
            }
        }

        override fun onStickerDeleted(sticker: Sticker) {
            when (sticker) {
                is TextSticker -> {
                    showTextOption(false)
                }
            }
        }

        override fun onStickerDragFinished(sticker: Sticker) {
        }

        override fun onStickerTouchedDown(sticker: Sticker) {
        }

        override fun onStickerZoomFinished(sticker: Sticker) {
        }

        override fun onStickerFlipped(sticker: Sticker) {
        }

        override fun onStickerDoubleTapped(sticker: Sticker) {
        }
    }
}