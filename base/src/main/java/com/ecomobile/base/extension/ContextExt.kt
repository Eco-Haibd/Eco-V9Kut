package com.ecomobile.base.extension

import android.content.Context
import android.content.Intent
import android.graphics.Point
import android.net.Uri
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Toast
import com.ecomobile.base.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun Context.isNavigationBarShown(): Boolean {
    val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val windowInsets = windowManager.currentWindowMetrics.windowInsets
        val navBarInsets = windowInsets.getInsets(WindowInsets.Type.navigationBars())
        navBarInsets.bottom > 0 || navBarInsets.top > 0 || navBarInsets.left > 0 || navBarInsets.right > 0
    } else {
        val realPoint = Point()
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.apply {
            getRealSize(realPoint)
            getMetrics(metrics)
        }
        metrics.heightPixels + metrics.widthPixels != realPoint.x + realPoint.y
    }
}

inline fun <reified T> Context.getApplication(callback: (T) -> Unit) {
    if (applicationContext is T) callback.invoke(applicationContext as T)
}

suspend fun Context.withContextMain(block: () -> Unit) {
    withContext(Dispatchers.Main) { block() }
}

private fun Context.getSharedPreferences() =
    getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)

fun Context.getBoolean(key: String, defaultValue: Boolean = false) =
    getSharedPreferences().getBoolean(key, defaultValue)

fun Context.getInt(key: String, defaultValue: Int = -1) =
    getSharedPreferences().getInt(key, defaultValue)

fun Context.getFloat(key: String, defaultValue: Float = -1f) =
    getSharedPreferences().getFloat(key, defaultValue)

fun Context.getLong(key: String, defaultValue: Long = -1) =
    getSharedPreferences().getLong(key, defaultValue)

fun Context.getString(key: String, defaultValue: String = "") =
    getSharedPreferences().getString(key, defaultValue)

fun Context.putExtra(key: String, value: Any) {
    val editor = getSharedPreferences().edit()
    when (value) {
        is Boolean -> editor.putBoolean(key, value)
        is Int -> editor.putInt(key, value)
        is String -> editor.putString(key, value)
        is Float -> editor.putFloat(key, value)
        is Long -> editor.putLong(key, value)
    }
    editor.apply()
}

fun Context.openUrl(url: String) {
    runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }.getOrElse {
        Toast.makeText(this, getString(R.string.can_not_open_url), Toast.LENGTH_LONG).show()
    }
}