package com.ecomobile.v9kut.screens.background

import android.view.View
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import com.ecomobile.base.BaseActivity
import com.ecomobile.base.permission.PermissionUtil
import com.ecomobile.base.storage_data.AppStates
import com.ecomobile.v9kut.R
import com.ecomobile.v9kut.databinding.ActivityBackgroundBinding

class BackgroundActivity: BaseActivity<ActivityBackgroundBinding>() {

    val imagePermissionLauncher = registerForActivityResult(RequestPermission()) {
        if (PermissionUtil.hasPhotoPermission(this)) {
            goToLibrary()
        } else {
            AppStates.putBoolean(PermissionUtil.imagePermission, true)
        }
    }

    val imagePermissionResult = registerForActivityResult(StartActivityForResult()) {
        if (PermissionUtil.hasPhotoPermission(this)) {
            goToLibrary()
        }
    }

    val chooseImageLauncher = registerForActivityResult(StartActivityForResult()) { result ->
        onHaveImage(result)
    }

    override val layoutResId: Int
        get() = R.layout.activity_background

    override fun getTopView(): View = binding.header

    override fun onCreate() {
        super.onCreate()
        setupListener()
    }
}