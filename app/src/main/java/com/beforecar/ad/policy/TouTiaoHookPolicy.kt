package com.beforecar.ad.policy

import android.app.Application
import com.beforecar.ad.policy.base.AbsHookPolicy
import com.beforecar.ad.policy.base.getProcessName
import com.beforecar.ad.policy.base.getStackInfo
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import org.json.JSONObject

/**
 * @author: wangpan
 * @email: p.wang@aftership.com
 * @date: 2021/5/8
 */
class TouTiaoHookPolicy : AbsHookPolicy() {

    override val tag: String = "tag_toutiao"

    override fun getPackageName(): String {
        return "com.ss.android.article.news"
    }

    override fun getMainApplicationName(): String {
        return "com.ss.android.article.news.ArticleApplication"
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        super.handleLoadPackage(lpparam)
        if (lpparam.processName != lpparam.packageName) return
        try {
            XposedHelpers.findAndHookMethod(
                "com.ss.android.article.news.ArticleApplication", lpparam.classLoader,
                "onCreate", object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val application = param.thisObject as Application
                        val classLoader = application.classLoader!!
                        if (application.getProcessName() == getPackageName()) {
                            onAppCreated(application, classLoader)
                        }
                    }
                }
            )
        } catch (t: Throwable) {
            log("onAppCreated fail: ${t.getStackInfo()}")
        }
    }

    private fun onAppCreated(application: Application, classLoader: ClassLoader) {
        //hook CallServerInterceptor 拦截器
        hookCallServerInterceptor(classLoader)
        //闪屏页广告
        removeSplashAd(classLoader)
    }

    /**
     * 移除启动页广告
     */
    private fun removeSplashAd(classLoader: ClassLoader) {
        try {
            val activityCls = XposedHelpers.findClassIfExists(
                "com.ss.android.article.base.feature.main.ArticleMainActivity",
                classLoader
            ) ?: return
            log("removeSplashAd start")
            XposedHelpers.findAndHookMethod(activityCls, "tryShowSplashAdView", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    param.result = null
                    log("removeSplashAd success")
                }
            })
        } catch (t: Throwable) {
            log("removeSplashAd fail: ${t.getStackInfo()}")
        }
    }

    private fun hookCallServerInterceptor(classLoader: ClassLoader) {
        try {
            val interceptorCls = XposedHelpers.findClass("com.bytedance.retrofit2.CallServerInterceptor", classLoader)
            val responseCls = XposedHelpers.findClass("com.bytedance.retrofit2.client.Response", classLoader)
            val jCls = XposedHelpers.findClass("com.bytedance.retrofit2.j", classLoader)
            XposedHelpers.findAndHookMethod(
                interceptorCls, "parseResponse", responseCls, jCls, object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val response = param.args[0] ?: return
                        val body = getBodyFromResponse(response)
                        val url = getUrlFromResponse(response)
                        if (body == null || url.isEmpty()) {
                            return
                        }
                        when {
                            //推荐列表
                            url.contains("api/news/feed/v88/") -> {
                                removeFeedListAdItems(body)
                            }
                            //检查更新
                            url.contains("check_version/") -> {
                                disableUpgrade(body)
                            }
                            //PostInnerFeedActivity
                            url.contains("api/feed/thread_aggr/v1/") -> {
                                removePostInnerFeedAdItems(body)
                            }
                        }
                    }

                    private fun getBodyFromResponse(response: Any): Any? {
                        try {
                            val bodyCls = XposedHelpers.findClass(
                                "com.bytedance.retrofit2.mime.TypedByteArray",
                                response.javaClass.classLoader
                            )
                            val body = XposedHelpers.callMethod(response, "getBody")
                            if (body != null && bodyCls == body.javaClass) {
                                return body
                            }
                        } catch (t: Throwable) {
                            log("getBodyFromResponse fail: ${t.getStackInfo()}")
                        }
                        return null
                    }

                    private fun getUrlFromResponse(response: Any): String {
                        return try {
                            return XposedHelpers.callMethod(response, "getUrl") as? String ?: ""
                        } catch (t: Throwable) {
                            log("getUrlFromResponse fail: ${t.getStackInfo()}")
                            ""
                        }
                    }
                }
            )
        } catch (t: Throwable) {
            log("hookCallServerInterceptor fail: ${t.getStackInfo()}")
        }
    }

    /**
     * 去掉 PostInnerFeedActivity 广告
     */
    private fun removePostInnerFeedAdItems(body: Any) {
        try {
            log("removePostInnerFeedAdItems start")
            val string = getBodyString(body)
            val newString = removePostInnerFeedAdString(string)
            setBodyString(body, newString)
        } catch (t: Throwable) {
            log("removePostInnerFeedAdItems fail: ${t.getStackInfo()}")
        }
    }

    private fun removePostInnerFeedAdString(string: String): String {
        try {
            val result = JSONObject(string)
            val data = result.optJSONArray("data") ?: return string
            var adItemCount = 0
            for (index in 0 until data.length()) {
                val item = data.optJSONObject(index)
                val content = JSONObject(item.optString("content"))
                val label = content.optString("label")
                if (label.contains("广告")) {
                    item.put("content", "")
                    adItemCount++
                }
            }
            log("removePostInnerFeedAdString success: $adItemCount")
            return result.toString()
        } catch (t: Throwable) {
            log("removePostInnerFeedAdString fail: ${t.getStackInfo()}")
        }
        return string
    }

    /**
     * 禁用更新
     */
    private fun disableUpgrade(body: Any) {
        try {
            log("disableUpgrade start")
            val result = JSONObject()
            result.put("code", 0)
            result.put("data", "")
            result.put("message", "success")
            setBodyString(body, result.toString())
            log("disableUpgrade success")
        } catch (t: Throwable) {
            log("disableUpgrade fail: ${t.getStackInfo()}")
        }
    }

    /**
     * 移除推荐列表广告
     */
    private fun removeFeedListAdItems(body: Any) {
        try {
            log("removeFeedListAdItems start")
            val string = getBodyString(body)
            val newString = removeFeedListAdString(string)
            setBodyString(body, newString)
        } catch (t: Throwable) {
            log("removeFeedListAdItems fail: ${t.getStackInfo()}")
        }
    }

    private fun removeFeedListAdString(string: String): String {
        try {
            val result = JSONObject(string)
            val data = result.optJSONArray("data") ?: return string
            var adItemCount = 0
            for (index in 0 until data.length()) {
                val item = data.optJSONObject(index)
                val content = JSONObject(item.optString("content"))
                val cellType = content.optInt("cell_type")
                val label = content.optString("label")
                //cellType == 48: 小视频推荐item
                if (cellType == 48 || label.contains("广告")) {
                    item.put("content", "")
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

    private fun getBodyString(body: Any): String {
        return try {
            val byteArray = XposedHelpers.callMethod(body, "getBytes") as ByteArray
            return String(byteArray)
        } catch (t: Throwable) {
            log("getBodyString fail: ${t.getStackInfo()}")
            ""
        }
    }

    private fun setBodyString(body: Any, string: String) {
        try {
            XposedHelpers.setObjectField(body, "bytes", string.toByteArray())
        } catch (t: Throwable) {
            log("setBodyString fail: ${t.getStackInfo()}")
        }
    }
}