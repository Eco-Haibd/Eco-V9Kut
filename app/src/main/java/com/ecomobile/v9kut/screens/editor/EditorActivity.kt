package com.ecomobile.v9kut.screens.editor

import android.view.View
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import com.ecomobile.base.BaseActivity
import com.ecomobile.v9kut.R
import com.ecomobile.v9kut.databinding.ActivityEditorBinding
import com.ecomobile.v9kut.screens.editor.adapter.CropOptionAdapter
import com.ecomobile.v9kut.screens.editor.adapter.TextOptionAdapter

class EditorActivity: BaseActivity<ActivityEditorBinding>() {

    companion object {
        const val IMAGE_PATH = "image_path"
    }

    val chooseImageLauncher = registerForActivityResult(StartActivityForResult()) { result ->
        replaceBackground(result)
    }

    var textOptionAdapter: TextOptionAdapter? = null
    var cropOptionAdapter: CropOptionAdapter? = null

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
        CROP, TEXT, STICKER
    }
}