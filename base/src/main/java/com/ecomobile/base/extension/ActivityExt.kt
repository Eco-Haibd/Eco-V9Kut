package com.ecomobile.base.extension

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.ecomobile.base.storage_data.AppStates
import com.google.android.gms.tasks.Task
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory

@SuppressLint("InternalInsetResource")
fun Activity.getStatusBarHeight(): Int {
    var result = 0
    val resourceId: Int = resources.getIdentifier("status_bar_height", "dimen", "android")
    if (resourceId > 0) {
        result = resources.getDimensionPixelSize(resourceId)
    }
    return (result * 1.2f).toInt()
}

fun Activity.rateInApp() {
    if (AppStates.wasRate) return
    val manager: ReviewManager = ReviewManagerFactory.create(this)
    val request: Task<ReviewInfo> = manager.requestReviewFlow()
    request.addOnCompleteListener {
        if (request.isSuccessful) {
            val reviewInfo: ReviewInfo = request.result
            if (!isFinishing && !isDestroyed) {
                manager.launchReviewFlow(this, reviewInfo)
                AppStates.wasRate = true
            }
        }
    }
}

fun AppCompatActivity.delay(duration: Long = 250, callback: () -> Unit) {
    Handler(Looper.getMainLooper()).postDelayed({
        if (!isFinishing && !isDestroyed) callback.invoke()
    }, duration)
}