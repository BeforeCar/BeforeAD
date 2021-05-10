package com.beforecar.ad.policy

import android.app.Application
import android.content.Context
import com.beforecar.ad.policy.base.IHookPolicy
import com.beforecar.ad.policy.base.getStackInfo
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import org.json.JSONArray
import org.json.JSONObject

/**
 * @author: wangpan
 * @email: p.wang@aftership.com
 * @date: 2021/5/6
 * 百度
 */
class BaiduHookPolicy : IHookPolicy() {

    override val TAG: String = "tag_baidu"

    override fun getPackageName(): String {
        return "com.baidu.searchbox"
    }

    override fun onMainApplicationCreate(application: Application, classLoader: ClassLoader) {
        //hook okhttp BridgeInterceptor
        hookBridgeInterceptor(classLoader)
        //闪屏页广告
        removeSplashAd(classLoader)
    }

    /**
     * hook okhttp BridgeInterceptor
     */
    private fun hookBridgeInterceptor(classLoader: ClassLoader) {
        try {
            log("hookBridgeInterceptor start")
            val interceptorCls = XposedHelpers.findClass("okhttp3.internal.http.BridgeInterceptor", classLoader)
            val chainCls = XposedHelpers.findClass("okhttp3.Interceptor\$Chain", classLoader)
            XposedHelpers.findAndHookMethod(interceptorCls, "intercept", chainCls, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val chain = param.args[0] as Any
                    val url = getUrlFromChain(chain)
                    val response = param.result ?: return
                    when {
                        //推荐列表
                        url.contains("cmd=100") -> {
                            log("removeFeedListAdItems api start")
                            val newResponse = removeFeedListAdItems(response)
                            if (newResponse != null) {
                                param.result = newResponse
                                log("removeFeedListAdItems api success")
                            }
                        }
                        //详情页评论列表广告
                        url.contains("cmd=308") -> {
                            log("removeDetailCommentListAdItems api start")
                            val emptyResponse = buildEmptyResponse(response)
                            if (emptyResponse != null) {
                                param.result = emptyResponse
                                log("removeDetailCommentListAdItems api success")
                            }
                        }
                        //图文新闻详情页推荐广告
                        url.contains("newspage/api/getmobads") -> {
                            log("removeDetailRecommendAdItems api start")
                            val emptyResponse = buildEmptyResponse(response)
                            if (emptyResponse != null) {
                                param.result = emptyResponse
                                log("removeDetailRecommendAdItems api success")
                            }
                        }
                        //视频新闻详情页广告
                        url.contains("cmd=207") -> {
                            log("removeVideoDetailAdItems api start")
                            val emptyResponse = buildEmptyResponse(response)
                            if (emptyResponse != null) {
                                param.result = emptyResponse
                                log("removeVideoDetailAdItems api success")
                            }
                        }
                        //视频新闻详情页推荐广告
                        url.contains("cmd=185") -> {
                            log("removeVideoDetailRecommendAdItems api start")
                            val newResponse = removeVideoDetailRecommendAdItems(response)
                            if (newResponse != null) {
                                param.result = newResponse
                                log("removeVideoDetailRecommendAdItems api success")
                            }
                        }
                        //splash ad
                        url.contains("action=update") -> {
                            log("removeSplashAd api start")
                            val emptyResponse = buildEmptyResponse(response)
                            if (emptyResponse != null) {
                                param.result = emptyResponse
                                log("removeSplashAd api success")
                            }
                        }
                        //检测更新
                        url.contains("cmd=301") -> {
                            log("removeAppUpgrade api start")
                            val emptyResponse = buildEmptyResponse(response)
                            if (emptyResponse != null) {
                                param.result = emptyResponse
                                log("removeAppUpgrade api success")
                            }
                        }
                    }
                }

                private fun getUrlFromChain(chain: Any): String {
                    try {
                        val request = XposedHelpers.callMethod(chain, "request") as Any
                        val httpUrl = XposedHelpers.callMethod(request, "url") as Any
                        return XposedHelpers.callMethod(httpUrl, "toString") as String
                    } catch (t: Throwable) {
                        log("getUrlFromChain fail: ${t.getStackInfo()}")
                    }
                    return ""
                }
            })
        } catch (t: Throwable) {
            log("hookBridgeInterceptor fail: ${t.getStackInfo()}")
        }
    }

    /**
     * 移除推荐列表的广告
     */
    private fun removeFeedListAdItems(response: Any): Any? {
        try {
            val string = getResponseString(response)
            val newString = removeFeedListAdString(string)
            return createNewResponse(response, newString)
        } catch (t: Throwable) {
            log("removeFeedListAdItems fail: ${t.getStackInfo()}")
        }
        return null
    }

    private fun removeFeedListAdString(string: String): String {
        try {
            val result = JSONObject(string)
            val data = result.optJSONObject("data") ?: return string
            val cmd = data.optJSONObject("100") ?: return string
            val itemList = cmd.optJSONObject("itemlist") ?: return string
            val items = itemList.optJSONArray("items") ?: return string
            var adItemCount = 0
            for (index in 0 until items.length()) {
                val item = items.getJSONObject(index)
                val itemData = item.optJSONObject("data") ?: continue
                val mode = itemData.optString("mode")
                if (mode == "ad") {
                    item.put("data", "")
                    adItemCount++
                }
            }
            log("removeFeedListAdString success: $adItemCount")
            return result.toString()
        } catch (t: Throwable) {
            log("removeFeedListAdString fail: ${t.getStackInfo()}")
        }
        return string
    }

    /**
     * 视频详情页推荐广告
     */
    private fun removeVideoDetailRecommendAdItems(response: Any): Any? {
        try {
            val string = getResponseString(response)
            val newString = removeVideoDetailRecommendAdString(string)
            return createNewResponse(response, newString)
        } catch (t: Throwable) {
            log("removeFeedListAdItems fail: ${t.getStackInfo()}")
        }
        return null
    }

    private fun removeVideoDetailRecommendAdString(string: String): String {
        try {
            val result = JSONObject(string)
            val data = result.optJSONObject("data") ?: return string
            val cmd = data.optJSONObject("185") ?: return string
            val adTpl = cmd.optJSONObject("adTpl")
            if (adTpl != null) {
                cmd.put("adTpl", "")
                log("removeVideoDetailRecommendAdString adTpl success")
            }
            val relate = cmd.optJSONObject("relate")
            if (relate != null) {
                val list = relate.optJSONArray("list") ?: JSONArray()
                var adItemCount = 0
                for (index in 0 until list.length()) {
                    val item = list.getJSONObject(index)
                    val vType = item.optInt("vType")
                    if (vType == 2) {
                        item.put("data", "")
                        adItemCount++
                    }
                }
                log("removeVideoDetailRecommendAdString relate success: $adItemCount")
            }
            return result.toString()
        } catch (t: Throwable) {
            log("removeVideoDetailRecommendAdString fail: ${t.getStackInfo()}")
        }
        return string
    }

    @Throws(Throwable::class)
    private fun getResponseString(response: Any): String {
        val body = XposedHelpers.callMethod(response, "body") ?: return ""
        return XposedHelpers.callMethod(body, "string") as? String ?: ""
    }

    @Throws(Throwable::class)
    private fun createNewResponse(response: Any, string: String): Any? {
        val classLoader = response.javaClass.classLoader
        val responseCls = XposedHelpers.findClass("okhttp3.Response", classLoader)
        val builderCls = XposedHelpers.findClass("okhttp3.Response\$Builder", classLoader)
        val bodyCls = XposedHelpers.findClass("okhttp3.ResponseBody", classLoader)
        val body = XposedHelpers.callMethod(response, "body") ?: return null
        val mediaType = XposedHelpers.callMethod(body, "contentType")
        val newBuilder = builderCls.getConstructor(responseCls).newInstance(response)
        val newBody = XposedHelpers.callStaticMethod(bodyCls, "create", mediaType, string)
        XposedHelpers.callMethod(newBuilder, "body", newBody)
        return XposedHelpers.callMethod(newBuilder, "build")
    }

    /**
     * 创建一个空的 response 基于给定的 response
     */
    private fun buildEmptyResponse(response: Any): Any? {
        try {
            val classLoader = response.javaClass.classLoader
            val responseCls = XposedHelpers.findClass("okhttp3.Response", classLoader)
            val builderCls = XposedHelpers.findClass("okhttp3.Response\$Builder", classLoader)
            val bodyCls = XposedHelpers.findClass("okhttp3.ResponseBody", classLoader)
            val body = XposedHelpers.callMethod(response, "body") ?: return null
            val mediaType = XposedHelpers.callMethod(body, "contentType")
            val emptyBody = XposedHelpers.callStaticMethod(bodyCls, "create", mediaType, "")
            val newBuilder = builderCls.getConstructor(responseCls).newInstance(response)
            XposedHelpers.callMethod(newBuilder, "body", emptyBody)
            return XposedHelpers.callMethod(newBuilder, "build")
        } catch (t: Throwable) {
            log("buildEmptyResponseBody fail: ${t.getStackInfo()}")
        }
        return null
    }

    /**
     * 热启动的闪屏页广告
     */
    private fun removeSplashAd(classLoader: ClassLoader) {
        try {
            val nCls = XposedHelpers.findClassIfExists("com.baidu.searchbox.introduction.n", classLoader)
            if (nCls == null) {
                log("removeSplashAd cancel")
                return
            }
            log("removeSplashAd start")
            XposedHelpers.findAndHookMethod(nCls, "na", Context::class.java, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    param.result = null
                    log("removeSplashAd success")
                }
            })
        } catch (t: Throwable) {
            log("removeSplashAd fail: ${t.getStackInfo()}")
        }
    }

}