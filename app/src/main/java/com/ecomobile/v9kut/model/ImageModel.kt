package com.ecomobile.v9kut.model

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ImageModel(
    val id: Long,
    val name: String,
    var path: String,
    var uri: Uri
): Parcelable
