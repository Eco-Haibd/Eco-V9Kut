package com.ecomobile.base.extension

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun runDelay(time: Long, callback: () -> Unit) {
    CoroutineScope(Dispatchers.IO).launch {
        delay(time)
        CoroutineScope(Dispatchers.Main).launch {
            callback()
        }
    }
}