package com.simplemobiletools.commons.extensions

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Looper
import android.os.TransactionTooLargeException
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.support.v4.provider.DocumentFile
import android.support.v7.app.AlertDialog
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.simplemobiletools.commons.R
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.dialogs.DonateDialog
import com.simplemobiletools.commons.dialogs.SecurityDialog
import com.simplemobiletools.commons.dialogs.WhatsNewDialog
import com.simplemobiletools.commons.dialogs.WritePermissionDialog
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.commons.models.FileDirItem
import com.simplemobiletools.commons.models.Release
import com.simplemobiletools.commons.models.SharedTheme
import com.simplemobiletools.commons.views.MyTextView
import kotlinx.android.synthetic.main.dialog_title.view.*
import java.io.*
import java.util.*

fun Activity.toast(id: Int, length: Int = Toast.LENGTH_SHORT) {
    if (isOnMainThread()) {
        showToast(this, id, length)
    } else {
        runOnUiThread {
            showToast(this, id, length)
        }
    }
}

fun Activity.toast(msg: String, length: Int = Toast.LENGTH_SHORT) {
    if (isOnMainThread()) {
        showToast(this, msg, length)
    } else {
        runOnUiThread {
            showToast(this, msg, length)
        }
    }
}

private fun showToast(activity: Activity, messageId: Int, length: Int) {
    if (!activity.isActivityDestroyed()) {
        Toast.makeText(activity.applicationContext, messageId, length).show()
    }
}

private fun showToast(activity: Activity, message: String, length: Int) {
    if (!activity.isActivityDestroyed()) {
        Toast.makeText(activity.applicationContext, message, length).show()
    }
}

fun Activity.showErrorToast(msg: String, length: Int = Toast.LENGTH_LONG) {
    toast(String.format(getString(R.string.an_error_occurred), msg), length)
}

fun Activity.showErrorToast(exception: Exception, length: Int = Toast.LENGTH_LONG) {
    showErrorToast(exception.toString(), length)
}

@SuppressLint("NewApi")
fun Activity.appLaunched() {
    baseConfig.internalStoragePath = getInternalStoragePath()
    updateSDCardPath()
    baseConfig.appRunCount++
    if (!isThankYouInstalled() && (baseConfig.appRunCount % 50 == 0)) {
        DonateDialog(this)
    }
}

@SuppressLint("InlinedApi")
fun Activity.isShowingSAFDialog(path: String, treeUri: String, requestCode: Int): Boolean {
    return if (needsStupidWritePermissions(path) && (treeUri.isEmpty() || !hasProperStoredTreeUri())) {
        runOnUiThread {
            WritePermissionDialog(this, false) {
                Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                    if (resolveActivity(packageManager) == null) {
                        type = "*/*"
                    }

                    if (resolveActivity(packageManager) != null) {
                        startActivityForResult(this, requestCode)
                    } else {
                        toast(R.string.unknown_error_occurred)
                    }
                }
            }
        }
        true
    } else {
        false
    }
}

fun Activity.launchViewIntent(id: Int) = launchViewIntent(resources.getString(id))

fun Activity.launchViewIntent(url: String) {
    Thread {
        Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            if (resolveActivity(packageManager) != null) {
                startActivity(this)
            } else {
                toast(R.string.no_app_found)
            }
        }
    }.start()
}

fun Activity.shareUri(uri: Uri, applicationId: String) {
    Thread {
        val newUri = ensurePublicUri(uri, applicationId)
        Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, newUri)
            type = getUriMimeType(uri, newUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (resolveActivity(packageManager) != null) {
                try {
                    startActivity(Intent.createChooser(this, getString(R.string.share_via)))
                } catch (e: TransactionTooLargeException) {
                    toast(R.string.maximum_share_reached)
                }
            } else {
                toast(R.string.no_app_found)
            }
        }
    }.start()
}

fun Activity.shareUris(uris: ArrayList<Uri>, applicationId: String) {
    Thread {
        if (uris.size == 1) {
            shareUri(uris.first(), applicationId)
        } else {
            val newUris = uris.map { ensurePublicUri(it, applicationId) } as ArrayList<Uri>
            var mimeType = newUris.getMimeType()
            if (mimeType.isEmpty() || mimeType == "*/*") {
                mimeType = uris.getMimeType()
            }
            Intent().apply {
                action = Intent.ACTION_SEND_MULTIPLE
                type = mimeType
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, newUris)

                if (resolveActivity(packageManager) != null) {
                    try {
                        startActivity(Intent.createChooser(this, getString(R.string.share_via)))
                    } catch (e: TransactionTooLargeException) {
                        toast(R.string.maximum_share_reached)
                    }
                } else {
                    toast(R.string.no_app_found)
                }
            }
        }
    }.start()
}

fun Activity.setAs(uri: Uri, applicationId: String) {
    Thread {
        val newUri = ensurePublicUri(uri, applicationId)
        Intent().apply {
            action = Intent.ACTION_ATTACH_DATA
            setDataAndType(newUri, getUriMimeType(uri, newUri))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val chooser = Intent.createChooser(this, getString(R.string.set_as))

            if (resolveActivity(packageManager) != null) {
                startActivityForResult(chooser, REQUEST_SET_AS)
            } else {
                toast(R.string.no_app_found)
            }
        }
    }.start()
}

fun Activity.openEditor(uri: Uri, applicationId: String) {
    Thread {
        val newUri = ensurePublicUri(uri, applicationId)
        Intent().apply {
            action = Intent.ACTION_EDIT
            setDataAndType(newUri, getUriMimeType(uri, newUri))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra(MediaStore.EXTRA_OUTPUT, uri)

            if (resolveActivity(packageManager) != null) {
                startActivityForResult(this, REQUEST_EDIT_IMAGE)
            } else {
                toast(R.string.no_app_found)
            }
        }
    }.start()
}

fun Activity.openFile(uri: Uri, forceChooser: Boolean, applicationId: String) {
    Thread {
        val newUri = try {
            ensurePublicUri(uri, applicationId)
        } catch (e: Exception) {
            showErrorToast(e)
            return@Thread
        }
        val mimeType = getUriMimeType(uri, newUri)
        Intent().apply {
            action = Intent.ACTION_VIEW
            setDataAndType(newUri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            if (applicationId == "com.simplemobiletools.gallery") {
                putExtra(IS_FROM_GALLERY, true)
            }

            putExtra(REAL_FILE_PATH, uri)

            if (resolveActivity(packageManager) != null) {
                val chooser = Intent.createChooser(this, getString(R.string.open_with))
                startActivity(if (forceChooser) chooser else this)
            } else {
                if (!tryGenericMimeType(this, mimeType, newUri)) {
                    toast(R.string.no_app_found)
                }
            }
        }
    }.start()
}

fun Activity.tryGenericMimeType(intent: Intent, mimeType: String, uri: Uri): Boolean {
    var genericMimeType = mimeType.getGenericMimeType()
    if (genericMimeType.isEmpty()) {
        genericMimeType = "*/*"
    }

    intent.setDataAndType(uri, genericMimeType)
    return if (intent.resolveActivity(packageManager) != null) {
        startActivity(intent)
        true
    } else {
        false
    }
}

fun BaseSimpleActivity.checkWhatsNew(releases: List<Release>, currVersion: Int) {
    if (baseConfig.lastVersion == 0) {
        baseConfig.lastVersion = currVersion
        return
    }

    val newReleases = arrayListOf<Release>()
    releases.filterTo(newReleases) { it.id > baseConfig.lastVersion }

    if (newReleases.isNotEmpty() && !baseConfig.avoidWhatsNew) {
        WhatsNewDialog(this, newReleases)
    }

    baseConfig.lastVersion = currVersion
}

fun BaseSimpleActivity.deleteFolders(folders: ArrayList<FileDirItem>, deleteMediaOnly: Boolean = true, callback: ((wasSuccess: Boolean) -> Unit)? = null) {
    if (Looper.myLooper() == Looper.getMainLooper()) {
        Thread {
            deleteFoldersBg(folders, deleteMediaOnly, callback)
        }.start()
    } else {
        deleteFoldersBg(folders, deleteMediaOnly, callback)
    }
}

fun BaseSimpleActivity.deleteFoldersBg(folders: ArrayList<FileDirItem>, deleteMediaOnly: Boolean = true, callback: ((wasSuccess: Boolean) -> Unit)? = null) {
    var wasSuccess = false
    var needPermissionForPath = ""
    for (folder in folders) {
        if (needsStupidWritePermissions(folder.path) && baseConfig.treeUri.isEmpty()) {
            needPermissionForPath = folder.path
            break
        }
    }

    handleSAFDialog(needPermissionForPath) {
        folders.forEachIndexed { index, folder ->
            deleteFolderBg(folder, deleteMediaOnly) {
                if (it)
                    wasSuccess = true

                if (index == folders.size - 1) {
                    callback?.invoke(wasSuccess)
                }
            }
        }
    }
}

fun BaseSimpleActivity.deleteFolder(folder: FileDirItem, deleteMediaOnly: Boolean = true, callback: ((wasSuccess: Boolean) -> Unit)? = null) {
    if (Looper.myLooper() == Looper.getMainLooper()) {
        Thread {
            deleteFolderBg(folder, deleteMediaOnly, callback)
        }.start()
    } else {
        deleteFolderBg(folder, deleteMediaOnly, callback)
    }
}

fun BaseSimpleActivity.deleteFolderBg(fileDirItem: FileDirItem, deleteMediaOnly: Boolean = true, callback: ((wasSuccess: Boolean) -> Unit)? = null) {
    val folder = File(fileDirItem.path)
    if (folder.exists()) {
        val filesArr = folder.listFiles()
        if (filesArr == null) {
            callback?.invoke(true)
            return
        }

        val filesList = (filesArr as Array).toList()
        val files = filesList.filter { !deleteMediaOnly || it.isImageVideoGif() }
        for (file in files) {
            deleteFileBg(file.toFileDirItem(applicationContext), false) { }
        }

        if (folder.listFiles()?.isEmpty() == true) {
            deleteFileBg(fileDirItem, true) { }
        }
    }
    callback?.invoke(true)
}

fun BaseSimpleActivity.deleteFiles(files: ArrayList<FileDirItem>, allowDeleteFolder: Boolean = false, callback: ((wasSuccess: Boolean) -> Unit)? = null) {
    if (Looper.myLooper() == Looper.getMainLooper()) {
        Thread {
            deleteFilesBg(files, allowDeleteFolder, callback)
        }.start()
    } else {
        deleteFilesBg(files, allowDeleteFolder, callback)
    }
}

fun BaseSimpleActivity.deleteFilesBg(files: ArrayList<FileDirItem>, allowDeleteFolder: Boolean = false, callback: ((wasSuccess: Boolean) -> Unit)? = null) {
    if (files.isEmpty()) {
        callback?.invoke(true)
        return
    }

    var wasSuccess = false
    handleSAFDialog(files[0].path) {
        files.forEachIndexed { index, file ->
            deleteFileBg(file, allowDeleteFolder) {
                if (it) {
                    wasSuccess = true
                }

                if (index == files.size - 1) {
                    callback?.invoke(wasSuccess)
                }
            }
        }
    }
}

fun BaseSimpleActivity.deleteFile(fileDirItem: FileDirItem, allowDeleteFolder: Boolean = false, callback: ((wasSuccess: Boolean) -> Unit)? = null) {
    if (Looper.myLooper() == Looper.getMainLooper()) {
        Thread {
            deleteFileBg(fileDirItem, allowDeleteFolder, callback)
        }.start()
    } else {
        deleteFileBg(fileDirItem, allowDeleteFolder, callback)
    }
}

@SuppressLint("NewApi")
fun BaseSimpleActivity.deleteFileBg(fileDirItem: FileDirItem, allowDeleteFolder: Boolean = false, callback: ((wasSuccess: Boolean) -> Unit)? = null) {
    val path = fileDirItem.path
    val file = File(path)
    var fileDeleted = !isPathOnOTG(path) && (!file.exists() || file.delete())
    if (fileDeleted) {
        rescanDeletedPath(fileDirItem.path) {
            callback?.invoke(true)
        }
    } else {
        if (file.isDirectory || allowDeleteFolder) {
            fileDeleted = deleteRecursively(file)
        }

        if (!fileDeleted) {
            if (isPathOnSD(path)) {
                handleSAFDialog(fileDirItem.path) {
                    trySAFFileDelete(fileDirItem, allowDeleteFolder, callback)
                }
            } else if (isPathOnOTG(fileDirItem.path)) {
                trySAFFileDelete(fileDirItem, allowDeleteFolder, callback)
            }
        }
    }
}

private fun deleteRecursively(file: File): Boolean {
    if (file.isDirectory) {
        val files = file.listFiles() ?: return file.delete()
        for (child in files) {
            deleteRecursively(child)
        }
    }

    return file.delete()
}

fun Activity.scanFile(file: File, callback: (() -> Unit)? = null) {
    applicationContext.scanFile(file, callback)
}

fun Activity.scanPath(path: String, callback: (() -> Unit)? = null) {
    applicationContext.scanPath(path, callback)
}

fun Activity.scanFiles(files: ArrayList<File>, callback: (() -> Unit)? = null) {
    applicationContext.scanFiles(files, callback)
}

fun Activity.scanPaths(paths: ArrayList<String>, callback: (() -> Unit)? = null) {
    applicationContext.scanPaths(paths, callback)
}

fun Activity.rescanPaths(paths: ArrayList<String>, callback: (() -> Unit)? = null) {
    applicationContext.rescanPaths(paths, callback)
}

@SuppressLint("NewApi")
fun BaseSimpleActivity.renameFile(oldPath: String, newPath: String, callback: ((success: Boolean) -> Unit)? = null) {
    if (needsStupidWritePermissions(newPath)) {
        handleSAFDialog(newPath) {
            val document = getDocumentFile(oldPath)
            if (document == null || (File(oldPath).isDirectory != document.isDirectory)) {
                callback?.invoke(false)
                return@handleSAFDialog
            }

            try {
                val uri = DocumentsContract.renameDocument(applicationContext.contentResolver, document.uri, newPath.getFilenameFromPath())
                if (document.uri != uri) {
                    updateInMediaStore(oldPath, newPath)
                    scanPaths(arrayListOf(oldPath, newPath)) {
                        if (!baseConfig.keepLastModified) {
                            updateLastModified(newPath, System.currentTimeMillis())
                        }
                        callback?.invoke(true)
                    }
                } else {
                    callback?.invoke(false)
                }
            } catch (e: SecurityException) {
                showErrorToast(e)
                callback?.invoke(false)
            }
        }
    } else if (File(oldPath).renameTo(File(newPath))) {
        if (File(newPath).isDirectory) {
            deleteFromMediaStore(oldPath)
            scanPath(newPath) {
                callback?.invoke(true)
            }
        } else {
            if (!baseConfig.keepLastModified) {
                File(newPath).setLastModified(System.currentTimeMillis())
            }
            updateInMediaStore(oldPath, newPath)
            scanPath(newPath) {
                callback?.invoke(true)
            }
        }
    } else {
        callback?.invoke(false)
    }
}

fun Activity.hideKeyboard() {
    val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow((currentFocus ?: View(this)).windowToken, 0)
    window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
    currentFocus?.clearFocus()
}

fun Activity.showKeyboard(et: EditText) {
    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.showSoftInput(et, InputMethodManager.SHOW_IMPLICIT)
}

fun Activity.hideKeyboard(view: View) {
    val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
}

fun BaseSimpleActivity.getFileOutputStream(fileDirItem: FileDirItem, callback: (outputStream: OutputStream?) -> Unit) {
    if (needsStupidWritePermissions(fileDirItem.path)) {
        handleSAFDialog(fileDirItem.path) {
            var document = getDocumentFile(fileDirItem.path)
            if (document == null) {
                val error = String.format(getString(R.string.could_not_create_file), fileDirItem.path)
                showErrorToast(error)
                callback(null)
                return@handleSAFDialog
            }

            if (!File(fileDirItem.path).exists()) {
                document = document.createFile("", fileDirItem.name)
            }

            if (document?.exists() == true) {
                try {
                    callback(applicationContext.contentResolver.openOutputStream(document.uri))
                } catch (e: FileNotFoundException) {
                    showErrorToast(e)
                    callback(null)
                }
            } else {
                val error = String.format(getString(R.string.could_not_create_file), fileDirItem.path)
                showErrorToast(error)
                callback(null)
            }
        }
    } else {
        callback(FileOutputStream(File(fileDirItem.path)))
    }
}

fun BaseSimpleActivity.getFileOutputStreamSync(path: String, mimeType: String, parentDocumentFile: DocumentFile? = null): OutputStream? {
    val targetFile = File(path)

    return if (needsStupidWritePermissions(path)) {
        val documentFile = parentDocumentFile ?: getDocumentFile(path.getParentPath())
        if (documentFile == null) {
            val error = String.format(getString(R.string.could_not_create_file), targetFile.parent)
            showErrorToast(error)
            return null
        }

        val newDocument = documentFile.createFile(mimeType, path.getFilenameFromPath())
        applicationContext.contentResolver.openOutputStream(newDocument!!.uri)
    } else {
        FileOutputStream(targetFile)
    }
}

fun BaseSimpleActivity.getFileInputStreamSync(path: String): InputStream? {
    return if (needsStupidWritePermissions(path)) {
        val fileDocument = getSomeDocumentFile(path)
        applicationContext.contentResolver.openInputStream(fileDocument?.uri)
    } else {
        FileInputStream(File(path))
    }
}

fun Activity.handleHiddenFolderPasswordProtection(callback: () -> Unit) {
    if (baseConfig.isPasswordProtectionOn) {
        SecurityDialog(this, baseConfig.passwordHash, baseConfig.protectionType) { hash, type, success ->
            if (success) {
                callback()
            }
        }
    } else {
        callback()
    }
}

fun Activity.handleAppPasswordProtection(callback: (success: Boolean) -> Unit) {
    if (baseConfig.appPasswordProtectionOn) {
        SecurityDialog(this, baseConfig.appPasswordHash, baseConfig.appProtectionType) { hash, type, success ->
            callback(success)
        }
    } else {
        callback(true)
    }
}

fun BaseSimpleActivity.createDirectorySync(directory: String): Boolean {
    if (doesFilePathExist(directory)) {
        return true
    }

    if (needsStupidWritePermissions(directory)) {
        val documentFile = getDocumentFile(directory.getParentPath()) ?: return false
        val newDir = documentFile.createDirectory(directory.getFilenameFromPath())
        return newDir != null
    }

    return File(directory).mkdirs()
}

fun BaseSimpleActivity.useEnglishToggled() {
    val conf = resources.configuration
    conf.locale = if (baseConfig.useEnglish) Locale.ENGLISH else Locale.getDefault()
    resources.updateConfiguration(conf, resources.displayMetrics)
    restartActivity()
}

fun BaseSimpleActivity.restartActivity() {
    finish()
    startActivity(intent)
}

fun Activity.isActivityDestroyed() = isJellyBean1Plus() && isDestroyed

fun Activity.updateSharedTheme(sharedTheme: SharedTheme): Int {
    try {
        val contentValues = MyContentProvider.fillThemeContentValues(sharedTheme)
        return applicationContext.contentResolver.update(MyContentProvider.CONTENT_URI, contentValues, null, null)
    } catch (e: Exception) {
        showErrorToast(e)
    }
    return 0
}

fun Activity.copyToClipboard(text: String) {
    val clip = ClipData.newPlainText(getString(R.string.simple_commons), text)
    (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClip = clip
    toast(R.string.value_copied_to_clipboard)
}

fun Activity.setupDialogStuff(view: View, dialog: AlertDialog, titleId: Int = 0, callback: (() -> Unit)? = null) {
    if (isActivityDestroyed()) {
        return
    }

    if (view is ViewGroup)
        updateTextColors(view)
    else if (view is MyTextView) {
        view.setColors(baseConfig.textColor, if (isBlackAndWhiteTheme()) Color.WHITE else baseConfig.primaryColor, baseConfig.backgroundColor)
    }

    var title: TextView? = null
    if (titleId != 0) {
        title = layoutInflater.inflate(R.layout.dialog_title, null) as TextView
        title.dialog_title_textview.apply {
            setText(titleId)
            setTextColor(baseConfig.textColor)
        }
    }

    dialog.apply {
        setView(view)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setCustomTitle(title)
        setCanceledOnTouchOutside(true)
        show()
        getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(baseConfig.textColor)
        getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(baseConfig.textColor)
        getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(baseConfig.textColor)
        window.setBackgroundDrawable(ColorDrawable(baseConfig.backgroundColor))
    }
    callback?.invoke()
}
