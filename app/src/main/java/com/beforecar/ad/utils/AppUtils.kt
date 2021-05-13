package com.beforecar.ad.utils

import android.content.Context
import android.util.TypedValue

/**
 * @author: wangpan
 * @email: p.wang@aftership.com
 * @date: 2021/5/6
 */
object AppUtils {

    fun dp2px(context: Context, dpValue: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpValue, context.resources.displayMetrics)
    }

}