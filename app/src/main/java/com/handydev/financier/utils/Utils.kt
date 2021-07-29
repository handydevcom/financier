package com.handydev.financier.utils

import android.content.Context
import android.os.Build
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes

@Suppress("DEPRECATION")
@ColorInt
fun getColorHelper(context: Context, @ColorRes id: Int) =
    if (Build.VERSION.SDK_INT >= 23) context.getColor(id) else context.resources.getColor(id);