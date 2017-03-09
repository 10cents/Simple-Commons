package com.simplemobiletools.commons.extensions

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteConstraintException
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.support.v4.content.ContextCompat
import android.support.v4.content.FileProvider
import android.support.v4.provider.DocumentFile
import android.text.TextUtils
import com.simplemobiletools.commons.R
import java.io.File
import java.util.*
import java.util.regex.Pattern

fun Context.hasReadStoragePermission() = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
fun Context.hasWriteStoragePermission() = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

fun Context.storeStoragePaths() {
    Thread({
        baseConfig.internalStoragePath = getInternalStoragePath()
        baseConfig.sdCardPath = getSDCardPath().trimEnd('/')
    }).start()
}

// http://stackoverflow.com/a/18871043/1967672
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
fun Context.getSDCardPath(): String {
    if (!isLollipopPlus()) {
        return ""
    }

    return getStorageDirectories().firstOrNull { it.trimEnd('/') != internalStoragePath } ?: ""
}

fun Context.hasExternalSDCard() = sdCardPath.isNotEmpty()

fun Context.getStorageDirectories(): Array<String> {
    val paths = HashSet<String>()
    val rawExternalStorage = System.getenv("EXTERNAL_STORAGE")
    val rawSecondaryStoragesStr = System.getenv("SECONDARY_STORAGE")
    val rawEmulatedStorageTarget = System.getenv("EMULATED_STORAGE_TARGET")
    if (TextUtils.isEmpty(rawEmulatedStorageTarget)) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getExternalFilesDirs(null).filterNotNull().map { it.absolutePath }
                    .mapTo(paths) { it.substring(0, it.indexOf("Android/data")) }
        } else {
            if (TextUtils.isEmpty(rawExternalStorage)) {
                paths.addAll(physicalPaths)
            } else {
                paths.add(rawExternalStorage)
            }
        }
    } else {
        val path = Environment.getExternalStorageDirectory().absolutePath
        val folders = Pattern.compile("/").split(path)
        val lastFolder = folders[folders.size - 1]
        var isDigit = false
        try {
            Integer.valueOf(lastFolder)
            isDigit = true
        } catch (ignored: NumberFormatException) {
        }

        val rawUserId = if (isDigit) lastFolder else ""
        if (TextUtils.isEmpty(rawUserId)) {
            paths.add(rawEmulatedStorageTarget)
        } else {
            paths.add(rawEmulatedStorageTarget + File.separator + rawUserId)
        }
    }

    if (!TextUtils.isEmpty(rawSecondaryStoragesStr)) {
        val rawSecondaryStorages = rawSecondaryStoragesStr.split(File.pathSeparator.toRegex()).dropLastWhile(String::isEmpty).toTypedArray()
        Collections.addAll(paths, *rawSecondaryStorages)
    }
    return paths.toTypedArray()
}

fun Context.getHumanReadablePath(path: String): String {
    return getString(when (path) {
        "/" -> R.string.root
        internalStoragePath -> R.string.internal
        else -> R.string.sd_card
    })
}

fun Context.humanizePath(path: String): String {
    val basePath = path.getBasePath(this)
    return if (basePath == "/")
        "${getHumanReadablePath(basePath)}$path"
    else
        path.replaceFirst(basePath, getHumanReadablePath(basePath))
}

fun Context.getInternalStoragePath() = Environment.getExternalStorageDirectory().toString().trimEnd('/')

@SuppressLint("NewApi")
fun Context.isPathOnSD(path: String) = sdCardPath.isNotEmpty() && path.startsWith(sdCardPath)

fun Context.isKitkatPlus() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT

fun Context.isLollipopPlus() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP

fun Context.isNougatPlus() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N

@SuppressLint("NewApi")
fun Context.needsStupidWritePermissions(path: String) = isPathOnSD(path) && isLollipopPlus()

@SuppressLint("NewApi")
fun Context.isAStorageRootFolder(path: String): Boolean {
    val trimmed = path.trimEnd('/')
    return trimmed.isEmpty() || trimmed == internalStoragePath || trimmed == sdCardPath
}

fun Context.getMyFileUri(file: File): Uri {
    return if (isNougatPlus())
        FileProvider.getUriForFile(this, "$packageName.provider", file)
    else
        Uri.fromFile(file)
}

@SuppressLint("NewApi")
fun Context.getFileDocument(path: String, treeUri: String): DocumentFile? {
    if (!isLollipopPlus())
        return null

    var relativePath = path.substring(sdCardPath.length)
    if (relativePath.startsWith(File.separator))
        relativePath = relativePath.substring(1)

    var document = DocumentFile.fromTreeUri(this, Uri.parse(treeUri))
    val parts = relativePath.split("/")
    for (part in parts) {
        val currDocument = document.findFile(part)
        if (currDocument != null)
            document = currDocument
    }
    return document
}

@SuppressLint("NewApi")
fun Context.tryFastDocumentDelete(file: File): Boolean {
    val document = getFastDocument(file)
    return if (document?.isFile == true) {
        document.delete()
    } else
        false
}

@SuppressLint("NewApi")
fun Context.getFastDocument(file: File): DocumentFile? {
    if (!isLollipopPlus())
        return null

    val relativePath = Uri.encode(file.absolutePath.substring(baseConfig.sdCardPath.length).trim('/'))
    val sdCardPathPart = baseConfig.sdCardPath.split("/").filter(String::isNotEmpty).last().trim('/')
    val fullUri = "${baseConfig.treeUri}/document/$sdCardPathPart%3A$relativePath"
    return DocumentFile.fromSingleUri(this, Uri.parse(fullUri))
}

fun Context.scanFile(file: File, action: () -> Unit) {
    scanFiles(arrayListOf(file), action)
}

fun Context.scanPath(path: String, action: () -> Unit) {
    scanPaths(arrayListOf(path), action)
}

fun Context.scanFiles(files: ArrayList<File>, action: () -> Unit) {
    val allPaths = ArrayList<String>()
    for (file in files) {
        allPaths.addAll(getPaths(file))
    }
    rescanPaths(allPaths, action)
}

fun Context.scanPaths(paths: ArrayList<String>, action: () -> Unit) {
    val allPaths = ArrayList<String>()
    for (path in paths) {
        allPaths.addAll(getPaths(File(path)))
    }
    rescanPaths(allPaths, action)
}

fun Context.rescanPaths(paths: ArrayList<String>, action: () -> Unit) {
    var cnt = paths.size
    MediaScannerConnection.scanFile(this, paths.toTypedArray(), null, { s, uri ->
        if (--cnt == 0)
            action.invoke()
    })
}

fun getPaths(file: File): ArrayList<String> {
    val paths = ArrayList<String>()
    if (file.isDirectory) {
        val files = file.listFiles() ?: return paths
        for (curFile in files) {
            paths.addAll(getPaths(curFile))
        }
    } else {
        paths.add(file.absolutePath)
    }
    return paths
}

fun Context.getFileUri(file: File): Uri {
    return if (file.isImageSlow()) {
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    } else if (file.isVideoSlow()) {
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    } else {
        MediaStore.Files.getContentUri("external")
    }
}

// these functions update the mediastore instantly, MediaScannerConnection.scanFile takes some time to really get applied
fun Context.deleteFromMediaStore(file: File): Boolean {
    val where = "${MediaStore.MediaColumns.DATA} = ?"
    val args = arrayOf(file.absolutePath)
    return contentResolver.delete(getFileUri(file), where, args) == 1
}

fun Context.updateInMediaStore(oldFile: File, newFile: File): Boolean {
    val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DATA, newFile.absolutePath)
        put(MediaStore.MediaColumns.DISPLAY_NAME, newFile.name)
        put(MediaStore.MediaColumns.TITLE, newFile.name)
    }
    val uri = getFileUri(oldFile)
    val where = "${MediaStore.MediaColumns.DATA} = ?"
    val args = arrayOf(oldFile.absolutePath)

    return try {
        contentResolver.update(uri, values, where, args) == 1
    } catch (e: SQLiteConstraintException) {
        false
    }
}

private val physicalPaths = arrayListOf(
        "/storage/sdcard0", "/storage/sdcard1", // Motorola Xoom
        "/storage/extsdcard", // Samsung SGS3
        "/storage/sdcard0/external_sdcard", // User request
        "/mnt/extsdcard", "/mnt/sdcard/external_sd", // Samsung galaxy family
        "/mnt/external_sd", "/mnt/media_rw/sdcard1", // 4.4.2 on CyanogenMod S3
        "/removable/microsd", // Asus transformer prime
        "/mnt/emmc", "/storage/external_SD", // LG
        "/storage/ext_sd", // HTC One Max
        "/storage/removable/sdcard1", // Sony Xperia Z1
        "/data/sdext", "/data/sdext2", "/data/sdext3", "/data/sdext4", "/sdcard1", // Sony Xperia Z
        "/sdcard2", // HTC One M8s
        "/storage/microsd" // ASUS ZenFone 2
)
