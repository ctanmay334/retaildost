package com.example.utils

import android.content.Context
import android.util.TypedValue

object ResponsiveUtils {
    fun dpToPx(context: Context, dp: Float): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.resources.displayMetrics).toInt()
    }

    fun spToPx(context: Context, sp: Float): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, context.resources.displayMetrics).toInt()
    }

    fun getScreenWidthDp(context: Context): Int {
        return (context.resources.displayMetrics.widthPixels / context.resources.displayMetrics.density).toInt()
    }

    fun isSmallScreen(context: Context) = getScreenWidthDp(context) < 360
    fun isLargeScreen(context: Context) = getScreenWidthDp(context) >= 480
    fun isTablet(context: Context) = getScreenWidthDp(context) >= 600
}
