package com.ecomobile.base.extension

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator

fun View.visible() {
    this.visibility = View.VISIBLE
}

fun View.invisible() {
    this.visibility = View.INVISIBLE
}

fun View.gone() {
    this.visibility = View.GONE
}

fun View.setMargin(left: Int? = null, top: Int? = null, right: Int? = null, bottom: Int? = null) {
    if (layoutParams is ViewGroup.MarginLayoutParams) {
        val p = layoutParams as ViewGroup.MarginLayoutParams
        val l = left ?: p.leftMargin
        val t = top ?: p.topMargin
        val r = right ?: p.rightMargin
        val b = bottom ?: p.bottomMargin
        p.setMargins(l, t, r, b)
        layoutParams = p
        requestLayout()
    }
}

fun View.click(result: ((View) -> Unit)) {
    setOnClickListener {
        isEnabled = false
        result(this)
        kotlin.runCatching { postDelayed({ isEnabled = true }, 300) }
    }
}

fun View.slideDown(isShow: Boolean = false, duration: Long = 200L, action: () -> Unit = {}) {
    clearAnimation()
    visible()
    val startY = if (isShow) -height.toFloat() else 0f
    val endY = if (isShow) 0f else height.toFloat()
    val transitionY = ObjectAnimator.ofFloat(this, View.TRANSLATION_Y, startY, endY).apply {
        this.duration = duration
        interpolator = AccelerateDecelerateInterpolator()
    }
    AnimatorSet().apply {
        play(transitionY)
        addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                action()
            }

            override fun onAnimationCancel(animation: Animator) {
                super.onAnimationCancel(animation)
                action()
            }
        })
        start()
    }
}

fun View.slideUp(isShow: Boolean = false, duration: Long = 200L, action: () -> Unit = {}) {
    clearAnimation()
    visible()
    val startY = if (isShow) height.toFloat() else 0f
    val endY = if (isShow) 0f else -height.toFloat()
    val transitionY = ObjectAnimator.ofFloat(this, View.TRANSLATION_Y, startY, endY).apply {
        this.duration = duration
        interpolator = AccelerateDecelerateInterpolator()
    }
    AnimatorSet().apply {
        play(transitionY)
        addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                action()
            }

            override fun onAnimationCancel(animation: Animator) {
                super.onAnimationCancel(animation)
                action()
            }
        })
        start()
    }
}