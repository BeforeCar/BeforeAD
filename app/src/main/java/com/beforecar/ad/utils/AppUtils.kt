package com.beforecar.ad.utils

import android.content.Context
import android.graphics.Point
import android.util.TypedValue
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebView

/**
 * @author: wangpan
 * @email: p.wang@aftership.com
 * @date: 2021/5/6
 */
object AppUtils {

    fun dp2px(context: Context, dpValue: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpValue, context.resources.displayMetrics)
    }

    @Suppress("DEPRECATION")
    fun getScreenWidth(context: Context): Int {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val point = Point()
        wm.defaultDisplay.getRealSize(point)
        return point.x
    }

    @Suppress("DEPRECATION")
    fun getScreenHeight(context: Context): Int {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val point = Point()
        wm.defaultDisplay.getRealSize(point)
        return point.y
    }

    /**
     * 公用销毁 WebView 的方法
     */
    fun destroyWebView(webView: WebView) {
        webView.loadDataWithBaseURL(null, "", "text/html", "utf-8", null)
        webView.clearHistory()
        val parent = webView.parent
        if (parent is ViewGroup) {
            parent.removeView(webView)
        }
        webView.destroy()
    }

    fun Class<*>.isAssignableFromKt(childClazz: Class<*>?): Boolean {
        if (childClazz == null) {
            return false
        }
        return this.isAssignableFrom(childClazz)
    }

}