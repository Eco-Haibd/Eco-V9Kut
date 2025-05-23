package com.ecomobile.photo_cutter.model

import android.graphics.Matrix
import android.graphics.Path
import android.graphics.Rect

data class EditOperation(
    val type: CutType,
    val path: Path = Path(),
    val paintSize: Float = 0f,
    val matrix: Matrix? = null,
    val cropRect: Rect? = null
)