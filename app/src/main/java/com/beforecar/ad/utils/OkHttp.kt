package com.beforecar.ad.utils

import com.beforecar.ad.policy.base.getStackInfo
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

/**
 * @author: wangpan
 * @email: p.wang@aftership.com
 * @date: 2021/5/11
 */
object OkHttp {

    /**
     * 从 chain 中获取请求的 url
     */
    fun getUrlFromChain(chain: Any): String {
        try {
            val request = XposedHelpers.callMethod(chain, "request") as Any
            val httpUrl = XposedHelpers.callMethod(request, "url") as Any
            return XposedHelpers.callMethod(httpUrl, "toString") as String
        } catch (t: Throwable) {
            XposedBridge.log("getUrlFromChain fail: ${t.getStackInfo()}")
        }
        return ""
    }

    /**
     * 获取 response 中的数据
     */
    @Throws(Throwable::class)
    fun getResponseString(response: Any): String {
        val body = XposedHelpers.callMethod(response, "body") ?: return ""
        return XposedHelpers.callMethod(body, "string") as? String ?: ""
    }

    /**
     * 基于给定的 response 和指定的字符串数据构建新的 response
     */
    @Throws(Throwable::class)
    fun createNewResponse(response: Any, string: String): Any? {
        val classLoader = response.javaClass.classLoader
        val responseCls = XposedHelpers.findClass("okhttp3.Response", classLoader)
        val builderCls = XposedHelpers.findClass("okhttp3.Response\$Builder", classLoader)
        val bodyCls = XposedHelpers.findClass("okhttp3.ResponseBody", classLoader)
        val body = XposedHelpers.callMethod(response, "body") ?: return null
        val mediaType = XposedHelpers.callMethod(body, "contentType")
        val newBuilder = builderCls.getDeclaredConstructor(responseCls).also {
            it.isAccessible = true
        }.newInstance(response)
        val newBody = XposedHelpers.callStaticMethod(bodyCls, "create", mediaType, string)
        XposedHelpers.callMethod(newBuilder, "body", newBody)
        return XposedHelpers.callMethod(newBuilder, "build")
    }

    /**
     * 基于给定的 response 构建一个空数据的 response
     */
    fun createEmptyResponse(response: Any): Any? {
        try {
            val classLoader = response.javaClass.classLoader
            val responseCls = XposedHelpers.findClass("okhttp3.Response", classLoader)
            val builderCls = XposedHelpers.findClass("okhttp3.Response\$Builder", classLoader)
            val bodyCls = XposedHelpers.findClass("okhttp3.ResponseBody", classLoader)
            val body = XposedHelpers.callMethod(response, "body") ?: return null
            val mediaType = XposedHelpers.callMethod(body, "contentType")
            val emptyBody = XposedHelpers.callStaticMethod(bodyCls, "create", mediaType, "")
            val newBuilder = builderCls.getDeclaredConstructor(responseCls).also {
                it.isAccessible = true
            }.newInstance(response)
            XposedHelpers.callMethod(newBuilder, "body", emptyBody)
            return XposedHelpers.callMethod(newBuilder, "build")
        } catch (t: Throwable) {
            XposedBridge.log("createEmptyResponse fail: ${t.getStackInfo()}")
        }
        return null
    }

    /**
     * 判断响应是否成功
     */
    fun isSuccessful(response: Any): Boolean {
        try {
            return XposedHelpers.callMethod(response, "isSuccessful") as Boolean
        } catch (t: Throwable) {
            XposedBridge.log("isSuccessful fail: ${t.getStackInfo()}")
        }
        return false
    }

}