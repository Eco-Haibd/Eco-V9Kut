package com.ecomobile.photo_cutter

import android.graphics.Matrix
import android.graphics.Path
import android.graphics.Rect
import com.ecomobile.photo_cutter.model.CutType
import com.ecomobile.photo_cutter.model.EditOperation

/**
 * Manages the history of editing operations for undo/redo functionality.
 */
class EditHistoryManager(private val onVersionChanged: (Int, Int) -> Unit) {

    private val versionStack = arrayListOf<EditOperation>()
    private var currentVersion = -1
    private val MAX_VERSIONS = 20

    /**
     * Saves an editing operation to the history.
     */
    fun saveOperation(type: CutType, path: Path = Path(), paintSize: Float = 60f, matrix: Matrix, cropRect: Rect? = null) {
        if (currentVersion < versionStack.size - 1) {
            versionStack.subList(currentVersion + 1, versionStack.size).clear()
        }
        val operation = EditOperation(type, Path(path), paintSize, Matrix(matrix), cropRect)
        versionStack.add(operation)
        currentVersion = versionStack.size - 1
        if (versionStack.size > MAX_VERSIONS) {
            versionStack.removeAt(0)
            currentVersion--
        }
        onVersionChanged(currentVersion, versionStack.size - 1)
    }

    /**
     * Clears the history.
     */
    fun clear() {
        versionStack.clear()
        currentVersion = -1
        onVersionChanged(currentVersion, versionStack.size - 1)
    }

    /**
     * Undoes the last operation.
     */
    fun undo() {
        if (currentVersion > 0) {
            currentVersion--
            onVersionChanged(currentVersion, versionStack.size - 1)
        }
    }

    /**
     * Redoes the last undone operation.
     */
    fun redo() {
        if (currentVersion < versionStack.size - 1) {
            currentVersion++
            onVersionChanged(currentVersion, versionStack.size - 1)
        }
    }

    fun getOperations(): List<EditOperation> = versionStack

    fun getCurrentVersion(): Int = currentVersion
}