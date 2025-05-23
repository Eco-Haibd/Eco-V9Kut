package com.ecomobile.v9kut.koin

import com.ecomobile.v9kut.viewmodel.ImageViewModel
import org.koin.dsl.module

val appModule = module {
    single { ImageViewModel() }
}