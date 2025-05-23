package com.ecomobile.v9kut.startup

import android.content.Context
import androidx.startup.Initializer
import com.ecomobile.base.storage_data.AppStates
import com.ecomobile.v9kut.BuildConfig
import com.ecomobile.v9kut.koin.moduleList
import com.google.firebase.FirebaseApp
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.fragment.koin.fragmentFactory
import org.koin.core.context.startKoin

class WorkManagerInitializer : Initializer<String> {

    override fun create(context: Context): String {
        if (BuildConfig.DEBUG) {
            //Test Ads
        }

        FirebaseApp.initializeApp(context.applicationContext)
        AppStates.init(context.applicationContext)
        startKoin {
            fragmentFactory()
            androidContext(context)
            modules(moduleList)
        }

        return ""
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return emptyList()
    }
}
