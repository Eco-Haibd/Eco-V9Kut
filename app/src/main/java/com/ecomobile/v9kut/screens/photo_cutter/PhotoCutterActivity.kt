package com.ecomobile.v9kut.screens.photo_cutter

import android.view.View
import com.ecomobile.base.BaseActivity
import com.ecomobile.v9kut.R
import com.ecomobile.v9kut.databinding.ActivityPhotoCutterBinding
import com.ecomobile.v9kut.screens.editor.adapter.CropOptionAdapter

class PhotoCutterActivity: BaseActivity<ActivityPhotoCutterBinding>() {

    companion object {
        const val IMAGE_PATH = "image_path"
    }

    var imageWidth = 0
    var imageHeight = 0

    var cropOptionAdapter: CropOptionAdapter? = null

    override val layoutResId: Int
        get() = R.layout.activity_photo_cutter

    override fun getTopView(): View = binding.header

    override fun onCreate() {
        super.onCreate()
    }

    override fun onView() {
        super.onView()
        setupView()
        setupListener()
    }
}