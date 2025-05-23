package com.ecomobile.v9kut.screens.crop

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.ecomobile.v9kut.databinding.ViewCropHeaderBinding

class CropHeaderView(
    context: Context,
    attrs: AttributeSet?
) : ConstraintLayout(context, attrs) {

    private val binding = ViewCropHeaderBinding.inflate(LayoutInflater.from(context), this, true)
    private var callback: Callback? = null

    init {
        setupListeners()
    }

    fun setCallback(callback: Callback) {
        this.callback = callback
    }

    private fun setupListeners() {
        binding.apply {
            close.setOnClickListener {
                callback?.onClose()
            }

            save.setOnClickListener {
                callback?.onSave()
            }
        }
    }

    interface Callback {
        fun onClose()
        fun onSave()
    }
}