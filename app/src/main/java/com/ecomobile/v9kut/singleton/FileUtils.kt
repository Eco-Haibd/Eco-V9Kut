package com.ecomobile.v9kut.singleton

import android.os.Environment

object FileUtils {

    fun cutterPath(): String {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).path + "/V9Kut/CutterImage/"
    }
}