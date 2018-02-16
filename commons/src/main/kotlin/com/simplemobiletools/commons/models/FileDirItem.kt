package com.simplemobiletools.commons.models

import android.content.Context
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.*
import java.io.File

data class FileDirItem(val path: String, val name: String = "", var isDirectory: Boolean = false, var children: Int = 0, var size: Long = 0L) :
        Comparable<FileDirItem> {
    companion object {
        var sorting: Int = 0
    }

    override fun compareTo(other: FileDirItem): Int {
        return if (isDirectory && !other.isDirectory) {
            -1
        } else if (!isDirectory && other.isDirectory) {
            1
        } else {
            var result: Int
            when {
                sorting and SORT_BY_NAME != 0 -> result = name.toLowerCase().compareTo(other.name.toLowerCase())
                sorting and SORT_BY_SIZE != 0 -> result = when {
                    size == other.size -> 0
                    size > other.size -> 1
                    else -> -1
                }
                sorting and SORT_BY_DATE_MODIFIED != 0 -> {
                    val file = File(path)
                    val otherFile = File(other.path)
                    result = when {
                        file.lastModified() == otherFile.lastModified() -> 0
                        file.lastModified() > otherFile.lastModified() -> 1
                        else -> -1
                    }
                }
                else -> {
                    result = getExtension().toLowerCase().compareTo(other.getExtension().toLowerCase())
                }
            }

            if (sorting and SORT_DESCENDING != 0) {
                result *= -1
            }
            result
        }
    }

    fun getExtension() = if (isDirectory) name else path.substringAfterLast('.', "")

    fun getBubbleText() = when {
        sorting and SORT_BY_SIZE != 0 -> size.formatSize()
        sorting and SORT_BY_DATE_MODIFIED != 0 -> File(path).lastModified().formatDate()
        sorting and SORT_BY_EXTENSION != 0 -> getExtension().toLowerCase()
        else -> name
    }

    fun getProperSize(context: Context, countHidden: Boolean): Long {
        return if (context.isPathOnOTG(path)) {
            context.getDocumentFile(path)?.getItemSize(countHidden) ?: 0
        } else {
            File(path).getProperSize(countHidden)
        }
    }

    fun getParentPath() = path.getParentPath()
}
