package com.ecomobile.firebase.remote_config

import com.google.firebase.ktx.BuildConfig
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.firebase.remoteconfig.ktx.remoteConfig

object RemoteConfigManager {


    private const val COOL_OF_TIME_AD = "Cool_Of_Time_Ad"

    private val defaults: Map<String, Any> = mapOf(
        COOL_OF_TIME_AD to 10000L
    )

    var isLoadRemoteSuccess = false

    private fun remoteConfig() = Firebase.remoteConfig

    fun fetchRemoteData() {
        remoteConfig().apply {
            val configSetting = FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(if (BuildConfig.DEBUG) 0 else 10)
                .build()
            setConfigSettingsAsync(configSetting)
            setDefaultsAsync(defaults).addOnCompleteListener {
                fetchAndActivate()
                    .addOnCompleteListener {
                        isLoadRemoteSuccess = true
                    }
            }
        }
    }

    fun getCoolOfTime(): Long = remoteConfig().getLong(COOL_OF_TIME_AD)
}