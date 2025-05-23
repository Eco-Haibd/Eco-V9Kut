package com.ecomobile.base

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView

abstract class BaseAdapter<T, VB : ViewDataBinding>(
    items: List<T>
) : RecyclerView.Adapter<BaseAdapter.BaseViewHolder<VB>>() {

    private var items: List<T> = items

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<VB> {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding: VB = DataBindingUtil.inflate(layoutInflater, getLayoutResId(), parent, false)
        return BaseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BaseViewHolder<VB>, position: Int) {
        bind(holder.binding, items[position], position)
        holder.binding.executePendingBindings()
    }

    override fun onBindViewHolder(holder: BaseViewHolder<VB>, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
            return
        }
        bind(holder.binding, items[position], position, payloads)
    }

    override fun getItemCount(): Int = items.size

    @SuppressLint("NotifyDataSetChanged")
    fun update(list: List<T>) {
        items = list
        notifyDataSetChanged()
    }

    abstract fun getLayoutResId(): Int

    abstract fun bind(binding: VB, item: T, position: Int)

    open fun bind(binding: VB, item: T, position: Int, payloads: MutableList<Any>) {}

    class BaseViewHolder<VB : ViewDataBinding>(val binding: VB) :
        RecyclerView.ViewHolder(binding.root)
}