package com.simplemobiletools.commons.activities

import android.annotation.TargetApi
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MenuItem
import com.simplemobiletools.commons.R
import com.simplemobiletools.commons.extensions.baseConfig
import com.simplemobiletools.commons.extensions.isShowingWritePermissions
import com.simplemobiletools.commons.extensions.sdCardPath
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.commons.helpers.APP_LICENSES
import com.simplemobiletools.commons.helpers.APP_NAME
import com.simplemobiletools.commons.helpers.APP_VERSION_NAME
import com.simplemobiletools.commons.helpers.OPEN_DOCUMENT_TREE
import java.io.File

open class BaseSimpleActivity : AppCompatActivity() {
    companion object {
        var funAfterPermission: (() -> Unit)? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        updateBackgroundColor()
        updateActionbarColor()
    }

    override fun onDestroy() {
        super.onDestroy()
        funAfterPermission = null
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
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
            val uriEnding = resultData.dataString.split("/").last()
            val sdCardEnding = sdCardPath.split("/").last()
            if (uriEnding.startsWith(sdCardEnding)) {
                saveTreeUri(resultData)
                funAfterPermission?.invoke()
            } else {
                toast(R.string.wrong_root_selected)
                funAfterPermission = null
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    fun saveTreeUri(resultData: Intent) {
        val treeUri = resultData.data
        baseConfig.treeUri = treeUri.toString()

        val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        contentResolver.takePersistableUriPermission(treeUri, takeFlags)
    }

    fun startAboutActivity(appNameId: Int, licenseMask: Int, versionName: String) {
        Intent(applicationContext, AboutActivity::class.java).apply {
            putExtra(APP_NAME, getString(appNameId))
            putExtra(APP_LICENSES, licenseMask)
            putExtra(APP_VERSION_NAME, versionName)
            startActivity(this)
        }
    }

    fun startCustomizationActivity() = startActivity(Intent(this, CustomizationActivity::class.java))

    fun launchViewIntent(id: Int) = launchViewIntent(resources.getString(id))

    fun launchViewIntent(url: String) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(browserIntent)
    }

    fun isShowingPermDialog(file: File, callback: () -> Unit): Boolean {
        funAfterPermission = callback
        return isShowingWritePermissions(file, baseConfig.treeUri, OPEN_DOCUMENT_TREE)
    }
}
