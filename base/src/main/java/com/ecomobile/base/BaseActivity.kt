package com.ecomobile.base

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.MutableLiveData
import com.ecomobile.base.databinding.LayoutLoadingBinding
import com.ecomobile.base.extension.consumeSystemBars
import com.ecomobile.base.extension.contextAwareActivityScope
import com.ecomobile.base.extension.hideBottomNavigationBar
import com.ecomobile.base.extension.isActive
import com.ecomobile.base.extension.lockPortraitScreen
import com.ecomobile.base.extension.makeSpacingTop
import org.koin.android.scope.AndroidScopeComponent
import org.koin.androidx.fragment.android.setupKoinFragmentFactory
import org.koin.core.scope.Scope

abstract class BaseActivity<B : ViewDataBinding>: AppCompatActivity(), AndroidScopeComponent {

    val binding: B by lazy {
        DataBindingUtil.setContentView<B>(this, layoutResId).apply {
            lifecycleOwner = this@BaseActivity
        }
    }

    override val scope: Scope by contextAwareActivityScope()

    abstract val layoutResId: Int

    open fun onCreate() {}

    open fun onView() {}

    open fun isLockPortraitScreen() = true

    open fun getTopView(): View = binding.root

    val insetsLiveData = MutableLiveData<WindowInsetsCompat>()

    var inForeground = false

    val layoutLoadingBinding by lazy { LayoutLoadingBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(null)
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
        binding.root.post { if (isActive()) onView() }
        setupKoinFragmentFactory(scope)
        consumeSystemBars()
        lockPortraitScreen()
        makeSpacingTop()
        onCreate()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        hideBottomNavigationBar(binding.root)
    }

    override fun onResume() {
        super.onResume()
        inForeground = true
        hideBottomNavigationBar(binding.root)
    }

    override fun onPause() {
        super.onPause()
        inForeground = false
    }

    override fun onDestroy() {
        binding.unbind()
        super.onDestroy()
        scope.close()
    }

    open fun setSystemBarStyle(lightMode: Boolean) {
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars =
            lightMode
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightNavigationBars =
            lightMode
    }

    open fun hideSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}