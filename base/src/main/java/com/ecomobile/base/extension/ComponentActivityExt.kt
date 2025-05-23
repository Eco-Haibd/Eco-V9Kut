package com.ecomobile.base.extension

import android.app.Activity
import androidx.activity.ComponentActivity
import org.koin.android.ext.android.getKoin
import org.koin.androidx.scope.LifecycleScopeDelegate
import org.koin.androidx.scope.activityScope

fun ComponentActivity.contextAwareActivityScope() = runCatching {
    LifecycleScopeDelegate<Activity>(
        lifecycleOwner = this,
        koin = getKoin()
    )
}.getOrElse { activityScope() }

//fun ComponentActivity.getKoin(): Koin {
//    return if (this is KoinComponent) {
//        getKoin()
//    } else {
//        GlobalContext.getOrNull() ?: startKoin {
//            androidContext(applicationContext)
//            modules(moduleList)
//        }.koin
//    }
//}