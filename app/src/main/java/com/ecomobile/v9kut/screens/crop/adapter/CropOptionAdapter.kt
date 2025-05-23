package com.ecomobile.v9kut.screens.crop.adapter

import android.content.Context
import androidx.core.content.ContextCompat
import com.ecomobile.base.BaseAdapter
import com.ecomobile.base.extension.click
import com.ecomobile.v9kut.R
import com.ecomobile.v9kut.databinding.ItemCropOptionBinding
import com.ecomobile.v9kut.screens.crop.model.CropOption

class CropOptionAdapter(
    private val context: Context,
    private val list: List<CropOption>
): BaseAdapter<CropOption, ItemCropOptionBinding>(list) {

    private var currentPosition = -1
    private var mapOfTitle = mapOf<CropOption, String>()
    private var mapOfIconRes = mapOf<CropOption, Int>()
    var onItemClick : ((CropOption) -> Unit)? = null

    init {
        currentPosition = 0
        initData()
    }

    private fun initData() {
        mapOfTitle = mapOf(
            CropOption.Original to context.getString(R.string.original),
            CropOption.FreeStyle to context.getString(R.string.freestyle),
            CropOption.RATIO_1_1 to "1:1",
            CropOption.RATIO_4_5 to "4:5",
            CropOption.RATIO_9_16 to "9:16",
            CropOption.RATIO_9_20 to "9:20",
            CropOption.RATIO_1_2 to "1:2",
            CropOption.RATIO_2_3 to "2:3",
            CropOption.RATIO_3_4 to "3:4",
            CropOption.RATIO_16_9 to "16:9",
            CropOption.RATIO_3_2 to "3:2",
            CropOption.RATIO_4_3 to "4:3",
            CropOption.RATIO_5_4 to "5:4"
        )
        mapOfIconRes = mapOf(
            CropOption.Original to R.drawable.ic_crop_type_original,
            CropOption.FreeStyle to R.drawable.ic_crop_type_freestyle,
            CropOption.RATIO_1_1 to R.drawable.ic_crop_type_ratio_1_1,
            CropOption.RATIO_4_5 to R.drawable.ic_crop_type_ratio_4_5,
            CropOption.RATIO_9_16 to R.drawable.ic_crop_type_ratio_9_16,
            CropOption.RATIO_9_20 to R.drawable.ic_crop_type_ratio_9_20,
            CropOption.RATIO_1_2 to R.drawable.ic_crop_type_ratio_1_2,
            CropOption.RATIO_2_3 to R.drawable.ic_crop_type_ratio_2_3,
            CropOption.RATIO_3_4 to R.drawable.ic_crop_type_ratio_3_4,
            CropOption.RATIO_16_9 to R.drawable.ic_crop_type_ratio_16_9,
            CropOption.RATIO_3_2 to R.drawable.ic_crop_type_ratio_3_2,
            CropOption.RATIO_4_3 to R.drawable.ic_crop_type_ratio_4_3,
            CropOption.RATIO_5_4 to R.drawable.ic_crop_type_ratio_5_4
        )
    }

    override fun getLayoutResId() = R.layout.item_crop_option

    override fun bind(binding: ItemCropOptionBinding, item: CropOption, position: Int) {
        binding.apply {
            imgIcon.setImageResource(mapOfIconRes[item] ?: R.drawable.ic_crop_type_original)
            tvTitle.text = mapOfTitle[item]
            if (currentPosition == position) {
                card.setCardBackgroundColor(ContextCompat.getColor(root.context, R.color.cSecondary))
                imgIcon.setColorFilter(ContextCompat.getColor(root.context, R.color.cPrimary))
                tvTitle.setTextColor(ContextCompat.getColor(root.context, R.color.cPrimary))
                tvTitle.setTextAppearance(R.style.SmallBodySemibold)
            } else {
                card.setCardBackgroundColor(ContextCompat.getColor(root.context, R.color.transparent))
                imgIcon.setColorFilter(ContextCompat.getColor(root.context, R.color.cTextPrimary))
                tvTitle.setTextColor(ContextCompat.getColor(root.context, R.color.cTextPrimary))
                tvTitle.setTextAppearance(R.style.SmallBodyMedium)
            }
            root.click {
                val oldPosition = currentPosition
                currentPosition = position
                notifyItemChanged(oldPosition, "payload")
                notifyItemChanged(currentPosition, "payload")
                onItemClick?.invoke(list[position])
            }
        }
    }

    override fun bind(binding: ItemCropOptionBinding, item: CropOption, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            super.bind(binding, item, position, payloads)
            return
        }
        binding.apply {
            if (currentPosition == position) {
                card.setCardBackgroundColor(ContextCompat.getColor(root.context, R.color.cSecondary))
                imgIcon.setColorFilter(ContextCompat.getColor(root.context, R.color.cPrimary))
                tvTitle.setTextColor(ContextCompat.getColor(root.context, R.color.cPrimary))
            } else {
                card.setCardBackgroundColor(ContextCompat.getColor(root.context, R.color.transparent))
                imgIcon.setColorFilter(ContextCompat.getColor(root.context, R.color.cTextPrimary))
                tvTitle.setTextColor(ContextCompat.getColor(root.context, R.color.cTextPrimary))
            }
        }
    }
}