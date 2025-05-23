package com.ecomobile.photo_cutter

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.net.Uri
import android.provider.MediaStore
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.widget.AppCompatImageView
import com.ecomobile.photo_cutter.model.CutType
import com.ecomobile.photo_cutter.model.EditOperation
import com.ecomobile.photo_cutter.model.FreedomCutterAnimationMode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min

@SuppressLint("ClickableViewAccessibility")
class OldFreedomCutterImageView(context: Context, attrs: AttributeSet?) : AppCompatImageView(context, attrs) {

    companion object {
        private const val TOUCH_BITMAP_SIZE = 400
        private const val MAX_VERSIONS = 20
    }

    private var callback: FreedomCutterCallbackImpl? = null

    private var workingBitmap: Bitmap? = null
    private var workingCanvas: Canvas? = null
    private var background: Bitmap? = null
    private var originalBitmap: Bitmap? = null
    private val versionStack = arrayListOf<EditOperation>()
    private var currentVersion = -1

    private var onHandType: CutType = CutType.INIT
    private var isInterruptedByMultiTouch = false
    private var drawPath: Path = Path()
    private var isDrawing: Boolean = false
    private var touchBitmap: Bitmap? = null
    private var isAICutInProgress = false
    private var cachedAICutBitmap: Bitmap? = null

    private var lastTouchX = 0f
    private var lastTouchY = 0f

    private var imagePath: String? = null
    private var imageUri: Uri? = null

    private val areaPaint = Paint().apply {
        color = Color.parseColor("#616EFF")
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 4f
        pathEffect = DashPathEffect(floatArrayOf(20f, 20f), 0f)
    }

    private val removePaint = Paint().apply {
        isAntiAlias = true
        isDither = true
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = 60f
    }

    private val restorePaint = Paint().apply {
        isAntiAlias = true
        isDither = true
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = 60f
    }

    private val circlePaint = Paint().apply {
        color = if (onHandType == CutType.REMOVE) Color.RED else Color.BLUE
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val alphaPaint = Paint().apply {
        alpha = (0.3f * 255).toInt()
    }

    private var paintSize = 60f

    private var matrix = Matrix()
    private var lastMidPoint = PointF()
    private var startAngle = 0f
    private var currentAngle = 0f

    //Variable to control centering full image
    private var isCenteringFullImage = false
    private var previousMatrix: Matrix? = null
    private var wasDrawing = false
    private var previousType: CutType = CutType.INIT

    private var scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scaleFactor = detector.scaleFactor
            matrix.postScale(scaleFactor, scaleFactor, detector.focusX, detector.focusY)
            imageMatrix = matrix
            invalidate()
            return true
        }
    })

    init {
        setOnTouchListener { _, event ->
            if (isCenteringFullImage || isAICutInProgress) return@setOnTouchListener true
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
                        invalidate()
                    }
                }

                MotionEvent.ACTION_POINTER_DOWN -> {
                    if (event.pointerCount == 2) {
                        if (isDrawing) {
                            resetDrawing()
                            isDrawing = false
                            isInterruptedByMultiTouch = true
                            reapplyOperations()
                            invalidate()
                        }
                        startAngle = getAngle(event)
                        lastMidPoint.set(getMidPoint(event))
                    }
                }

                MotionEvent.ACTION_MOVE -> {
                    when (event.pointerCount) {
                        1 -> {
                            if (isDrawing && !isInterruptedByMultiTouch && onHandType != CutType.AI_CUT) {
                                lastTouchX = event.x
                                lastTouchY = event.y
                                drawPath.lineTo(event.x, event.y)
                                when (onHandType) {
                                    CutType.REMOVE -> {
                                        erase(drawPath, matrix)
                                    }
                                    CutType.RESTORE -> {
                                        restore(drawPath, matrix)
                                    }
                                    else -> {}
                                }
                                invalidate()
                            }
                        }

                        2 -> {
                            if (isDrawing) {
                                resetDrawing()
                                isDrawing = false
                                isInterruptedByMultiTouch = true
                                invalidate()
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
                            imageMatrix = matrix
                            invalidate()
                        }
                    }
                }

                MotionEvent.ACTION_UP -> {
                    if (event.pointerCount == 1 && isDrawing && !isInterruptedByMultiTouch) {
                        when (onHandType) {
                            CutType.AREA -> {
                                drawPath.close()
                                clipWorkingBitmap(matrix)
                                saveState()
                            }
                            CutType.REMOVE -> saveState()
                            CutType.RESTORE -> saveState()
                            else -> {}
                        }
                        isDrawing = false
                        invalidate()
                    }
                    isInterruptedByMultiTouch = false
                }

                MotionEvent.ACTION_POINTER_UP -> {
                    if (event.pointerCount == 2) {
                        startAngle = currentAngle
                    }
                }
            }
            true
        }
        scaleType = ScaleType.MATRIX
        post { centerImage() }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerImage()
    }

    fun setListener(callback: FreedomCutterCallbackImpl) {
        this.callback = callback
    }

    fun setType(type: CutType) {
        onHandType = type
        circlePaint.color = when (type) {
            CutType.REMOVE -> Color.RED
            CutType.AI_CUT -> Color.GREEN
            else -> Color.BLUE
        }
        invalidate()
        callback?.onChangeType(type)
    }

    fun setSize(size: Float) {
        paintSize = size
        removePaint.strokeWidth = size
        restorePaint.strokeWidth = size
    }

    fun replaceImageFromStorage(imagePath: String) {
        this.imagePath = imagePath
        try {
            val bitmap = BitmapFactory.decodeFile(imagePath) ?: throw IllegalArgumentException("FreedomCutter can not load image from path: $imagePath")
            originalBitmap?.recycle()
            workingBitmap?.recycle()
            cachedAICutBitmap?.recycle()
            cachedAICutBitmap = null
            originalBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            workingBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            workingCanvas = Canvas(workingBitmap!!)
            setImageBitmap(workingBitmap)
            background?.recycle()
            background = createCheckerboardBitmap(width, height)
            centerImage()
            resetDrawing()
            versionStack.clear()
            val initOperation = EditOperation(
                type = CutType.INIT,
                path = Path(),
                paintSize = paintSize,
                matrix = Matrix(matrix)
            )
            versionStack.add(initOperation)
            currentVersion = 0
            invalidate()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun replaceImageFromStorage(uri: Uri) {
        this.imageUri = uri
        try {
            val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                ?: throw IllegalArgumentException("FreedomCutter can not load image from URI: $uri")
            originalBitmap?.recycle()
            workingBitmap?.recycle()
            cachedAICutBitmap?.recycle()
            cachedAICutBitmap = null
            originalBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            workingBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            workingCanvas = Canvas(workingBitmap!!)
            setImageBitmap(workingBitmap)
            background?.recycle()
            background = createCheckerboardBitmap(width, height)
            centerImage()
            resetDrawing()
            versionStack.clear()
            currentVersion = 0
            invalidate()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isCenteringFullImage() = isCenteringFullImage

    fun cutByAI() {
        if (isAICutInProgress) return
        isAICutInProgress = true
        centerImage()

        cachedAICutBitmap?.let { maskBitmap ->
            CoroutineScope(Dispatchers.Main).launch {
                applyAICutBitmap(maskBitmap)
                saveAICutState()
                isAICutInProgress = false
                callback?.onAICutCompleted(true)
            }
            return
        }

        val bitmap = when {
            imageUri != null -> {
                try {
                    MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri!!)
                } catch (e: Exception) {
                    callback?.onAICutCompleted(false)
                    isAICutInProgress = false
                    return
                }
            }
            imagePath != null -> {
                try {
                    BitmapFactory.decodeFile(imagePath!!)
                } catch (e: Exception) {
                    callback?.onAICutCompleted(false)
                    isAICutInProgress = false
                    return
                }
            }
            else -> {
                originalBitmap ?: run {
                    callback?.onAICutCompleted(false)
                    isAICutInProgress = false
                    return
                }
            }
        }

        val pixels = IntArray(bitmap.width * bitmap.height)
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val options = SubjectSegmenterOptions.Builder()
            .enableMultipleSubjects(
                SubjectSegmenterOptions.SubjectResultOptions.Builder().enableConfidenceMask().build()
            )
            .build()
        val segmentClient = SubjectSegmentation.getClient(options)

        CoroutineScope(Dispatchers.IO).launch {
            segmentClient.process(inputImage)
                .addOnSuccessListener { result ->
                    val subjects = result.subjects
                    if (subjects.isEmpty()) {
                        callback?.onAICutCompleted(false)
                        isAICutInProgress = false
                        return@addOnSuccessListener
                    }

                    val subject = subjects[0]
                    val mask = subject.confidenceMask ?: run {
                        callback?.onAICutCompleted(false)
                        isAICutInProgress = false
                        return@addOnSuccessListener
                    }

                    val colors = IntArray(inputImage.width * inputImage.height) { 0 }
                    for (j in 0 until subject.height) {
                        for (i in 0 until subject.width) {
                            if (mask.get() > 0.5) {
                                colors[(subject.startY + j) * inputImage.width + subject.startX + i] =
                                    pixels[(subject.startY + j) * bitmap.width + subject.startX + i]
                            }
                        }
                    }
                    val maskBitmap = Bitmap.createBitmap(colors, inputImage.width, inputImage.height, Bitmap.Config.ARGB_8888)
                    CoroutineScope(Dispatchers.Main).launch {
                        cachedAICutBitmap = maskBitmap
                        applyAICutBitmap(maskBitmap)
                        saveAICutState()
                        callback?.onAICutCompleted(true)
                    }
                    isAICutInProgress = false
                }
                .addOnFailureListener {
                    callback?.onAICutCompleted(false)
                    isAICutInProgress = false
                }
        }
    }

    fun centerFullImage() {
        if (isCenteringFullImage || workingBitmap == null) return

        previousMatrix = Matrix(matrix)
        wasDrawing = isDrawing
        previousType = onHandType
        isCenteringFullImage = true
        isDrawing = false

        setType(CutType.INIT)

        val drawable = drawable ?: return
        val imageWidth = drawable.intrinsicWidth.toFloat()
        val imageHeight = drawable.intrinsicHeight.toFloat()
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()

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
                imageMatrix = matrix
                currentAngle = getAngleFromMatrix()
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    matrix.set(targetMatrix)
                    imageMatrix = matrix
                    currentAngle = 0f
                    workingBitmap?.recycle()
                    originalBitmap?.let { srcBitmap ->
                        workingBitmap = srcBitmap.copy(Bitmap.Config.ARGB_8888, true)
                        workingCanvas = Canvas(workingBitmap!!)
                        setImageBitmap(workingBitmap)
                        invalidate()
                    }
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
        if (!isCenteringFullImage || previousMatrix == null) {
            return
        }

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
                imageMatrix = matrix
                currentAngle = getAngleFromMatrix()
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    matrix.set(targetMatrix)
                    imageMatrix = matrix
                    isCenteringFullImage = false
                    isDrawing = wasDrawing
                    setType(previousType)
                    reapplyOperations()
                    previousMatrix = null
                    invalidate()
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

    fun cropImage(rect: Rect) {
        if (isAICutInProgress || workingBitmap == null) return

        workingBitmap?.let { bitmap ->
            val boundedRect = Rect(
                maxOf(0, rect.left),
                maxOf(0, rect.top),
                minOf(bitmap.width, rect.right),
                minOf(bitmap.height, rect.bottom)
            )
            if (boundedRect.width() <= 0 || boundedRect.height() <= 0) {
                callback?.onCropCompleted(false)
                return
            }
            val croppedBitmap = Bitmap.createBitmap(
                bitmap,
                boundedRect.left,
                boundedRect.top,
                boundedRect.width(),
                boundedRect.height()
            )

            val bm = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bm)
            canvas.drawBitmap(croppedBitmap, rect.left.toFloat(), rect.top.toFloat(), null)
            workingBitmap?.recycle()
            workingBitmap = bm.copy(Bitmap.Config.ARGB_8888, true)
            workingCanvas = Canvas(workingBitmap!!)
            setImageBitmap(workingBitmap)

            croppedBitmap.recycle()
            bm.recycle()

            saveCropState(boundedRect)
            resetDrawing()
            invalidate()

            callback?.onCropCompleted(true)
        }
    }

    fun prev() {
        if (currentVersion > 0) {
            currentVersion--
            reapplyOperations()
            invalidate()
            callback?.onChangeVersion(currentVersion, versionStack.size - 1)
        }
    }

    fun next() {
        if (currentVersion < versionStack.size - 1) {
            currentVersion++
            reapplyOperations()
            invalidate()
            callback?.onChangeVersion(currentVersion, versionStack.size - 1)
        }
    }

    fun getNonTransparentBitmap(): Bitmap? {
        workingBitmap?.let { bitmap ->
            val width = bitmap.width
            val height = bitmap.height

            var minX = width
            var minY = height
            var maxX = 0
            var maxY = 0
            var hasNonTransparent = false

            for (x in 0 until width) {
                for (y in 0 until height) {
                    val pixel = bitmap.getPixel(x, y)
                    if (Color.alpha(pixel) > 0) {
                        hasNonTransparent = true
                        minX = minOf(minX, x)
                        minY = minOf(minY, y)
                        maxX = maxOf(maxX, x)
                        maxY = maxOf(maxY, y)
                    }
                }
            }
            if (!hasNonTransparent) return null

            val newWidth = maxX - minX + 1
            val newHeight = maxY - minY + 1

            val resultBitmap = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888)

            for (x in minX..maxX) {
                for (y in minY..maxY) {
                    val pixel = bitmap.getPixel(x, y)
                    if (Color.alpha(pixel) > 0) {
                        resultBitmap.setPixel(x - minX, y - minY, pixel)
                    }
                }
            }

            return resultBitmap
        }
        return null
    }

    private fun resetDrawing() {
        drawPath.reset()
    }

    private fun centerImage() {
        val drawable = drawable ?: return
        val imageWidth = drawable.intrinsicWidth.toFloat()
        val imageHeight = drawable.intrinsicHeight.toFloat()
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()

        val scale = min(viewWidth * 0.9f / imageWidth, viewHeight * 0.9f / imageHeight)
        val scaledWidth = imageWidth * scale
        val scaledHeight = imageHeight * scale
        val dx = (viewWidth - scaledWidth) / 2
        val dy = (viewHeight - scaledHeight) / 2

        matrix.reset()
        matrix.postScale(scale, scale)
        matrix.postTranslate(dx, dy)
        imageMatrix = matrix
    }

    private fun getAngleFromMatrix(): Float {
        val values = FloatArray(9)
        matrix.getValues(values)
        return Math.toDegrees(atan2(values[Matrix.MSKEW_Y].toDouble(), values[Matrix.MSCALE_Y].toDouble())).toFloat()
    }

    private fun applyAICutBitmap(maskBitmap: Bitmap) {
        workingBitmap?.recycle()
        workingBitmap = maskBitmap.copy(Bitmap.Config.ARGB_8888, true)
        workingCanvas = Canvas(workingBitmap!!)
        setImageBitmap(workingBitmap)
        resetDrawing()
        invalidate()
    }

    private fun saveAICutState() {
        if (currentVersion < versionStack.size - 1) {
            for (i in versionStack.size - 1 downTo currentVersion + 1) {
                versionStack.removeAt(i)
            }
        }
        val operation = EditOperation(
            type = CutType.AI_CUT,
            matrix = Matrix(matrix)
        )
        versionStack.add(operation)
        currentVersion = versionStack.size - 1
        if (versionStack.size > MAX_VERSIONS) {
            versionStack.removeAt(0)
            currentVersion--
        }
        callback?.onChangeVersion(currentVersion, versionStack.size - 1)
    }

    private fun saveCropState(rect: Rect) {
        if (currentVersion < versionStack.size - 1) {
            for (i in versionStack.size - 1 downTo currentVersion + 1) {
                versionStack.removeAt(i)
            }
        }
        val operation = EditOperation(
            type = CutType.CROP,
            cropRect = Rect(rect),
            matrix = Matrix(matrix)
        )
        versionStack.add(operation)
        currentVersion = versionStack.size - 1
        if (versionStack.size > MAX_VERSIONS) {
            versionStack.removeAt(0)
            currentVersion--
        }
        callback?.onChangeVersion(currentVersion, versionStack.size - 1)
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

    private fun getMatrixScale(): Float {
        val values = FloatArray(9)
        matrix.getValues(values)
        val scaleX = values[Matrix.MSCALE_X]
        val scaleY = values[Matrix.MSCALE_Y]
        return max(scaleX, scaleY)
    }

    private fun clipWorkingBitmap(operationMatrix: Matrix?) {
        workingBitmap?.let { bitmap ->
            val clippedBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(clippedBitmap)
            val mappedPath = Path()
            val invertedMatrix = Matrix()
            if (operationMatrix?.invert(invertedMatrix) == true) {
                drawPath.transform(invertedMatrix, mappedPath)
            } else {
                mappedPath.set(drawPath)
            }
            canvas.clipPath(mappedPath)
            canvas.drawBitmap(bitmap, 0f, 0f, null)

            workingBitmap?.recycle()
            workingBitmap = clippedBitmap
            workingCanvas = Canvas(workingBitmap!!)
            setImageBitmap(workingBitmap)
        }
    }

    private fun erase(path: Path, operationMatrix: Matrix?) {
        workingCanvas?.let { canvas ->
            val mappedPath = Path()
            val invertedMatrix = Matrix()
            if (operationMatrix?.invert(invertedMatrix) == true) {
                path.transform(invertedMatrix, mappedPath)
            } else {
                mappedPath.set(path)
            }
            canvas.drawPath(mappedPath, removePaint.apply {
                strokeWidth = paintSize
            })
            setImageBitmap(workingBitmap)
        }
    }

    private fun restore(path: Path, operationMatrix: Matrix?) {
        workingCanvas?.let { canvas ->
            originalBitmap?.let { srcBitmap ->
                val mappedPath = Path()
                val invertedMatrix = Matrix()
                if (operationMatrix?.invert(invertedMatrix) == true) {
                    path.transform(invertedMatrix, mappedPath)
                } else {
                    mappedPath.set(path)
                }
                val shader = BitmapShader(srcBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
                restorePaint.shader = shader
                canvas.drawPath(mappedPath, restorePaint.apply {
                    strokeWidth = paintSize
                })
                restorePaint.shader = null
                setImageBitmap(workingBitmap)
            }
        }
    }

    private fun saveState() {
        if (currentVersion < versionStack.size - 1) {
            for (i in versionStack.size - 1 downTo currentVersion + 1) {
                versionStack.removeAt(i)
            }
        }
        val operation = EditOperation(
            type = onHandType,
            path = Path(drawPath),
            paintSize = paintSize,
            matrix = Matrix(matrix)
        )
        versionStack.add(operation)
        currentVersion = versionStack.size - 1
        if (versionStack.size > MAX_VERSIONS) {
            versionStack.removeAt(0)
            currentVersion--
        }
        callback?.onChangeVersion(currentVersion, versionStack.size - 1)
    }

    private fun reapplyOperations() {
        workingBitmap?.recycle()
        originalBitmap?.let { srcBitmap ->
            workingBitmap = srcBitmap.copy(Bitmap.Config.ARGB_8888, true)
            workingCanvas = Canvas(workingBitmap!!)
            setImageBitmap(workingBitmap)
            for (i in 0..currentVersion) {
                val operation = versionStack[i]
                paintSize = operation.paintSize
                when (operation.type) {
                    CutType.INIT -> {}
                    CutType.REMOVE -> {
                        erase(operation.path, operation.matrix)
                    }
                    CutType.RESTORE -> {
                        restore(operation.path, operation.matrix)
                    }
                    CutType.AREA -> {
                        drawPath.set(operation.path)
                        clipWorkingBitmap(operation.matrix)
                    }
                    CutType.AI_CUT -> {
                        cachedAICutBitmap?.let { cachedAICutBitmap ->
                            workingBitmap?.recycle()
                            workingBitmap = cachedAICutBitmap.copy(Bitmap.Config.ARGB_8888, true)
                            workingCanvas = Canvas(workingBitmap!!)
                            setImageBitmap(workingBitmap)
                        }
                    }
                    CutType.CROP -> {
                        operation.cropRect?.let { rect ->
                            val boundedRect = Rect(
                                maxOf(0, rect.left),
                                maxOf(0, rect.top),
                                minOf(workingBitmap!!.width, rect.right),
                                minOf(workingBitmap!!.height, rect.bottom)
                            )
                            if (boundedRect.width() > 0 && boundedRect.height() > 0) {
                                val croppedBitmap = Bitmap.createBitmap(
                                    workingBitmap!!,
                                    boundedRect.left,
                                    boundedRect.top,
                                    boundedRect.width(),
                                    boundedRect.height()
                                )
                                val bm = Bitmap.createBitmap(srcBitmap.width, srcBitmap.height, Bitmap.Config.ARGB_8888)
                                val canvas = Canvas(bm)
                                canvas.drawBitmap(croppedBitmap, rect.left.toFloat(), rect.top.toFloat(), null)
                                workingBitmap?.recycle()
                                workingBitmap = bm
                                workingCanvas = Canvas(workingBitmap!!)
                                setImageBitmap(workingBitmap)
                            }
                        }
                        matrix.set(operation.matrix)
                        imageMatrix = matrix
                    }
                }
            }
        }
    }

    private fun createTouchAreaBitmap(touchX: Float, touchY: Float, circlePaint: Paint): Bitmap {
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
            paintSize / 2,
            circlePaint
        )
        return touchBitmap
    }

    private fun checkShowZoomView(canvas: Canvas) {
        if (!isDrawing || onHandType == CutType.AREA) return
        touchBitmap?.let {
            canvas.drawBitmap(it, 0f, 0f, null)
        }
    }

    override fun onDraw(canvas: Canvas) {
        touchBitmap?.recycle()
        touchBitmap = null
        background?.let { canvas.drawBitmap(it, 0f, 0f, null) }
        if (onHandType == CutType.RESTORE) {
            originalBitmap?.let { bitmap ->
                canvas.drawBitmap(bitmap, matrix, alphaPaint)
            }
        }
        super.onDraw(canvas)
        when (onHandType) {
            CutType.AREA, CutType.AI_CUT -> {
                if (isDrawing) {
                    canvas.drawPath(drawPath, areaPaint)
                }
            }
            CutType.REMOVE, CutType.RESTORE -> {
                if (isDrawing) {
                    canvas.drawCircle(lastTouchX, lastTouchY, (paintSize / 2f) * getMatrixScale(), circlePaint)
                    touchBitmap = createTouchAreaBitmap(lastTouchX, lastTouchY, circlePaint)
                }
            }
            else -> {}
        }
        checkShowZoomView(canvas)
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

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        originalBitmap?.recycle()
        originalBitmap = null
        workingBitmap?.recycle()
        workingBitmap = null
        background?.recycle()
        background = null
        touchBitmap?.recycle()
        touchBitmap = null
        cachedAICutBitmap?.recycle()
        cachedAICutBitmap = null
        versionStack.clear()
        workingCanvas = null
        currentVersion = -1
        callback = null
    }

    open class FreedomCutterCallbackImpl {
        open fun onChangeVersion(currentVersion: Int, totalVersion: Int) {}
        open fun onChangeType(type: CutType) {}
        open fun onAICutCompleted(success: Boolean) {}
        open fun onCenterModeChanged(mode: FreedomCutterAnimationMode) {}
        open fun onCropCompleted(success: Boolean) {}
    }
}