package com.ecomobile.base.recyclerview

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class PercentWidthItemDecoration(
    private val context: Context,
    private val spacing: Int,
    private val spacingHorizontal: Int = 0,
    private val widthRatio: Float
): RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        val screenWidth = context.resources.displayMetrics.widthPixels
        val itemWidth = (screenWidth * widthRatio).toInt()

        view.layoutParams.width = itemWidth
        outRect.right = spacing
        if (position == 0) {
            outRect.left = spacingHorizontal
        } else if (position == state.itemCount - 1) {
            outRect.right = spacingHorizontal
        }
    }
}