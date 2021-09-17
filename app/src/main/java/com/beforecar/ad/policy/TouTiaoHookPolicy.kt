package com.beforecar.ad.policy

import android.app.Application
import com.beforecar.ad.policy.base.AbsHookPolicy
import com.beforecar.ad.policy.base.getStackInfo
import com.beforecar.ad.utils.FileUtils
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.lang.reflect.Method

/**
 * @author: wangpan
 * @email: p.wang@aftership.com
 * @date: 2021/5/8
 */
class TouTiaoHookPolicy : AbsHookPolicy() {

    companion object {

        const val CallServerInterceptor = "com.bytedance.retrofit2.CallServerInterceptor"
        const val Response = "com.bytedance.retrofit2.client.Response"
        const val SsResponse = "com.bytedance.retrofit2.SsResponse"
        const val TypedByteArray = "com.bytedance.retrofit2.mime.TypedByteArray"

    }

    override val tag: String = "tag_toutiao"

    override fun getPackageName(): String {
        return "com.ss.android.article.news"
    }

    override fun onMainApplicationBeforeCreate(application: Application, classLoader: ClassLoader) {
        //hook CallServerInterceptor 拦截器
        hookCallServerInterceptor(classLoader)
        //闪屏页广告
        removeSplashAd(application)
    }

    /**
     * 移除启动页广告
     */
    private fun removeSplashAd(application: Application) {
        try {
            val path1 = application.getExternalFilesDir(null)?.absolutePath ?: ""
            val bool1 = FileUtils.deleteFilesInDir(File("$path1/splashCache/"))

            val path2 = application.filesDir.absolutePath
            val bool2 = FileUtils.deleteFilesInDir(File("$path2/splashCache/"))

            val path3 = application.externalCacheDir?.parent ?: ""
            val bool3 = FileUtils.deleteFilesInDir(File("$path3/splashCache/"))

            log("removeSplashAd success: $bool1, $bool2, $bool3")
        } catch (t: Throwable) {
            log("removeSplashAd fail: ${t.getStackInfo()}")
        }
    }

    private fun findParseResponseMethod(classLoader: ClassLoader): Method? {
        try {
            val interceptorCls = XposedHelpers.findClass(CallServerInterceptor, classLoader)
            val ssResponseCls = XposedHelpers.findClass(SsResponse, classLoader)
            val responseCls = XposedHelpers.findClass(Response, classLoader)
            for (method in interceptorCls.declaredMethods) {
                if (method.returnType != ssResponseCls) {
                    continue
                }
                val parameterTypes = method.parameterTypes
                if (parameterTypes.size == 2 && parameterTypes[0] == responseCls) {
                    log("findParseResponseMethod success: $method")
                    return method
                }
            }
        } catch (t: Throwable) {
            log("findParseResponseMethod fail: ${t.getStackInfo()}")
        }
        return null
    }

    private fun hookCallServerInterceptor(classLoader: ClassLoader) {
        val parseResponseMethod = findParseResponseMethod(classLoader)
        if (parseResponseMethod == null) {
            log("hookCallServerInterceptor cancel: parseResponseMethod is null")
            return
        }

        try {
            XposedBridge.hookMethod(parseResponseMethod, object : XC_MethodHook() {
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
                        //视频详情页播放完后的广告
                        url.contains("api/ad/post_patch/v1/") -> {
                            removeVideoAdItems(body)
                        }
                        //启动页广告
                        url.contains("api/ad/splash/news_article/v14/") -> {
                            removeSplashAdItems(body)
                        }
                        //小程序推荐
                        url.contains("tfe/route/micro_recommend/list/v1/") -> {
                            removeMicroRecommendItems(body)
                        }
                        //新闻详情页底部推荐广告
                        url.contains("article/slow_information/v27/") -> {
                            removeArticleSlowInformation(body)
                        }
                    }
                }
            })
        } catch (t: Throwable) {
            log("hookCallServerInterceptor fail: ${t.getStackInfo()}")
        }
    }

    private fun getBodyFromResponse(response: Any): Any? {
        try {
            val classLoader = response.javaClass.classLoader!!
            val bodyClass = XposedHelpers.findClass(TypedByteArray, classLoader)
            val body = XposedHelpers.callMethod(response, "getBody")
            if (body != null && bodyClass == body.javaClass) {
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
            val adTypes = listOf(48, 1860)
            for (index in 0 until data.length()) {
                val item = data.optJSONObject(index)
                val content = JSONObject(item.optString("content"))
                val cellType = content.optInt("cell_type")
                val label = content.optString("label")
                if (cellType in adTypes || label.contains("广告")) {
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

    /**
     * 视频详情页播放完后的广告
     */
    private fun removeVideoAdItems(body: Any) {
        try {
            log("removeVideoAdItems start")
            val string = getBodyString(body)
            val newString = removeVideoAdItemString(string)
            setBodyString(body, newString)
        } catch (t: Throwable) {
            log("removeVideoAdItems fail: ${t.getStackInfo()}")
        }
    }

    private fun removeVideoAdItemString(string: String): String {
        try {
            val result = JSONObject(string)
            val data = result.optJSONObject("data") ?: return string
            val adItems = data.optJSONArray("ad_item") ?: JSONArray()
            var adItemCount = 0
            if (adItems.length() > 0) {
                data.put("ad_item", "")
                adItemCount = adItems.length()
            }
            log("removeVideoAdItemString success: $adItemCount")
            return result.toString()
        } catch (t: Throwable) {
            log("removeFeedListAdString fail: ${t.getStackInfo()}")
        }
        return string
    }

    /**
     * 去除启动页广告
     */
    private fun removeSplashAdItems(body: Any) {
        try {
            log("removeSplashAdItems start")
            val string = getBodyString(body)
            val newString = removeSplashAdItemString(string)
            setBodyString(body, newString)
        } catch (t: Throwable) {
            log("removeSplashAdItems fail: ${t.getStackInfo()}")
        }
    }

    private fun removeSplashAdItemString(string: String): String {
        try {
            val result = JSONObject(string)
            val data = result.optJSONObject("data")
            if (data != null) {
                result.put("data", "")
                log("removeSplashAdItemString success")
            }
            return result.toString()
        } catch (t: Throwable) {
            log("removeSplashAdItemString fail: ${t.getStackInfo()}")
        }
        return string
    }

    /**
     * 新闻详情页底部推荐广告
     */
    private fun removeArticleSlowInformation(body: Any) {
        try {
            log("removeArticleSlowInformation start")
            val string = getBodyString(body)
            val newString = removeArticleSlowInformationString(string)
            setBodyString(body, newString)
        } catch (t: Throwable) {
            log("removeArticleSlowInformation fail: ${t.getStackInfo()}")
        }
    }

    private fun removeArticleSlowInformationString(string: String): String {
        try {
            val result = JSONObject(string)
            val data = result.optJSONObject("data")
            if (data != null) {
                result.put("data", "")
                log("removeArticleSlowInformationString success")
            }
            return result.toString()
        } catch (t: Throwable) {
            log("removeArticleSlowInformationString fail: ${t.getStackInfo()}")
        }
        return string
    }

    /**
     * 去除小程序推荐
     */
    private fun removeMicroRecommendItems(body: Any) {
        try {
            log("removeMicroRecommendItems start")
            val string = getBodyString(body)
            val newString = removeMicroRecommendItemString(string)
            setBodyString(body, newString)
        } catch (t: Throwable) {
            log("removeMicroRecommendItems fail: ${t.getStackInfo()}")
        }
    }

    private fun removeMicroRecommendItemString(string: String): String {
        try {
            val result = JSONObject(string)
            val dataItems = result.optJSONArray("data") ?: JSONArray()
            if (dataItems.length() > 0) {
                result.put("data", "")
                log("removeMicroRecommendItemString success")
            }
            return result.toString()
        } catch (t: Throwable) {
            log("removeMicroRecommendItemString fail: ${t.getStackInfo()}")
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