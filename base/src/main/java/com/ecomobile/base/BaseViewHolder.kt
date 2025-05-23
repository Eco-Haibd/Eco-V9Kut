package com.ecomobile.base

import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView

abstract class BaseViewHolder<B : ViewDataBinding, M>(val binding: B) :
    RecyclerView.ViewHolder(binding.root) {
    abstract fun onBind(model: M)
}