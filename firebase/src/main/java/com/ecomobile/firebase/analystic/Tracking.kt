package com.ecomobile.firebase.analystic

import android.util.Log
import androidx.core.os.bundleOf
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase

object Tracking {
    private val tracker: FirebaseAnalytics = Firebase.analytics

    fun logEvent(name: String, data: Map<String, String>) {
        val bundle = android.os.Bundle()
        data.forEach { (key, value) ->
            bundle.putString(key, value)
        }
        Log.i("Tracking", " $name $bundle")
        tracker.logEvent(name, bundle)
    }

    fun logEvent(name: String) {
        Log.i("Tracking", " $name")
        tracker.logEvent(name, bundleOf())
    }
}