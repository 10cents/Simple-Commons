package com.simplemobiletools.commons.activities

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import com.simplemobiletools.commons.R
import kotlinx.android.synthetic.main.activity_customization.*
import yuku.ambilwarna.AmbilWarnaDialog

class CustomizationActivity : BaseSimpleActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customization)

        setupColorsPickers()
        updateTextColors(customization_holder)
        updateActionbarColor()

        customization_text_color_holder.setOnClickListener { pickTextColor() }
        customization_background_color_holder.setOnClickListener { pickBackgroundColor() }
        customization_primary_color_holder.setOnClickListener { pickPrimaryColor() }
    }

    private fun setupColorsPickers() {
        customization_text_color.setBackgroundColor(baseConfig.textColor)
        customization_primary_color.setBackgroundColor(baseConfig.primaryColor)
        customView(customization_background_color, baseConfig.backgroundColor, getContrastColor(baseConfig.backgroundColor))
    }

    fun customView(view: View, backgroundColor: Int, borderColor: Int) {
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(backgroundColor)
            setStroke(2, borderColor)
            view.setBackgroundDrawable(this)
        }
    }

    fun getContrastColor(color: Int): Int {
        val y = (299 * Color.red(color) + 587 * Color.green(color) + 114 * Color.blue(color)) / 1000
        return if (y >= 128) Color.BLACK else Color.WHITE
    }

    private fun pickTextColor() {
        AmbilWarnaDialog(this, baseConfig.textColor, object : AmbilWarnaDialog.OnAmbilWarnaListener {
            override fun onCancel(dialog: AmbilWarnaDialog) {
            }

            override fun onOk(dialog: AmbilWarnaDialog, color: Int) {
                baseConfig.textColor = color
                setupColorsPickers()
                updateTextColors(customization_holder)
            }
        }).show()
    }

    private fun pickBackgroundColor() {
        AmbilWarnaDialog(this, baseConfig.backgroundColor, object : AmbilWarnaDialog.OnAmbilWarnaListener {
            override fun onCancel(dialog: AmbilWarnaDialog) {
            }

            override fun onOk(dialog: AmbilWarnaDialog, color: Int) {
                baseConfig.backgroundColor = color
                setupColorsPickers()
                updateBackgroundColor()
            }
        }).show()
    }

    private fun pickPrimaryColor() {
        AmbilWarnaDialog(this, baseConfig.primaryColor, object : AmbilWarnaDialog.OnAmbilWarnaListener {
            override fun onCancel(dialog: AmbilWarnaDialog) {
            }

            override fun onOk(dialog: AmbilWarnaDialog, color: Int) {
                baseConfig.primaryColor = color
                setupColorsPickers()
                updateActionbarColor()
            }
        }).show()
    }
}
