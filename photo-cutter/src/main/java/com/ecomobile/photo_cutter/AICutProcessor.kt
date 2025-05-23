package com.ecomobile.photo_cutter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.min

/**
 * Processes AI-based image cutting using ML Kit Subject Segmentation.
 */
class AICutProcessor(
    private val context: Context,
    private val onAICutCompleted: (Boolean, Bitmap?, String?) -> Unit
) {

    /**
     * Resizes bitmap to a maximum dimension while maintaining aspect ratio.
     */
    private fun resizeBitmap(bitmap: Bitmap, maxDimension: Int = 1024): Bitmap {
        val ratio = min(maxDimension.toFloat() / bitmap.width, maxDimension.toFloat() / bitmap.height)
        if (ratio >= 1) return bitmap
        val newWidth = (bitmap.width * ratio).toInt()
        val newHeight = (bitmap.height * ratio).toInt()
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * Performs AI-based cutting on the provided image.
     */
    fun cutByAI(originalBitmap: Bitmap?, imageUri: Uri?, imagePath: String?) {
        val bitmap = when {
            imageUri != null -> {
                try {
                    MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
                } catch (e: Exception) {
                    onAICutCompleted(false, null, "Failed to load bitmap from URI: ${e.message}")
                    return
                }
            }
            imagePath != null -> {
                try {
                    BitmapFactory.decodeFile(imagePath)
                } catch (e: Exception) {
                    onAICutCompleted(false, null, "Failed to load bitmap from path: ${e.message}")
                    return
                }
            }
            else -> originalBitmap ?: run {
                onAICutCompleted(false, null, "No valid bitmap provided")
                return
            }
        }

//        val resizedBitmap = resizeBitmap(bitmap)
        val inputImage = InputImage.fromBitmap(bitmap, 0)
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
                        onAICutCompleted(false, null, "No subjects found")
                        return@addOnSuccessListener
                    }

                    val subject = subjects[0]
                    val mask = subject.confidenceMask ?: run {
                        onAICutCompleted(false, null, "No confidence mask available")
                        return@addOnSuccessListener
                    }

                    val pixels = IntArray(bitmap.width * bitmap.height)
                    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
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
                        onAICutCompleted(true, maskBitmap, null)
                    }
                }
                .addOnFailureListener { e ->
                    onAICutCompleted(false, null, "AI cut failed: ${e.message}")
                }
        }
    }
}