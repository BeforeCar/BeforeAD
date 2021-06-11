package com.beforecar.ad.okhttp

import com.beforecar.ad.policy.base.getStackInfo
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import java.lang.reflect.Method

/**
 * @author: wangpan
 * @email: p.wang@aftership.com
 * @date: 2021/6/11
 */
class OkHttpHelper private constructor(
    private val realCall: String,
    private val getResponseWithInterceptorChain: String,
    private val getRequest: String
) {

    @Throws(Throwable::class)
    fun getResponseWithInterceptorChainMethod(classLoader: ClassLoader): Method {
        val realCallCls = XposedHelpers.findClass(realCall, classLoader)
        return XposedHelpers.findMethodExact(realCallCls, getResponseWithInterceptorChain, *emptyArray())
    }

    fun getUrl(realCall: Any): String {
        try {
            val request = XposedHelpers.callMethod(realCall, getRequest)
            return getUrlFromRequest(request)
        } catch (t: Throwable) {
            XposedBridge.log("getUrl fail: ${t.getStackInfo()}")
        }
        return ""
    }

    private fun getUrlFromRequest(request: Any): String {
        try {
            val string = request.toString()
            val startTag = "url="
            val endTag = ","
            val startIndex = string.indexOf(startTag)
            val endIndex = string.indexOf(endTag, startIndex = startIndex + startTag.length)
            if (startIndex != -1 && endIndex != -1) {
                return string.substring(startIndex + startTag.length, endIndex)
            }
        } catch (t: Throwable) {
            XposedBridge.log("getUrlFromRequest fail: ${t.getStackInfo()}")
        }
        return ""
    }

    companion object {

        fun create(
            realCall: String = "okhttp3.RealCall",
            getResponseWithInterceptorChain: String = "getResponseWithInterceptorChain",
            getRequest: String = "request"
        ): OkHttpHelper {
            return OkHttpHelper(
                realCall,
                getResponseWithInterceptorChain,
                getRequest
            )
        }

    }

}