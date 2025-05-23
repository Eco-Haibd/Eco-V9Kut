package com.ecomobile.v9kut.screens.cutter_success

import android.net.Uri
import android.view.View
import com.ecomobile.base.BaseActivity
import com.ecomobile.v9kut.R
import com.ecomobile.v9kut.databinding.ActivityCutterSuccessBinding

class CutterSuccessActivity: BaseActivity<ActivityCutterSuccessBinding>() {

    companion object {
        const val IMAGE_URI = "IMAGE_URI"
    }

    override val layoutResId: Int
        get() = R.layout.activity_cutter_success

    override fun getTopView(): View = findViewById(R.id.header)

    override fun onView() {
        super.onView()
        val uri = intent.getParcelableExtra(IMAGE_URI) as Uri?
        binding.imageView.setImageURI(uri)
    }
}