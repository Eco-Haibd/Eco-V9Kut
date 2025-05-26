package com.ecomobile.v9kut.screens.editor.adapter

import android.content.Context
import androidx.core.content.ContextCompat
import com.ecomobile.base.BaseAdapter
import com.ecomobile.base.extension.click
import com.ecomobile.v9kut.R
import com.ecomobile.v9kut.databinding.ItemEditorTextOptionBinding
import com.ecomobile.v9kut.screens.editor.model.TextEditorOption

class TextOptionAdapter(
    private val context: Context,
    private val list: List<TextEditorOption>
): BaseAdapter<TextEditorOption, ItemEditorTextOptionBinding>(list){

    private var currentPosition = -1
    private var mapOfTitle = mapOf<TextEditorOption, String>()
    private var mapOfIconRes = mapOf<TextEditorOption, Int>()
    var onItemClick : ((TextEditorOption) -> Unit)? = null

    init {
        currentPosition = 0
        initData()
    }

    private fun initData() {
        mapOfTitle = mapOf(
            TextEditorOption.COLOR to context.getString(R.string.color),
            TextEditorOption.FONT to context.getString(R.string.font),
            TextEditorOption.BOLD to context.getString(R.string.bold),
            TextEditorOption.ITALIC to context.getString(R.string.italic),
            TextEditorOption.UNDERLINE to context.getString(R.string.underline),
        )
        mapOfIconRes = mapOf(
            TextEditorOption.COLOR to R.drawable.ic_editor_text_color,
            TextEditorOption.FONT to R.drawable.ic_editor_text_font,
            TextEditorOption.BOLD to R.drawable.ic_editor_text_bold,
            TextEditorOption.ITALIC to R.drawable.ic_editor_text_italic,
            TextEditorOption.UNDERLINE to R.drawable.ic_editor_text_underline,
        )
    }

    override fun getLayoutResId() = R.layout.item_editor_text_option

    override fun bind(binding: ItemEditorTextOptionBinding, item: TextEditorOption, position: Int) {
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
//                currentPosition = if (!arrayListOf(EditorType.REPLACE, EditorType.CROP).contains(item)) {
//                    position
//                } else {
//                    -1
//                }
                notifyItemChanged(oldPosition, "payload")
                notifyItemChanged(currentPosition, "payload")
                onItemClick?.invoke(list[position])
            }
        }
    }

    override fun bind(binding: ItemEditorTextOptionBinding, item: TextEditorOption, position: Int, payloads: MutableList<Any>) {
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