package com.simplemobiletools.commons.views

import android.content.Context
import android.graphics.PorterDuff
import android.util.AttributeSet
import android.widget.EditText

class MyEditText : EditText {

    constructor(context: Context) : super(context) {
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
    }

    fun setColor(textColor: Int, bottomLineColor: Int) {
        background.mutate().setColorFilter(bottomLineColor, PorterDuff.Mode.SRC_ATOP)

        // requires android:textCursorDrawable="@null" in xml to color the cursor too
        setTextColor(textColor)
    }
}
