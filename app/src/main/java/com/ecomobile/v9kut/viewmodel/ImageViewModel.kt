package com.ecomobile.v9kut.viewmodel

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ecomobile.v9kut.model.FolderModel
import com.ecomobile.v9kut.model.ImageModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File

class ImageViewModel: ViewModel() {

    private var images: MutableList<ImageModel> = mutableListOf()
    var folders: MutableLiveData<MutableList<FolderModel>> = MutableLiveData()
    var currentFolder: MutableLiveData<FolderModel> = MutableLiveData()
    var folderImages: MutableLiveData<MutableList<ImageModel>> = MutableLiveData()
    var selectedImage: ImageModel? = null

    fun queryAllImages(context: Context) {
        runBlocking {
            CoroutineScope(Dispatchers.IO).launch {
                val imageList: MutableList<ImageModel> = withContext(Dispatchers.IO) {
                    val imageList = mutableListOf<ImageModel>()
                    val projection = arrayOf(
                        MediaStore.Images.Media._ID,
                        MediaStore.Images.Media.DISPLAY_NAME,
                        MediaStore.Images.Media.DATA,
                        MediaStore.MediaColumns.DATA,
                        MediaStore.MediaColumns.SIZE)

                    val selection: String? = null
                    val selectionArgs: Array<String>? = null
                    val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

                    val cursor = context.contentResolver.query(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        projection,
                        selection,
                        selectionArgs,
                        sortOrder
                    )

                    cursor?.use {
                        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                        val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                        val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
                        val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                        while (cursor.moveToNext()) {
                            val id = cursor.getLong(idColumn)
                            val name = cursor.getString(nameColumn)?:""
                            val path = cursor.getString(pathColumn)?:""
                            val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                            val imageModel = ImageModel(id, name, path, uri)
                            imageList.add(imageModel)
                        }
                    }
                    imageList
                }

                withContext(Dispatchers.Main) {
                    images = imageList
                    getAllFolder(context)
                }
            }
        }
    }

    private fun getAllFolder(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val parents = mutableListOf<File>()
            val map = mutableMapOf<String, ImageModel>()
            val mapCount = mutableMapOf<String, Int>()
            images.forEach {
                val file = File(it.path)
                val parentFile = file.parentFile
                if (parentFile != null) {
                    if (parents.contains(parentFile).not()) {
                        parents.add(parentFile)
                        map[parentFile.path] = it
                    }
                    if (mapCount.containsKey(parentFile.path)) {
                        mapCount[parentFile.path] = mapCount[parentFile.path]!! + 1
                    } else {
                        mapCount[parentFile.path] = 1
                    }
                }
            }
            parents.sortBy { it.name.trim() }
            val tempFolders = parents.map {
                FolderModel(it.name, it.path, mapCount[it.path]?:0, map[it.path]?.path ?: "")
            } as MutableList<FolderModel>
            if (tempFolders.isNotEmpty()) {
                tempFolders.add(0, FolderModel("All images", "", images.size, images[0].path))
            }
            withContext(Dispatchers.Main) {
                folders.value = tempFolders
            }
        }
    }

    fun getAllImageInCurrentFolder(position: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            val list = images.filter {
                if (folders.value?.get(position)?.path == "") {
                    true
                } else {
                    val parentFile = File(it.path).parentFile
                    if (parentFile == null) {
                        false
                    } else {
                        parentFile.path == folders.value?.get(position)?.path
                    }
                }
            } as MutableList
            withContext(Dispatchers.Main) {
                currentFolder.value = folders.value?.get(position)
                folderImages.value = list
            }
        }
    }
}