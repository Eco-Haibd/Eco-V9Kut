package com.ecomobile.v9kut.screens.editor

import android.view.View
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import com.ecomobile.base.BaseActivity
import com.ecomobile.v9kut.R
import com.ecomobile.v9kut.databinding.ActivityEditorBinding
import com.ecomobile.v9kut.screens.editor.adapter.EditorFeatureAdapter
import com.ecomobile.v9kut.screens.editor.model.EditorType

class EditorActivity: BaseActivity<ActivityEditorBinding>() {

    companion object {
        const val IMAGE_PATH = "image_path"
    }

    private val items = listOf(
        EditorType.IMAGE,
        EditorType.REPLACE,
        EditorType.STICKER,
        EditorType.TEXT,
        EditorType.CROP
    )

    val chooseImageLauncher = registerForActivityResult(StartActivityForResult()) { result ->
        replaceBackground(result)
    }

    override val layoutResId: Int
        get() = R.layout.activity_editor

    override fun getTopView(): View = binding.header

    override fun onCreate() {
        super.onCreate()
        setupRecyclerViewFeature()
        listener()
    }

    override fun onView() {
        super.onView()
        setupView()
    }

    val editorFeatureAdapter = EditorFeatureAdapter(this, items).apply {
        onItemClick = { feature ->
            binding.apply {
                when(feature) {
                    EditorType.IMAGE -> {}
                    EditorType.REPLACE -> openGallery()
                    EditorType.STICKER -> {}
                    EditorType.TEXT -> {
                    }
                    EditorType.CROP -> setShowCropView(true)
                }
            }
        }
    }
}