package com.ecomobile.v9kut.screens.photo_cutter.adapter

import androidx.core.content.ContextCompat
import com.ecomobile.base.BaseAdapter
import com.ecomobile.base.extension.click
import com.ecomobile.v9kut.R
import com.ecomobile.v9kut.databinding.ItemFeatureBinding
import com.ecomobile.v9kut.screens.photo_cutter.model.CutterFeature

class CutterFeatureAdapter(
    private val list: List<CutterFeature>
): BaseAdapter<CutterFeature, ItemFeatureBinding>(list) {

    private var currentPosition = -1
    var onItemClick : ((CutterFeature) -> Unit)? = null

    init {
        currentPosition = 0
    }

    override fun getLayoutResId() = R.layout.item_feature

    override fun bind(binding: ItemFeatureBinding, item: CutterFeature, position: Int) {
        binding.apply {
            imgIcon.setImageResource(item.icon)
            tvTitle.text = item.title
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

    override fun bind(binding: ItemFeatureBinding, item: CutterFeature, position: Int, payloads: MutableList<Any>) {
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