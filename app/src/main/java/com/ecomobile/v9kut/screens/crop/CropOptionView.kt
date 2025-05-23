package com.ecomobile.v9kut.screens.crop

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.ecomobile.base.recyclerview.PercentWidthItemDecoration
import com.ecomobile.v9kut.databinding.ViewCropOptionBottomBinding
import com.ecomobile.v9kut.screens.crop.adapter.CropOptionAdapter
import com.ecomobile.v9kut.screens.crop.model.CropOption
import kotlin.math.roundToInt

class CropOptionView(
    context: Context,
    attrs: AttributeSet?
) : ConstraintLayout(context, attrs) {

    private val binding = ViewCropOptionBottomBinding.inflate(LayoutInflater.from(context), this, true)

    private val listOption = listOf(
        CropOption.Original,
        CropOption.FreeStyle,
        CropOption.RATIO_1_1,
        CropOption.RATIO_4_5,
        CropOption.RATIO_9_16,
        CropOption.RATIO_9_20,
        CropOption.RATIO_1_2,
        CropOption.RATIO_2_3,
        CropOption.RATIO_3_4,
        CropOption.RATIO_16_9,
        CropOption.RATIO_3_2,
        CropOption.RATIO_4_3,
        CropOption.RATIO_5_4
    )

    private var callback: Callback? = null

    init {
    }

    fun setCallback(callback: Callback) {
        this.callback = callback
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        setupRecyclerView()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        binding.rvOption.apply {
            addItemDecoration(
                PercentWidthItemDecoration(
                    context = context,
                   0,
                    resources.getDimension(com.intuit.sdp.R.dimen._8sdp).roundToInt(),
                    widthRatio = 0.2f
                )
            )
            adapter = CropOptionAdapter(context, listOption).apply {
                onItemClick = {
                    callback?.onCropOptionClick(it)
                }
            }
        }
    }

    interface Callback {
        fun onCropOptionClick(cropOption: CropOption)
    }

}