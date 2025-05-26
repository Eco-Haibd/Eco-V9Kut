package com.ecomobile.v9kut.screens.editor.adapter

import android.graphics.Color
import com.ecomobile.base.BaseAdapter
import com.ecomobile.base.extension.gone
import com.ecomobile.base.extension.visible
import com.ecomobile.v9kut.R
import com.ecomobile.v9kut.databinding.ItemTextColorBinding
import com.ecomobile.v9kut.screens.editor.model.TextColorOption

class TextColorAdapter(
    private val list: List<TextColorOption>
): BaseAdapter<TextColorOption, ItemTextColorBinding>(list) {

    private var currentPosition = -1
    var onItemClick : ((TextColorOption) -> Unit)? = null

    fun setPosition(position: Int) {
        val oldPosition = currentPosition
        currentPosition = position
        notifyItemChanged(currentPosition, "payload")
        if (oldPosition != -1) notifyItemChanged(oldPosition, "payload")
        if (position != -1) onItemClick?.invoke(list[position])
    }

    override fun getLayoutResId() = R.layout.item_text_color

    override fun bind(binding: ItemTextColorBinding, item: TextColorOption, position: Int) {
        binding.apply {
            border.setCardBackgroundColor(Color.parseColor(item.borderColor))
            main.setCardBackgroundColor(Color.parseColor(item.mainColor))
            icon.setColorFilter(Color.parseColor(item.iconColor))
            if (currentPosition == position) {
                border.setCardBackgroundColor(Color.parseColor(item.borderColor))
                icon.visible()
            } else {
                border.setCardBackgroundColor(Color.parseColor(item.mainColor))
                icon.gone()
            }
            root.setOnClickListener {
                val oldPosition = currentPosition
                currentPosition = position
                notifyItemChanged(oldPosition, "payload")
                notifyItemChanged(currentPosition, "payload")
                onItemClick?.invoke(list[position])
            }
        }
    }

    override fun bind(binding: ItemTextColorBinding, item: TextColorOption, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            super.bind(binding, item, position, payloads)
            return
        }
        binding.apply {
            if (currentPosition == position) {
                border.setCardBackgroundColor(Color.parseColor(item.borderColor))
                icon.visible()
            } else {
                border.setCardBackgroundColor(Color.parseColor(item.mainColor))
                icon.gone()
            }
        }
    }
}