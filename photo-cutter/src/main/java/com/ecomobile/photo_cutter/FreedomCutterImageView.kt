package com.ecomobile.photo_cutter

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.graphics.toRect
import com.ecomobile.photo_cutter.model.CutType
import kotlin.math.max
import kotlin.math.min

/**
 * Custom ImageView for displaying and rendering the image being edited.
 * Delegates touch handling, image editing, history management, and AI cutting to separate components.
 */
@SuppressLint("ClickableViewAccessibility")
class FreedomCutterImageView(context: Context, attrs: AttributeSet?) : AppCompatImageView(context, attrs) {

    private var callback: FreedomCutterCallbackImpl? = null
    private lateinit var touchHandler: ImageTouchHandler
    private lateinit var editHistoryManager: EditHistoryManager
    private lateinit var imageEditor: ImageEditor
    private lateinit var aiCutProcessor: AICutProcessor

    private var workingBitmap: Bitmap? = null
    private var background: Bitmap? = null
    private var touchBitmap: Bitmap? = null
    private var matrix = Matrix()

    private val areaPaint = Paint().apply {
        color = Color.parseColor("#616EFF")
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 4f
        pathEffect = DashPathEffect(floatArrayOf(20f, 20f), 0f)
    }

    private val circlePaint = Paint().apply {
        color = Color.BLUE
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val alphaPaint = Paint().apply {
        alpha = (0.3f * 255).toInt()
    }

    init {
        scaleType = ScaleType.MATRIX
        initializeComponents()
        setOnTouchListener(touchHandler)
        post { centerImage() }
    }

    private fun initializeComponents() {
        imageEditor = ImageEditor(context, ::updateBitmap)
        editHistoryManager = EditHistoryManager(::onVersionChanged)
        touchHandler = ImageTouchHandler(context, this, imageEditor, editHistoryManager, ::onMatrixChanged, ::onDrawPath)
        aiCutProcessor = AICutProcessor(context, ::onAICutCompleted)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        background = createCheckerboardBitmap(w, h)
        centerImage()
    }

    /**
     * Sets the callback for UI updates and events.
     */
    fun setListener(callback: FreedomCutterCallbackImpl) {
        this.callback = callback
        touchHandler.setListener(callback)
    }

    /**
     * Sets the current editing mode (e.g., AREA, REMOVE, RESTORE).
     */
    fun setType(type: CutType) {
        touchHandler.setCutType(type)
        circlePaint.color = when (type) {
            CutType.REMOVE -> Color.RED
            CutType.AI_CUT -> Color.GREEN
            else -> Color.BLUE
        }
        invalidate()
        callback?.onChangeType(type)
    }

    /**
     * Sets the paint size for editing operations.
     */
    fun setSize(size: Float) {
        imageEditor.setPaintSize(size)
        invalidate()
    }

    /**
     * Loads an image from a file path.
     */
    fun replaceImageFromStorage(imagePath: String) {
        imageEditor.replaceImageFromStorage(imagePath)
        editHistoryManager.clear()
        editHistoryManager.saveOperation(CutType.INIT, matrix = Matrix(matrix))
        centerImage()
    }

    /**
     * Loads an image from a URI.
     */
    fun replaceImageFromStorage(uri: Uri) {
        imageEditor.replaceImageFromUri(uri)
        editHistoryManager.clear()
        editHistoryManager.saveOperation(CutType.INIT, matrix = Matrix(matrix))
        centerImage()
    }

    /**
     * Initiates AI-based cutting using ML Kit.
     */
    fun cutByAI() {
        aiCutProcessor.cutByAI(imageEditor.getOriginalBitmap(), imageEditor.getImageUri(), imageEditor.getImagePath())
    }

    fun isCenteringFullImage(): Boolean {
        return touchHandler.isCenteringFullImage()
    }

    /**
     * Centers the image with animation.
     */
    fun centerFullImage() {
        touchHandler.centerFullImage()
    }

    /**
     * Exits center mode with animation.
     */
    fun exitCenterFullImage() {
        touchHandler.exitCenterFullImage()
    }

    /**
     * Crops the image using the provided rectangle.
     */
    fun cropImage(rect: Rect) {
        imageEditor.cropImage(rect) { success, error ->
            if (success) {
                editHistoryManager.saveOperation(CutType.CROP, cropRect = rect, matrix = Matrix(matrix))
            }
            callback?.onCropCompleted(success, error)
        }
    }

    /**
     * Undoes the last operation.
     */
    fun prev() {
        editHistoryManager.undo()
    }

    /**
     * Redoes the last undone operation.
     */
    fun next() {
        editHistoryManager.redo()
    }

    /**
     * Returns a bitmap containing only non-transparent pixels.
     */
    fun getNonTransparentBitmap(): Bitmap? {
        return imageEditor.getNonTransparentBitmap()
    }

    private fun updateBitmap(bitmap: Bitmap?) {
        safeRecycle(workingBitmap)
        workingBitmap = bitmap?.copy(Bitmap.Config.ARGB_8888, true)
        setImageBitmap(workingBitmap)
        invalidate()
    }

    private fun onMatrixChanged(newMatrix: Matrix) {
        matrix.set(newMatrix)
        imageMatrix = matrix
        invalidate()
    }

    private fun onDrawPath(path: Path, isDrawing: Boolean, lastTouchX: Float, lastTouchY: Float) {
        if (isDrawing && touchHandler.getCutType() != CutType.AREA) {
            safeRecycle(touchBitmap)
            touchBitmap = createTouchAreaBitmap(lastTouchX, lastTouchY)
            val rect = RectF(
                lastTouchX - imageEditor.getPaintSize(),
                lastTouchY - imageEditor.getPaintSize(),
                lastTouchX + imageEditor.getPaintSize(),
                lastTouchY + imageEditor.getPaintSize()
            )
            invalidate(rect.toRect())
        } else {
            invalidate()
        }
    }

    private fun onVersionChanged(currentVersion: Int, totalVersions: Int) {
        callback?.onChangeVersion(currentVersion, totalVersions)
        imageEditor.reapplyOperations(editHistoryManager.getOperations(), currentVersion)
    }

    private fun onAICutCompleted(success: Boolean, bitmap: Bitmap?, error: String?) {
        if (success && bitmap != null) {
            imageEditor.applyAICutBitmap(bitmap)
            editHistoryManager.saveOperation(CutType.AI_CUT, matrix = Matrix(matrix))
        }
        callback?.onAICutCompleted(success, error)
    }

    override fun onDraw(canvas: Canvas) {
        safeRecycle(touchBitmap)
        touchBitmap = null
        background?.let { canvas.drawBitmap(it, 0f, 0f, null) }
        if (touchHandler.getCutType() == CutType.RESTORE) {
            imageEditor.getOriginalBitmap()?.let { bitmap ->
                canvas.drawBitmap(bitmap, matrix, alphaPaint)
            }
        }
        super.onDraw(canvas)
        when (touchHandler.getCutType()) {
            CutType.AREA, CutType.AI_CUT -> {
                if (touchHandler.isDrawing()) {
                    canvas.drawPath(touchHandler.getDrawPath(), areaPaint)
                }
            }
            CutType.REMOVE, CutType.RESTORE -> {
                if (touchHandler.isDrawing()) {
                    canvas.drawCircle(
                        touchHandler.getLastTouchX(),
                        touchHandler.getLastTouchY(),
                        (imageEditor.getPaintSize() / 2f) * getMatrixScale(),
                        circlePaint
                    )
                    touchBitmap = createTouchAreaBitmap(touchHandler.getLastTouchX(), touchHandler.getLastTouchY())
                }
            }
            else -> {}
        }
        touchBitmap?.let { canvas.drawBitmap(it, 0f, 0f, null) }
    }

    private fun centerImage() {
        val drawable = drawable ?: return
        val imageWidth = drawable.intrinsicWidth.toFloat()
        val imageHeight = drawable.intrinsicHeight.toFloat()
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()

        val scale = min(viewWidth * 0.9f / imageWidth, viewHeight * 0.9f / imageHeight)
        val dx = (viewWidth - imageWidth * scale) / 2
        val dy = (viewHeight - imageHeight * scale) / 2

        matrix.reset()
        matrix.postScale(scale, scale)
        matrix.postTranslate(dx, dy)
        imageMatrix = matrix
        touchHandler.updateMatrix(matrix)
    }

    private fun createTouchAreaBitmap(touchX: Float, touchY: Float): Bitmap {
        val bitmapSize = 200
        val touchBitmap = Bitmap.createBitmap(bitmapSize, bitmapSize, Bitmap.Config.ARGB_8888)
        val touchCanvas = Canvas(touchBitmap)
        val invertedMatrix = Matrix()
        if (!matrix.invert(invertedMatrix)) {
            return touchBitmap
        }
        val touchPoint = floatArrayOf(touchX, touchY)
        invertedMatrix.mapPoints(touchPoint)
        val bitmapX = touchPoint[0]
        val bitmapY = touchPoint[1]
        val halfSize = bitmapSize / 2f
        val srcLeft = bitmapX - halfSize
        val srcTop = bitmapY - halfSize
        val srcRight = bitmapX + halfSize
        val srcBottom = bitmapY + halfSize

        val dstRect = RectF(0f, 0f, bitmapSize.toFloat(), bitmapSize.toFloat())

        workingBitmap?.let { bitmap ->
            val srcRect = Rect(
                maxOf(0, srcLeft.toInt()).coerceAtMost(bitmap.width),
                maxOf(0, srcTop.toInt()).coerceAtMost(bitmap.height),
                minOf(bitmap.width, srcRight.toInt()).coerceAtMost(bitmap.width),
                minOf(bitmap.height, srcBottom.toInt()).coerceAtMost(bitmap.height)
            )
            touchCanvas.drawBitmap(bitmap, srcRect, dstRect, null)
        }

        touchCanvas.drawCircle(
            bitmapSize / 2f,
            bitmapSize / 2f,
            imageEditor.getPaintSize() / 2,
            circlePaint
        )
        return touchBitmap
    }

    private fun createCheckerboardBitmap(width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()
        val size = 5
        for (x in 0 until width step size) {
            for (y in 0 until height step size) {
                paint.color = if ((x / size + y / size) % 2 == 0) Color.LTGRAY else Color.WHITE
                canvas.drawRect(x.toFloat(), y.toFloat(), (x + size).toFloat(), (y + size).toFloat(), paint)
            }
        }
        return bitmap
    }

    private fun getMatrixScale(): Float {
        val values = FloatArray(9)
        matrix.getValues(values)
        return max(values[Matrix.MSCALE_X], values[Matrix.MSCALE_Y])
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        safeRecycle(workingBitmap, background, touchBitmap)
        workingBitmap = null
        background = null
        touchBitmap = null
        callback = null
    }

    private fun safeRecycle(vararg bitmaps: Bitmap?) {
        bitmaps.forEach { bitmap ->
            bitmap?.takeIf { !it.isRecycled }?.recycle()
        }
    }
}