package com.simplemobiletools.commons.activities

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.bluetooth.BluetoothClass.Service.AUDIO
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.util.Pair
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import com.simplemobiletools.commons.R
import com.simplemobiletools.commons.asynctasks.CopyMoveTask
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.*
import java.io.File
import java.util.*

open class BaseSimpleActivity : AppCompatActivity() {
    var copyMoveCallback: (() -> Unit)? = null
    var actionOnPermission: ((granted: Boolean) -> Unit)? = null

    companion object {
        var funAfterSAFPermission: (() -> Unit)? = null
    }

    override fun onResume() {
        super.onResume()
        updateBackgroundColor()
        updateActionbarColor()
    }

    override fun onStop() {
        super.onStop()
        actionOnPermission = null
    }

    override fun onDestroy() {
        super.onDestroy()
        funAfterSAFPermission = null
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        android.R.id.home -> {
            finish()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    fun updateBackgroundColor(color: Int = baseConfig.backgroundColor) {
        window.decorView.setBackgroundColor(color)
    }

    fun updateActionbarColor(color: Int = baseConfig.primaryColor) {
        supportActionBar?.setBackgroundDrawable(ColorDrawable(color))
        updateStatusbarColor(color)
    }

    fun updateStatusbarColor(color: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val hsv = FloatArray(3)
            Color.colorToHSV(color, hsv)
            hsv[2] *= 0.85f
            window.statusBarColor = Color.HSVToColor(hsv)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == OPEN_DOCUMENT_TREE && resultCode == Activity.RESULT_OK && resultData != null) {
            if (isProperFolder(resultData.data)) {
                saveTreeUri(resultData)
                funAfterSAFPermission?.invoke()
            } else {
                toast(R.string.wrong_root_selected)
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                startActivityForResult(intent, requestCode)
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private fun saveTreeUri(resultData: Intent) {
        val treeUri = resultData.data
        baseConfig.treeUri = treeUri.toString()

        val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        contentResolver.takePersistableUriPermission(treeUri, takeFlags)
    }

    private fun isProperFolder(uri: Uri) = isExternalStorageDocument(uri) && isRootUri(uri) && !isInternalStorage(uri)

    @SuppressLint("NewApi")
    private fun isRootUri(uri: Uri) = DocumentsContract.getTreeDocumentId(uri).endsWith(":")

    @SuppressLint("NewApi")
    private fun isInternalStorage(uri: Uri) = isExternalStorageDocument(uri) && DocumentsContract.getTreeDocumentId(uri).contains("primary")

    private fun isExternalStorageDocument(uri: Uri) = "com.android.externalstorage.documents" == uri.authority

    fun startAboutActivity(appNameId: Int, licenseMask: Int, versionName: String) {
        Intent(applicationContext, AboutActivity::class.java).apply {
            putExtra(APP_NAME, getString(appNameId))
            putExtra(APP_LICENSES, licenseMask)
            putExtra(APP_VERSION_NAME, versionName)
            startActivity(this)
        }
    }

    fun startCustomizationActivity() = startActivity(Intent(this, CustomizationActivity::class.java))

    fun handleSAFDialog(file: File, callback: () -> Unit): Boolean {
        return if (isShowingSAFDialog(file, baseConfig.treeUri, OPEN_DOCUMENT_TREE)) {
            funAfterSAFPermission = callback
            true
        } else {
            callback()
            false
        }
    }

    fun copyMoveFilesTo(files: ArrayList<File>, source: String, destination: String, isCopyOperation: Boolean, copyPhotoVideoOnly: Boolean, callback: () -> Unit) {
        if (source == destination) {
            toast(R.string.source_and_destination_same)
            return
        }

        val destinationFolder = File(destination)
        if (!destinationFolder.exists()) {
            toast(R.string.invalid_destination)
            return
        }

        if (files.size == 1) {
            if (File(destinationFolder.absolutePath, files[0].name).exists()) {
                toast(R.string.name_taken)
                return
            }
        }

        handleSAFDialog(destinationFolder) {
            copyMoveCallback = callback
            if (isCopyOperation) {
                toast(R.string.copying)
                startCopyMove(files, destinationFolder, isCopyOperation, copyPhotoVideoOnly)
            } else {
                if (isPathOnSD(source) || isPathOnSD(destinationFolder.absolutePath)) {
                    handleSAFDialog(File(source)) {
                        toast(R.string.moving)
                        startCopyMove(files, destinationFolder, false, copyPhotoVideoOnly)
                    }
                } else {
                    toast(R.string.moving)
                    val updatedFiles = ArrayList<File>(files.size * 2)
                    updatedFiles.addAll(files)
                    for (file in files) {
                        val newFile = File(destinationFolder, file.name)
                        if (!newFile.exists() && file.renameTo(newFile))
                            updatedFiles.add(newFile)
                    }

                    scanFiles(updatedFiles) {
                        runOnUiThread {
                            copyMoveListener.copySucceeded(false, files.size * 2 == updatedFiles.size)
                        }
                    }
                }
            }
        }
    }

    private fun startCopyMove(files: ArrayList<File>, destinationFolder: File, isCopyOperation: Boolean, copyPhotoVideoOnly: Boolean) {
        val pair = Pair<ArrayList<File>, File>(files, destinationFolder)
        CopyMoveTask(this, isCopyOperation, copyPhotoVideoOnly, copyMoveListener).execute(pair)
    }

    fun handlePermission(permissionId: Int, callback: (granted: Boolean) -> Unit) {
        actionOnPermission = null
        val permString = getPermissionString(permissionId)
        if (hasPermission(permString)) {
            callback(true)
        } else {
            actionOnPermission = callback
            ActivityCompat.requestPermissions(this, arrayOf(permString), 1)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty()) {
            actionOnPermission?.invoke(grantResults[0] == 0)
        }
    }

    private fun hasPermission(permString: String) = ContextCompat.checkSelfPermission(this, permString) == PackageManager.PERMISSION_GRANTED

    private fun getPermissionString(id: Int) = when (id) {
        PERMISSION_READ_STORAGE -> Manifest.permission.READ_EXTERNAL_STORAGE
        PERMISSION_WRITE_STORAGE -> Manifest.permission.WRITE_EXTERNAL_STORAGE
        PERMISSION_CAMERA -> Manifest.permission.CAMERA
        PERMISSION_RECORD_AUDIO -> Manifest.permission.RECORD_AUDIO
        PERMISSION_READ_CONTACTS -> Manifest.permission.READ_CONTACTS
        PERMISSION_WRITE_CALENDAR -> Manifest.permission.WRITE_CALENDAR
        else -> ""
    }

    private val copyMoveListener = object : CopyMoveTask.CopyMoveListener {
        override fun copySucceeded(copyOnly: Boolean, copiedAll: Boolean) {
            if (copyOnly) {
                toast(if (copiedAll) R.string.copying_success else R.string.copying_success_partial)
            } else {
                toast(if (copiedAll) R.string.moving_success else R.string.moving_success_partial)
            }
            copyMoveCallback?.invoke()
            copyMoveCallback = null
        }

        override fun copyFailed() {
            toast(R.string.copy_move_failed)
            copyMoveCallback = null
        }
    }
}
