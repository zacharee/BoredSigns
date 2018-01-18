package com.zacharee1.boredsigns.views

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet

class NavBarDragItemView : ConstraintLayout {
    var key: String? = null

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attributeSet: AttributeSet?) : super(context, attributeSet)
}