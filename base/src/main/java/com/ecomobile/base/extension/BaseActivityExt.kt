package com.ecomobile.base.extension

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.ecomobile.base.BaseActivity
import kotlinx.coroutines.delay

fun BaseActivity<*>.isActive() = !isFinishing && !isDestroyed

fun BaseActivity<*>.consumeSystemBars(
    allowPadding: Boolean = true,
    callback: ((statusBarHeight: Int, bottomBarHeight: Int) -> Unit)? = null
) {
    ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { _, insets ->
        val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
        val bottomBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
        if (allowPadding && binding.root is ViewGroup && binding.root.paddingTop + binding.root.paddingBottom != statusBarHeight + bottomBarHeight && isNavigationBarShown()) {
            binding.root.setPadding(0, statusBarHeight, 0, bottomBarHeight)
        }
        callback?.invoke(statusBarHeight, bottomBarHeight)
        WindowInsetsCompat.CONSUMED
    }
}

@SuppressLint("SourceLockedOrientationActivity")
fun BaseActivity<*>.lockPortraitScreen() {
    if (!isLockPortraitScreen()) return
    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
}

fun BaseActivity<*>.hideBottomNavigationBar(viewRoot: View) {
    ViewCompat.setOnApplyWindowInsetsListener(viewRoot) { view, insets ->
        val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        view.setPadding(
            systemBarsInsets.left,
            systemBarsInsets.top,
            systemBarsInsets.right,
            0 // Ensures no upward shift when the bar is hidden
        )
        insets
    }
    WindowCompat.setDecorFitsSystemWindows(window, false)
    WindowCompat.getInsetsController(window, window.decorView).apply {
        systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        hide(WindowInsetsCompat.Type.navigationBars())
    }
}

fun BaseActivity<*>.makeSpacingTop() {
    if (binding.root == getTopView()) return
    getTopView().setMargin(top = getStatusBarHeight())
}

fun BaseActivity<*>.showLoadingView() {
    runOnUiThread {
        layoutLoadingBinding.root.layoutParams = binding.root.layoutParams
        (binding.root as ViewGroup).apply {
            removeView(layoutLoadingBinding.root)
            addView(layoutLoadingBinding.root)
        }
    }
}

fun BaseActivity<*>.hideLoadingView() {
    runOnUiThread {
        delay { (binding.root as ViewGroup).removeView(layoutLoadingBinding.root) }
    }
}