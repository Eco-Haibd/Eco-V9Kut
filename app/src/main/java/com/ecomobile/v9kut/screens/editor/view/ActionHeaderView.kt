package com.ecomobile.v9kut.screens.editor.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.ecomobile.v9kut.databinding.ViewActionHeaderBinding

class ActionHeaderView(
    context: Context,
    attrs: AttributeSet?
) : ConstraintLayout(context, attrs) {

    private val binding = ViewActionHeaderBinding.inflate(LayoutInflater.from(context), this, true)
    private var callback: Callback? = null

    init {
        setupListeners()
    }

    fun setCallback(callback: Callback) {
        this.callback = callback
    }

    fun setTitle(title: String) {
        post {
            binding.tvTitle.text = title
        }
    }

    private fun setupListeners() {
        binding.apply {
            close.setOnClickListener {
                callback?.onClose()
            }

            done.setOnClickListener {
                callback?.onDone()
            }
        }
    }

    interface Callback {
        fun onClose()
        fun onDone()
    }
}