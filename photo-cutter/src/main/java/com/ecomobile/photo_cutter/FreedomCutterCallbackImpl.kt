package com.ecomobile.photo_cutter

import com.ecomobile.photo_cutter.model.CutType
import com.ecomobile.photo_cutter.model.FreedomCutterAnimationMode

interface FreedomCutterCallbackImpl {
    fun onChangeType(type: CutType)
    fun onChangeVersion(currentVersion: Int, totalVersions: Int)
    fun onAICutCompleted(success: Boolean, error: String? = null)
    fun onCropCompleted(success: Boolean, error: String? = null)
    fun onCenterModeChanged(mode: FreedomCutterAnimationMode)
}