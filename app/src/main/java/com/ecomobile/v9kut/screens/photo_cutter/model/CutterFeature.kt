package com.ecomobile.v9kut.screens.photo_cutter.model

data class CutterFeature (
    val type: CutterType,
    val icon: Int,
    val title: String
)

enum class CutterType {
    MANUAL_CUT,
    CROP,
    ROTATE,
    ERASER,
    REPAIR
}