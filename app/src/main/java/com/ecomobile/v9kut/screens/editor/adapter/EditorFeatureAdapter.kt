package com.ecomobile.v9kut.screens.editor.adapter

import android.content.Context
import androidx.core.content.ContextCompat
import com.ecomobile.base.BaseAdapter
import com.ecomobile.base.extension.click
import com.ecomobile.v9kut.R
import com.ecomobile.v9kut.databinding.ItemEditorFeatureBinding
import com.ecomobile.v9kut.screens.editor.model.EditorOption

class EditorFeatureAdapter(
    private val context: Context,
    private val list: List<EditorOption>
): BaseAdapter<EditorOption, ItemEditorFeatureBinding>(list) {

    private var currentPosition = -1
    private var mapOfTitle = mapOf<EditorOption, String>()
    private var mapOfIconRes = mapOf<EditorOption, Int>()
    var onItemClick : ((EditorOption) -> Unit)? = null

    init {
        currentPosition = 0
        initData()
    }

    private fun initData() {
        mapOfTitle = mapOf(
            EditorOption.IMAGE to context.getString(R.string.add_image),
            EditorOption.REPLACE to context.getString(R.string.replace),
            EditorOption.STICKER to context.getString(R.string.add_sticker),
            EditorOption.TEXT to context.getString(R.string.add_text),
            EditorOption.CROP to context.getString(R.string.crop)
        )
        mapOfIconRes = mapOf(
            EditorOption.IMAGE to R.drawable.ic_editor_type_image,
            EditorOption.REPLACE to R.drawable.ic_editor_type_replace,
            EditorOption.STICKER to R.drawable.ic_editor_type_sticker,
            EditorOption.TEXT to R.drawable.ic_editor_type_text,
            EditorOption.CROP to R.drawable.ic_editor_type_crop
        )
    }

    override fun getLayoutResId() = R.layout.item_editor_feature

    override fun bind(binding: ItemEditorFeatureBinding, item: EditorOption, position: Int) {
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
                currentPosition = if (!arrayListOf(EditorOption.REPLACE, EditorOption.CROP).contains(item)) {
                    position
                } else {
                    -1
                }
                notifyItemChanged(oldPosition, "payload")
                notifyItemChanged(currentPosition, "payload")
                onItemClick?.invoke(list[position])
            }
        }
    }

    override fun bind(binding: ItemEditorFeatureBinding, item: EditorOption, position: Int, payloads: MutableList<Any>) {
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