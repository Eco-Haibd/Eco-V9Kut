package com.ecomobile.v9kut.screens.photo_cutter

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.SeekBar
import androidx.lifecycle.lifecycleScope
import com.ecomobile.base.extension.click
import com.ecomobile.base.extension.gone
import com.ecomobile.base.extension.invisible
import com.ecomobile.base.extension.slideDown
import com.ecomobile.base.extension.slideUp
import com.ecomobile.base.extension.visible
import com.ecomobile.base.recyclerview.PercentWidthItemDecoration
import com.ecomobile.photo_cutter.FreedomCutterCallbackImpl
import com.ecomobile.photo_cutter.model.CutType
import com.ecomobile.photo_cutter.model.FreedomCutterAnimationMode
import com.ecomobile.v9kut.R
import com.ecomobile.v9kut.screens.editor.model.CropOption
import com.ecomobile.v9kut.screens.cutter_success.CutterSuccessActivity
import com.ecomobile.v9kut.screens.editor.adapter.CropOptionAdapter
import com.ecomobile.v9kut.screens.editor.view.ActionHeaderView
import com.ecomobile.v9kut.screens.photo_cutter.adapter.CutterFeatureAdapter
import com.ecomobile.v9kut.screens.photo_cutter.model.CutterType
import com.ecomobile.v9kut.singleton.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import kotlin.math.roundToInt

fun PhotoCutterActivity.setupView() {
    intent.getStringExtra(PhotoCutterActivity.IMAGE_PATH)?.let {
        binding.fcImage.replaceImageFromStorage(it)
        BitmapFactory.decodeFile(it)?.let {bm ->
            imageWidth = bm.width
            imageHeight = bm.height
            binding.cropView.setImageBitmap(bm.copy(Bitmap.Config.ARGB_8888, true))
            bm.recycle()
        }
        binding.cropActionHeader.setTitle(getString(R.string.crop))
    }
    setupRecyclerViewFeature()
}

fun PhotoCutterActivity.setupRecyclerViewFeature() {
    val items = listOf(
        CutterType.MANUAL_CUT,
        CutterType.CROP,
        CutterType.ROTATE,
        CutterType.ERASER,
        CutterType.REPAIR
    )
    binding.rcvFeature.apply {
        addItemDecoration(
            PercentWidthItemDecoration(
                context = this@setupRecyclerViewFeature,
                resources.getDimension(com.intuit.sdp.R.dimen._4sdp).roundToInt(),
                resources.getDimension(com.intuit.sdp.R.dimen._12sdp).roundToInt(),
                widthRatio = 0.28f
            )
        )
        adapter = CutterFeatureAdapter(this@setupRecyclerViewFeature, items).apply {
            onItemClick = { feature ->
                binding.apply {
                    when(feature) {
                        CutterType.MANUAL_CUT -> binding.fcImage.setType(CutType.AREA)
                        CutterType.CROP -> {
                            if (!fcImage.isCenteringFullImage()) {
                                fcImage.centerFullImage()
                            }
                        }
                        CutterType.ROTATE -> binding.fcImage.cutByAI()
                        CutterType.ERASER -> binding.fcImage.setType(CutType.REMOVE)
                        CutterType.REPAIR -> binding.fcImage.setType(CutType.RESTORE)
                    }
                }
            }
        }
    }
    binding.cropActionBottom.apply {
        val listOption = listOf(
            CropOption.Original,
            CropOption.FreeStyle,
            CropOption.RATIO_1_1,
            CropOption.RATIO_4_5,
            CropOption.RATIO_9_16,
            CropOption.RATIO_9_20,
            CropOption.RATIO_1_2,
            CropOption.RATIO_2_3,
            CropOption.RATIO_3_4,
            CropOption.RATIO_16_9,
            CropOption.RATIO_3_2,
            CropOption.RATIO_4_3,
            CropOption.RATIO_5_4
        )
        cropOptionAdapter = CropOptionAdapter(this@setupRecyclerViewFeature, listOption).apply {
            setAdapter(this)
        }
    }
}

fun PhotoCutterActivity.setupListener() {
    binding.apply {
        imgBack.click {
            finish()
        }
        forward.click {
            fcImage.next()
        }
        backward.click {
            fcImage.prev()
        }
        cardSave.click {
            lifecycleScope.launch(Dispatchers.IO) {
                saveCutterImage()?.let {
                    Intent(this@setupListener, CutterSuccessActivity::class.java).apply {
                        putExtra(CutterSuccessActivity.IMAGE_URI, it)
                        startActivity(this)
                    }
                }
            }
        }
        cropActionHeader.setCallback(object : ActionHeaderView.Callback {
            override fun onClose() {
                setShowCropView(false)
                fcImage.exitCenterFullImage()
            }

            override fun onDone() {
                cropImage()
            }
        })
        cropOptionAdapter?.onItemClick = { cropOption ->
            setCropOption(cropOption)
        }
        fcImage.setListener(object : FreedomCutterCallbackImpl {
            override fun onChangeType(type: CutType) {
                when(type) {
                    CutType.REMOVE -> {
                        frmSize.visible()
                        tvSize.text = getString(R.string.eraser_size)
                    }
                    CutType.RESTORE -> {
                        frmSize.visible()
                        tvSize.text = getString(R.string.brush_size)
                    }
                    else -> {
                        frmSize.gone()
                    }
                }
            }

            override fun onChangeVersion(currentVersion: Int, totalVersions: Int) {
                tvBackward.text = "$currentVersion"
                tvForward.text = "${totalVersions - currentVersion}"
                if (currentVersion == 0) {
                    tvBackward.invisible()
                    imgBackward.alpha = 0.1f
                } else {
                    tvBackward.visible()
                    imgBackward.alpha = 1f
                }
                if (currentVersion == totalVersions) {
                    tvForward.invisible()
                    imgForward.alpha = 0.1f
                } else {
                    tvForward.visible()
                    imgForward.alpha = 1f
                }
            }

            override fun onAICutCompleted(success: Boolean, error: String?) {
            }

            override fun onCropCompleted(success: Boolean, error: String?) {
            }

            override fun onCenterModeChanged(mode: FreedomCutterAnimationMode) {
                when (mode) {
                    FreedomCutterAnimationMode.ENTER -> setShowCropView(true)
                    else -> {}
                }
            }

        })
        sbSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                fcImage.setSize(progress.toFloat())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })
    }
}

fun PhotoCutterActivity.setShowCropView(isShow: Boolean) {
    binding.apply {
        if (isShow) {
            cropActionHeader.slideDown(true)
            cropActionBottom.slideUp(true)
            cropView.visible()
            headerBase.slideUp { headerBase.invisible() }
            fcImage.gone()
            rcvFeature.slideDown { rcvFeature.invisible() }
        } else {
            cropActionHeader.slideUp { cropActionHeader.invisible() }
            cropActionBottom.slideDown { cropActionBottom.invisible() }
            cropView.gone()
            headerBase.slideDown(true)
            fcImage.visible()
            rcvFeature.slideUp(true)
        }
    }
}

fun PhotoCutterActivity.setCropOption(cropOption: CropOption) {
    binding.cropView.apply {
        when (cropOption) {
            CropOption.Original -> setOriginRatio()
            CropOption.FreeStyle -> setFixedAspectRatio(false)
            CropOption.RATIO_1_1 -> setAspectRatio(1, 1)
            CropOption.RATIO_4_5 -> setAspectRatio(4, 5)
            CropOption.RATIO_9_16 -> setAspectRatio(9, 16)
            CropOption.RATIO_9_20 -> setAspectRatio(9, 20)
            CropOption.RATIO_1_2 -> setAspectRatio(1, 2)
            CropOption.RATIO_2_3 -> setAspectRatio(2, 3)
            CropOption.RATIO_3_4 -> setAspectRatio(3, 4)
            CropOption.RATIO_16_9 -> setAspectRatio(16, 9)
            CropOption.RATIO_3_2 -> setAspectRatio(3, 2)
            CropOption.RATIO_4_3 -> setAspectRatio(4, 3)
            CropOption.RATIO_5_4 -> setAspectRatio(5, 4)
        }
    }
}

fun PhotoCutterActivity.cropImage() {
    binding.fcImage.cropImage(binding.cropView.cropRect)
    setShowCropView(false)
    binding.fcImage.exitCenterFullImage()
}

fun PhotoCutterActivity.saveCutterImage(): Uri? {
    val bitmap = binding.fcImage.getNonTransparentBitmap() ?: return null
    var outputStream: OutputStream? = null
    val uri : Uri?

    try {
        val fileName = "cutter_${System.currentTimeMillis()}.png"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val relativePath = FileUtils.cutterPath().replace(
                Environment.getExternalStorageDirectory().absolutePath + "/",
                ""
            )
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, relativePath)
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }

            val resolver = contentResolver
            uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                outputStream = resolver.openOutputStream(uri)
                outputStream?.apply {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, this)
                    flush()
                }
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
        } else {
            val parent = File(FileUtils.cutterPath())
            if (!parent.exists()) parent.mkdirs()
            val outputFile = File(parent, fileName)
            outputStream = FileOutputStream(outputFile)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()

            uri = Uri.fromFile(outputFile)
            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.DATA, outputFile.absolutePath)
            })
        }
        return uri
    } catch (e: Exception) {
        Log.e("HAI", "Failed to save image", e)
        return null
    } finally {
        outputStream?.close()
        bitmap.recycle()
    }
}