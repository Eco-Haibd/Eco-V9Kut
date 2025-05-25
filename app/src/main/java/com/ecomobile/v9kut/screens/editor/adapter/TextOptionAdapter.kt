package com.ecomobile.v9kut.screens.editor.adapter

import android.content.Context
import com.ecomobile.base.BaseAdapter
import com.ecomobile.v9kut.R
import com.ecomobile.v9kut.databinding.ItemEditorTextOptionBinding
import com.ecomobile.v9kut.screens.editor.model.TextEditorType

class TextOptionAdapter(
    private val context: Context,
    private val list: List<TextEditorType>
): BaseAdapter<TextEditorType, ItemEditorTextOptionBinding>(list){

    override fun getLayoutResId() = R.layout.item_editor_text_option

    override fun bind(binding: ItemEditorTextOptionBinding, item: TextEditorType, position: Int) {

    }
}