package com.mohammadkk.myfilebrowser.ui

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import com.mohammadkk.myfilebrowser.R

class LabelView : AppCompatTextView {
    constructor(context: Context) : super(context) {
        setupView(context, null, 0)
    }
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        setupView(context, attrs, 0)
    }
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        setupView(context, attrs, defStyleAttr)
    }
    private fun setupView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        val a = context.obtainStyledAttributes(attrs, R.styleable.LabelView, defStyleAttr, 0)
        try {
            var fontAsset = a.getString(R.styleable.LabelView_fontAsset)
            if (fontAsset.isNullOrEmpty()) {
                fontAsset = "fonts/noto_sans_arabic_medium.ttf"
            }
            val font = Typeface.createFromAsset(context.assets, fontAsset)
            val boldStyle = a.getBoolean(R.styleable.LabelView_boldStyle, false)
            val typeface = Typeface.create(font, if (boldStyle) Typeface.BOLD else Typeface.NORMAL)
            setTypeface(typeface)
        } finally {
            a.recycle()
        }
    }
}