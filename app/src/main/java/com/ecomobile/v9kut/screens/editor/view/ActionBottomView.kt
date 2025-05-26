package com.ecomobile.v9kut.screens.editor.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.ecomobile.base.BaseAdapter
import com.ecomobile.base.recyclerview.PercentWidthItemDecoration
import com.ecomobile.v9kut.databinding.ViewActionBottomBinding
import kotlin.math.roundToInt

class ActionBottomView(
    context: Context,
    attrs: AttributeSet?
) : ConstraintLayout(context, attrs) {

    private val binding = ViewActionBottomBinding.inflate(LayoutInflater.from(context), this, true)

    private var adapter: BaseAdapter<*, *>? = null

    fun setAdapter(adapter: BaseAdapter<*, *>) {
        this.adapter = adapter
        binding.rvOption.apply {
            addItemDecoration(
                PercentWidthItemDecoration(
                    context = context,
                    0,
                    resources.getDimension(com.intuit.sdp.R.dimen._8sdp).roundToInt(),
                    widthRatio = 0.2f
                )
            )
            this.adapter = adapter
        }
    }
}