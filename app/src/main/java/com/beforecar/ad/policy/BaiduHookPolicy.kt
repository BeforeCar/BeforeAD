package com.beforecar.ad.policy

import android.app.Application
import android.content.Context
import com.beforecar.ad.policy.base.IHookPolicy
import com.beforecar.ad.policy.base.getStackInfo
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
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
        //首页推荐列表
        removeFeedListAdItems(classLoader)
        //详情页评论列表
        removeDetailCommentListAdItems(classLoader)
        //视频详情页推荐广告
        removeVideoDetailRecommendAdItems(classLoader)
        //闪屏页广告
        removeSplashAd(classLoader)
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
                        //图文新闻详情页推荐广告
                        url.contains("newspage/api/getmobads") -> {
                            val emptyResponse = buildEmptyResponseBody(response)
                            if (emptyResponse != null) {
                                param.result = emptyResponse
                                log("hookBridgeInterceptor removeDetailRecommendAdItems success")
                            }
                        }
                        //视频新闻详情页广告
                        url.contains("cmd=207") -> {
                            val emptyResponse = buildEmptyResponseBody(response)
                            if (emptyResponse != null) {
                                param.result = emptyResponse
                                log("hookBridgeInterceptor removeVideoDetailAdItems success")
                            }
                        }
                        //splash ad
                        url.contains("action=update") -> {
                            val emptyResponse = buildEmptyResponseBody(response)
                            if (emptyResponse != null) {
                                param.result = emptyResponse
                                log("hookBridgeInterceptor removeSplashAd success")
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
                        log("hookBridgeInterceptor getUrlFromChain fail: ${t.getStackInfo()}")
                    }
                    return ""
                }

                private fun buildEmptyResponseBody(response: Any): Any? {
                    try {
                        val responseCls = XposedHelpers.findClassIfExists("okhttp3.Response", classLoader)
                        val builderCls = XposedHelpers.findClassIfExists("okhttp3.Response\$Builder", classLoader)
                        val mediaTypeCls = XposedHelpers.findClass("okhttp3.MediaType", classLoader)
                        val bodyCls = XposedHelpers.findClass("okhttp3.ResponseBody", classLoader)
                        val mediaType = XposedHelpers.callStaticMethod(mediaTypeCls, "parse", "application/json;")
                        val emptyBody = XposedHelpers.callStaticMethod(bodyCls, "create", mediaType, "")
                        val newBuilder = builderCls.getConstructor(responseCls).newInstance(response)
                        XposedHelpers.callMethod(newBuilder, "body", emptyBody)
                        return XposedHelpers.callMethod(newBuilder, "build")
                    } catch (t: Throwable) {
                        log("hookBridgeInterceptor buildEmptyResponseBody fail: ${t.getStackInfo()}")
                    }
                    return null
                }
            })
        } catch (t: Throwable) {
            log("hookBridgeInterceptor fail: ${t.getStackInfo()}")
        }
    }

    /**
     * 视频详情页推荐广告
     */
    private fun removeVideoDetailRecommendAdItems(classLoader: ClassLoader) {
        try {
            val fCls = XposedHelpers.findClassIfExists("com.baidu.searchbox.video.detail.k.f", classLoader)
            val dCls = XposedHelpers.findClassIfExists("com.baidu.searchbox.video.detail.core.b.d", classLoader)
            if (fCls == null || dCls == null) {
                log("removeVideoDetailRecommendAdItems cancel: $fCls, $dCls")
                return
            }
            log("removeVideoDetailRecommendAdItems start")
            XposedHelpers.findAndHookMethod(fCls, "c", String::class.java, dCls, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val dObj = param.args[1] as Any
                    doRemove(dObj)
                }

                private fun doRemove(dObj: Any) {
                    try {
                        val rUXJson = XposedHelpers.getObjectField(dObj, "rUX") as? JSONObject ?: return
                        val relateJson = rUXJson.optJSONObject("relate") ?: return
                        val dataJson = relateJson.optJSONObject("data") ?: return
                        val adBanner = dataJson.remove("adBanner")
                        log("removeVideoDetailRecommendAdItems doRemove adBanner success: ${adBanner != null}")
                        val listJsonArray = dataJson.optJSONArray("list")
                        if (listJsonArray != null) {
                            //需要移除的 item index
                            val removeIndexList = mutableListOf<Int>()
                            val size = listJsonArray.length()
                            for (index in 0 until size) {
                                val item = listJsonArray.getJSONObject(index)
                                if (isAdItem(item)) {
                                    removeIndexList.add(index)
                                }
                            }
                            for (index in removeIndexList) {
                                listJsonArray.remove(index)
                            }
                            log("removeVideoDetailRecommendAdItems doRemove relate list success: ${removeIndexList.size}")
                        }
                    } catch (t: Throwable) {
                        log("removeVideoDetailRecommendAdItems doRemove fail: ${t.getStackInfo()}")
                    }
                }

                private fun isAdItem(item: JSONObject): Boolean {
                    try {
                        return item.optInt("vType") == 2
                    } catch (t: Throwable) {
                        log("removeVideoDetailRecommendAdItems isAdItem fail: ${t.getStackInfo()}")
                    }
                    return false
                }
            })
        } catch (t: Throwable) {
            log("removeVideoDetailRecommendAdItems fail: ${t.getStackInfo()}")
        }
    }

    /**
     * 移除评论列表广告
     */
    private fun removeDetailCommentListAdItems(classLoader: ClassLoader) {
        try {
            XposedHelpers.findAndHookMethod(
                "com.baidu.searchbox.model.e", classLoader,
                "mo", String::class.java, String::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        param.result = doRemove(param.result)
                    }

                    private fun doRemove(result: Any?): Any? {
                        if (result == null) {
                            return null
                        }
                        try {
                            val list = XposedHelpers.getObjectField(result, "meX") as? ArrayList<*>
                            if (list.isNullOrEmpty()) {
                                return result
                            }
                            log("removeDetailCommentAdItems success: ${list.size}")
                            list.clear()
                        } catch (t: Throwable) {
                            log("removeDetailCommentAdItems doRemove fail: ${t.getStackInfo()}")
                        }
                        return result
                    }
                })
        } catch (t: Throwable) {
            log("removeDetailCommentAdItems fail: ${t.getStackInfo()}")
        }
    }

    /**
     * 移除推荐列表的广告
     */
    private fun removeFeedListAdItems(classLoader: ClassLoader) {
        try {
            log("removeFeedListAdItems start")
            XposedHelpers.findAndHookMethod(
                "com.baidu.searchbox.feed.q.e", classLoader,
                "p", String::class.java, String::class.java, Boolean::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        param.result = doRemove(param.result)
                    }

                    private fun doRemove(result: Any?): Any? {
                        if (result == null) {
                            return null
                        }
                        try {
                            val items = XposedHelpers.getObjectField(result, "gqt") as? ArrayList<*>
                            if (items.isNullOrEmpty()) {
                                return result
                            }
                            val iterator = items.iterator()
                            var adItemCount = 0
                            while (iterator.hasNext()) {
                                val item = iterator.next()
                                if (isAdItem(item)) {
                                    adItemCount++
                                    iterator.remove()
                                }
                            }
                            if (adItemCount > 0) {
                                log("removeFeedListAdItems removeAdItems success: $adItemCount")
                            }
                        } catch (t: Throwable) {
                            log("removeFeedListAdItems removeAdItems fail: ${t.getStackInfo()}")
                        }
                        return result
                    }

                    private fun isAdItem(item: Any?): Boolean {
                        if (item == null) return false
                        try {
                            val data = XposedHelpers.getObjectField(item, "gDm") ?: return false
                            val mode = XposedHelpers.getObjectField(data, "mMode") as String
                            //log("item: $item, isAdItem: ${mode == "ad"}")
                            return mode == "ad"
                        } catch (t: Throwable) {
                            log("removeFeedListAdItems isAdItem fail: ${t.getStackInfo()}")
                        }
                        return false
                    }
                }
            )
        } catch (t: Throwable) {
            log("removeFeedListAdItems fail: ${t.getStackInfo()}")
        }
    }
}