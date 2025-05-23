package com.ecomobile.photo_cutter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.Shader
import android.net.Uri
import android.provider.MediaStore
import com.ecomobile.photo_cutter.model.CutType
import com.ecomobile.photo_cutter.model.EditOperation

/**
 * Manages image editing operations such as erase, restore, clip, and crop.
 */
class ImageEditor(
    private val context: Context,
    private val onBitmapUpdated: (Bitmap?) -> Unit
) {

    private var originalBitmap: Bitmap? = null
    private var workingBitmap: Bitmap? = null
    private var workingCanvas: Canvas? = null
    private var imagePath: String? = null
    private var imageUri: Uri? = null
    private var cachedAICutBitmap: Bitmap? = null
    private var paintSize = 60f

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

    fun setPaintSize(size: Float) {
        paintSize = size
        removePaint.strokeWidth = size
        restorePaint.strokeWidth = size
    }

    fun getPaintSize(): Float = paintSize

    fun getOriginalBitmap(): Bitmap? = originalBitmap

    fun getImageUri(): Uri? = imageUri

    fun getImagePath(): String? = imagePath

    private fun mapPathWithMatrix(path: Path, matrix: Matrix?): Path {
        val mappedPath = Path()
        val invertedMatrix = Matrix()
        if (matrix?.invert(invertedMatrix) == true) {
            path.transform(invertedMatrix, mappedPath)
        } else {
            mappedPath.set(path)
        }
        return mappedPath
    }

    /**
     * Loads an image from a file path.
     */
    fun replaceImageFromStorage(imagePath: String) {
        this.imagePath = imagePath
        try {
            val bitmap = BitmapFactory.decodeFile(imagePath)
                ?: throw IllegalArgumentException("Cannot load image from path: $imagePath")
            safeRecycle(originalBitmap, workingBitmap, cachedAICutBitmap)
            originalBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            workingBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            workingCanvas = Canvas(workingBitmap!!)
            cachedAICutBitmap = null
            onBitmapUpdated(workingBitmap)
        } catch (e: Exception) {
            onBitmapUpdated(null)
            throw e
        }
    }

    /**
     * Loads an image from a URI.
     */
    fun replaceImageFromUri(uri: Uri) {
        this.imageUri = uri
        try {
            val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                ?: throw IllegalArgumentException("Cannot load image from URI: $uri")
            safeRecycle(originalBitmap, workingBitmap, cachedAICutBitmap)
            originalBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            workingBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            workingCanvas = Canvas(workingBitmap!!)
            cachedAICutBitmap = null
            onBitmapUpdated(workingBitmap)
        } catch (e: Exception) {
            onBitmapUpdated(null)
            throw e
        }
    }

    /**
     * Applies an AI-cut bitmap to the working bitmap.
     */
    fun applyAICutBitmap(bitmap: Bitmap) {
        safeRecycle(workingBitmap, cachedAICutBitmap)
        cachedAICutBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        workingBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        workingCanvas = Canvas(workingBitmap!!)
        onBitmapUpdated(workingBitmap)
    }

    /**
     * Crops the image using the provided rectangle.
     */
    fun cropImage(rect: Rect, onComplete: (Boolean, String?) -> Unit) {
        workingBitmap?.let { bitmap ->
            val boundedRect = Rect(
                maxOf(0, rect.left),
                maxOf(0, rect.top),
                minOf(bitmap.width, rect.right),
                minOf(bitmap.height, rect.bottom)
            )
            if (boundedRect.width() <= 0 || boundedRect.height() <= 0) {
                onComplete(false, "Invalid crop rectangle")
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
            safeRecycle(workingBitmap, croppedBitmap)
            workingBitmap = bm.copy(Bitmap.Config.ARGB_8888, true)
            workingCanvas = Canvas(workingBitmap!!)
            onBitmapUpdated(workingBitmap)
            onComplete(true, null)
        } ?: onComplete(false, "No working bitmap available")
    }

    /**
     * Returns a bitmap containing only non-transparent pixels.
     */
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

    /**
     * Resets the working bitmap to the original bitmap.
     */
    fun resetToOriginal() {
        safeRecycle(workingBitmap)
        originalBitmap?.let { srcBitmap ->
            workingBitmap = srcBitmap.copy(Bitmap.Config.ARGB_8888, true)
            workingCanvas = Canvas(workingBitmap!!)
            onBitmapUpdated(workingBitmap)
        }
    }

    /**
     * Reapplies all operations up to the specified version.
     */
    fun reapplyOperations(operations: List<EditOperation>, currentVersion: Int) {
        safeRecycle(workingBitmap)
        originalBitmap?.let { srcBitmap ->
            workingBitmap = srcBitmap.copy(Bitmap.Config.ARGB_8888, true)
            workingCanvas = Canvas(workingBitmap!!)
            for (i in 0..currentVersion) {
                val operation = operations[i]
                setPaintSize(operation.paintSize)
                when (operation.type) {
                    CutType.REMOVE -> erase(operation.path, operation.matrix)
                    CutType.RESTORE -> restore(operation.path, operation.matrix)
                    CutType.AREA -> clipWorkingBitmap(operation.path, operation.matrix)
                    CutType.AI_CUT -> {
                        cachedAICutBitmap?.let { cached ->
                            safeRecycle(workingBitmap)
                            workingBitmap = cached.copy(Bitmap.Config.ARGB_8888, true)
                            workingCanvas = Canvas(workingBitmap!!)
                        }
                    }
                    CutType.CROP -> {
                        operation.cropRect?.let { rect ->
                            cropImage(rect) { _, _ -> }
                        }
                    }
                    else -> {}
                }
            }
            onBitmapUpdated(workingBitmap)
        }
    }

    fun clipWorkingBitmap(path: Path, matrix: Matrix?) {
        workingBitmap?.let { bitmap ->
            val clippedBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(clippedBitmap)
            canvas.clipPath(mapPathWithMatrix(path, matrix))
            canvas.drawBitmap(bitmap, 0f, 0f, null)
            safeRecycle(workingBitmap)
            workingBitmap = clippedBitmap
            workingCanvas = Canvas(workingBitmap!!)
            onBitmapUpdated(workingBitmap)
        }
    }

    fun erase(path: Path, matrix: Matrix?) {
        workingCanvas?.let { canvas ->
            canvas.drawPath(mapPathWithMatrix(path, matrix), removePaint)
            onBitmapUpdated(workingBitmap)
        }
    }

    fun restore(path: Path, matrix: Matrix?) {
        workingCanvas?.let { canvas ->
            originalBitmap?.let { srcBitmap ->
                val mappedPath = mapPathWithMatrix(path, matrix)
                val shader = BitmapShader(srcBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
                restorePaint.shader = shader
                canvas.drawPath(mappedPath, restorePaint)
                restorePaint.shader = null
                onBitmapUpdated(workingBitmap)
            }
        }
    }

    private fun safeRecycle(vararg bitmaps: Bitmap?) {
        bitmaps.forEach { bitmap ->
            bitmap?.takeIf { !it.isRecycled }?.recycle()
        }
    }
}