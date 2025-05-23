package com.ecomobile.photo_cutter

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Path
import android.graphics.PointF
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import com.ecomobile.photo_cutter.model.CutType
import com.ecomobile.photo_cutter.model.FreedomCutterAnimationMode
import kotlin.math.atan2
import kotlin.math.min

/**
 * Handles touch events for scaling, rotating, translating, and drawing on the image.
 */
class ImageTouchHandler(
    private val context: Context,
    private val view: FreedomCutterImageView,
    private val imageEditor: ImageEditor,
    private val editHistoryManager: EditHistoryManager,
    private val onMatrixChanged: (Matrix) -> Unit,
    private val onDrawPath: (Path, Boolean, Float, Float) -> Unit
) : View.OnTouchListener {

    private var callback: FreedomCutterCallbackImpl? = null
    private var cutType: CutType = CutType.INIT
    private var isDrawing = false
    private var isInterruptedByMultiTouch = false
    private var drawPath = Path()
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var matrix = Matrix()
    private var lastMidPoint = PointF()
    private var startAngle = 0f
    private var currentAngle = 0f
    private var isCenteringFullImage = false
    private var previousMatrix: Matrix? = null
    private var wasDrawing = false
    private var previousType: CutType = CutType.INIT
    private var tempBitmap: Bitmap? = null // Lưu trữ trạng thái bitmap tạm thời

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scaleFactor = detector.scaleFactor
            matrix.postScale(scaleFactor, scaleFactor, detector.focusX, detector.focusY)
            onMatrixChanged(matrix)
            return true
        }
    })

    fun setListener(callback: FreedomCutterCallbackImpl) {
        this.callback = callback
    }

    fun updateMatrix(newMatrix: Matrix) {
        matrix.set(newMatrix)
        previousMatrix = Matrix(newMatrix)
        onMatrixChanged(matrix)
    }

    fun setCutType(type: CutType) {
        cutType = type
        resetDrawing()
    }

    fun isDrawing(): Boolean = isDrawing

    fun getDrawPath(): Path = drawPath

    fun getLastTouchX(): Float = lastTouchX

    fun getLastTouchY(): Float = lastTouchY

    fun getCutType(): CutType = cutType

    fun centerFullImage() {
        if (isCenteringFullImage || view.drawable == null) return

        previousMatrix = Matrix(matrix)
        wasDrawing = isDrawing
        previousType = cutType
        isCenteringFullImage = true
        isDrawing = false

        view.setType(CutType.INIT)

        val drawable = view.drawable
        val imageWidth = drawable.intrinsicWidth.toFloat()
        val imageHeight = drawable.intrinsicHeight.toFloat()
        val viewWidth = view.width.toFloat()
        val viewHeight = view.height.toFloat()

        val scale = min(viewWidth / imageWidth, viewHeight / imageHeight)
        val dx = (viewWidth - imageWidth * scale) / 2
        val dy = (viewHeight - imageHeight * scale) / 2

        val targetMatrix = Matrix()
        targetMatrix.postScale(scale, scale)
        targetMatrix.postTranslate(dx, dy)
        targetMatrix.postRotate(0f, viewWidth / 2, viewHeight / 2)

        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 300
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animation ->
                val fraction = animation.animatedValue as Float
                val currentMatrix = Matrix(previousMatrix ?: matrix)
                val tempCurrentMatrix = Matrix()
                val startValues = FloatArray(9)
                val endValues = FloatArray(9)
                currentMatrix.getValues(startValues)
                targetMatrix.getValues(endValues)

                val interpolatedValues = FloatArray(9)
                for (i in 0 until 9) {
                    interpolatedValues[i] = startValues[i] + (endValues[i] - startValues[i]) * fraction
                }
                tempCurrentMatrix.setValues(interpolatedValues)

                matrix = tempCurrentMatrix
                onMatrixChanged(matrix)
                currentAngle = getAngleFromMatrix()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    matrix.set(targetMatrix)
                    onMatrixChanged(matrix)
                    currentAngle = 0f
                    imageEditor.resetToOriginal()
                    callback?.onCenterModeChanged(FreedomCutterAnimationMode.ENTER)
                }
                override fun onAnimationCancel(animation: Animator) {
                    callback?.onCenterModeChanged(FreedomCutterAnimationMode.ENTER)
                }
            })
            start()
        }
    }

    fun exitCenterFullImage() {
        if (!isCenteringFullImage || previousMatrix == null) return

        val targetMatrix = Matrix(previousMatrix!!)
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 300
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animation ->
                val fraction = animation.animatedValue as Float
                val currentMatrix = Matrix(matrix)
                val tempCurrentMatrix = Matrix()
                val startValues = FloatArray(9)
                val endValues = FloatArray(9)
                currentMatrix.getValues(startValues)
                targetMatrix.getValues(endValues)

                val interpolatedValues = FloatArray(9)
                for (i in 0 until 9) {
                    interpolatedValues[i] = startValues[i] + (endValues[i] - startValues[i]) * fraction
                }
                tempCurrentMatrix.setValues(interpolatedValues)

                matrix = tempCurrentMatrix
                onMatrixChanged(matrix)
                currentAngle = getAngleFromMatrix()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    matrix.set(targetMatrix)
                    onMatrixChanged(matrix)
                    isCenteringFullImage = false
                    isDrawing = wasDrawing
                    view.setType(previousType)
                    tempBitmap?.let { imageEditor.applyAICutBitmap(it) } // Khôi phục trạng thái bitmap
                    previousMatrix = null
                    callback?.onCenterModeChanged(FreedomCutterAnimationMode.EXIT)
                }
                override fun onAnimationCancel(animation: Animator) {
                    isCenteringFullImage = false
                    previousMatrix = null
                    callback?.onCenterModeChanged(FreedomCutterAnimationMode.EXIT)
                }
            })
            start()
        }
    }

    fun isCenteringFullImage(): Boolean {
        return isCenteringFullImage
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        if (isCenteringFullImage) return true
        scaleDetector.onTouchEvent(event)
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                if (event.pointerCount == 1) {
                    isDrawing = true
                    isInterruptedByMultiTouch = false
                    resetDrawing()
                    drawPath.moveTo(event.x, event.y)
                    lastTouchX = event.x
                    lastTouchY = event.y
                    onDrawPath(drawPath, isDrawing, lastTouchX, lastTouchY)
                }
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount == 2) {
                    if (isDrawing) {
                        resetDrawing()
                        isDrawing = false
                        isInterruptedByMultiTouch = true
                        tempBitmap = imageEditor.getOriginalBitmap()?.copy(Bitmap.Config.ARGB_8888, true) // Lưu trạng thái
                        onDrawPath(drawPath, isDrawing, lastTouchX, lastTouchY)
                    }
                    startAngle = getAngle(event)
                    lastMidPoint.set(getMidPoint(event))
                }
            }
            MotionEvent.ACTION_MOVE -> {
                when (event.pointerCount) {
                    1 -> {
                        if (isDrawing && !isInterruptedByMultiTouch && cutType != CutType.AI_CUT) {
                            lastTouchX = event.x
                            lastTouchY = event.y
                            drawPath.lineTo(event.x, event.y)
                            when (cutType) {
                                CutType.REMOVE -> {
                                    imageEditor.erase(drawPath, matrix)
                                    onDrawPath(drawPath, isDrawing, lastTouchX, lastTouchY)
                                }
                                CutType.RESTORE -> {
                                    imageEditor.restore(drawPath, matrix)
                                    onDrawPath(drawPath, isDrawing, lastTouchX, lastTouchY)
                                }
                                else -> {
                                    onDrawPath(drawPath, isDrawing, lastTouchX, lastTouchY)
                                }
                            }
                        }
                    }
                    2 -> {
                        if (isDrawing) {
                            resetDrawing()
                            isDrawing = false
                            isInterruptedByMultiTouch = true
                            tempBitmap = imageEditor.getOriginalBitmap()?.copy(Bitmap.Config.ARGB_8888, true) // Lưu trạng thái
                            onDrawPath(drawPath, isDrawing, lastTouchX, lastTouchY)
                        }
                        val newAngle = getAngle(event)
                        val rotation = newAngle - startAngle
                        currentAngle += rotation
                        val focusX = (event.getX(0) + event.getX(1)) / 2
                        val focusY = (event.getY(0) + event.getY(1)) / 2
                        matrix.postRotate(rotation, focusX, focusY)

                        val currentMidPoint = getMidPoint(event)
                        val dx = currentMidPoint.x - lastMidPoint.x
                        val dy = currentMidPoint.y - lastMidPoint.y
                        matrix.postTranslate(dx, dy)
                        lastMidPoint.set(currentMidPoint)

                        startAngle = newAngle
                        onMatrixChanged(matrix)
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                if (event.pointerCount == 1 && isDrawing && !isInterruptedByMultiTouch) {
                    when (cutType) {
                        CutType.AREA -> {
                            drawPath.close()
                            imageEditor.clipWorkingBitmap(drawPath, matrix)
                            editHistoryManager.saveOperation(
                                cutType,
                                Path(drawPath),
                                imageEditor.getPaintSize(),
                                Matrix(matrix)
                            )
                        }
                        CutType.REMOVE, CutType.RESTORE -> {
                            editHistoryManager.saveOperation(
                                cutType,
                                Path(drawPath),
                                imageEditor.getPaintSize(),
                                Matrix(matrix)
                            )
                        }
                        else -> {}
                    }
                    isDrawing = false
                    onDrawPath(drawPath, isDrawing, lastTouchX, lastTouchY)
                }
                isInterruptedByMultiTouch = false
                tempBitmap?.recycle()
                tempBitmap = null
            }
            MotionEvent.ACTION_POINTER_UP -> {
                if (event.pointerCount == 2) {
                    startAngle = currentAngle
                }
            }
        }
        return true
    }

    private fun resetDrawing() {
        drawPath.reset()
        onDrawPath(drawPath, isDrawing, lastTouchX, lastTouchY)
    }

    private fun getAngle(event: MotionEvent): Float {
        val x1 = event.getX(0)
        val y1 = event.getY(0)
        val x2 = event.getX(1)
        val y2 = event.getY(1)
        return Math.toDegrees(atan2((y2 - y1).toDouble(), (x2 - x1).toDouble())).toFloat()
    }

    private fun getMidPoint(event: MotionEvent): PointF {
        val x = (event.getX(0) + event.getX(1)) / 2
        val y = (event.getY(0) + event.getY(1)) / 2
        return PointF(x, y)
    }

    private fun getAngleFromMatrix(): Float {
        val values = FloatArray(9)
        matrix.getValues(values)
        return Math.toDegrees(atan2(values[Matrix.MSKEW_Y].toDouble(), values[Matrix.MSCALE_Y].toDouble())).toFloat()
    }
}