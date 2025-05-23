package com.ecomobile.base.extension

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun Fragment.finish() {
    appCompatActivity { activity ->
        activity.finish()
    }
}

fun Fragment.appCompatActivity(callback: (AppCompatActivity) -> Unit) {
    activity?.let {
        if (it is AppCompatActivity) callback(it)
    }
}

inline fun <reified T> Fragment.getParentActivity(callback: (T) -> Unit) {
    activity?.let {
        if (it is T) callback.invoke(it)
    }
}

fun Fragment.lifecycleScopeIO(callback: suspend () -> Unit) {
    lifecycleScope.launch(Dispatchers.IO) { callback() }
}

fun Fragment.lifecycleScopeMain(callback: suspend () -> Unit) {
    lifecycleScope.launch(Dispatchers.Main) { callback() }
}

fun Fragment.lifecycleScopeDefault(callback: suspend () -> Unit) {
    lifecycleScope.launch(Dispatchers.Default) { callback() }
}