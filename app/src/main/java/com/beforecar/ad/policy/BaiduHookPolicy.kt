package com.beforecar.ad.policy

import android.app.Application
import com.beforecar.ad.policy.base.AbsHookPolicy
import com.beforecar.ad.policy.base.getStackInfo
import com.beforecar.ad.policy.base.getVersionName
import com.beforecar.ad.utils.FileUtils
import com.beforecar.ad.utils.OkHttp
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * @author: wangpan
 * @email: p.wang@aftership.com
 * @date: 2021/5/6
 * 百度
 */
class BaiduHookPolicy : AbsHookPolicy() {

    override val tag: String = "tag_baidu"

    override fun getPackageName(): String {
        return "com.baidu.searchbox"
    }

    private var versionName: String = ""

    override fun onMainApplicationAfterCreate(application: Application, classLoader: ClassLoader) {
        this.versionName = application.getVersionName()
        //hook okhttp BridgeInterceptor
        hookBridgeInterceptor(classLoader)
        //删除启动页广告文件
        removeSplashFiles(application)
    }

    /**
     * 删除闪屏页广告文件
     */
    private fun removeSplashFiles(application: Application) {
        try {
            FileUtils.delete(File(application.filesDir, "splash"))
            FileUtils.delete(File(application.filesDir, "splash_cache"))
            FileUtils.delete(File(application.filesDir, "splash_collection"))
            FileUtils.delete(File(application.filesDir, "splash_collection_new"))
            FileUtils.delete(File(application.filesDir, "splash_preview_new"))
            FileUtils.delete(File(application.filesDir, "splash_source_new"))
            log("removeSplashFiles success")
        } catch (t: Throwable) {
            log("removeSplashFiles fail: ${t.getStackInfo()}")
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
                    val url = OkHttp.getUrlFromChain(chain)
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
                        //推荐列表2
                        url.contains("cmd=311") -> {
                            log("removeFeedListAdItems2 api start")
                            val emptyResponse = OkHttp.createEmptyResponse(response)
                            if (emptyResponse != null) {
                                param.result = emptyResponse
                                log("removeFeedListAdItems2 api success")
                            }
                        }
                        //详情页评论列表广告
                        url.contains("cmd=308") -> {
                            log("removeDetailCommentListAdItems api start")
                            val emptyResponse = OkHttp.createEmptyResponse(response)
                            if (emptyResponse != null) {
                                param.result = emptyResponse
                                log("removeDetailCommentListAdItems api success")
                            }
                        }
                        //图文新闻详情页推荐广告
                        url.contains("newspage/api/getmobads") -> {
                            log("removeDetailRecommendAdItems api start")
                            val emptyResponse = OkHttp.createEmptyResponse(response)
                            if (emptyResponse != null) {
                                param.result = emptyResponse
                                log("removeDetailRecommendAdItems api success")
                            }
                        }
                        //视频新闻详情页广告
                        url.contains("cmd=207") -> {
                            log("removeVideoDetailAdItems api start")
                            val emptyResponse = OkHttp.createEmptyResponse(response)
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
                        //启动页广告
                        url.contains("action=update") -> {
                            log("removeSplashAd1 api start")
                            val newResponse = removeSplashAd1(response)
                            if (newResponse != null) {
                                param.result = newResponse
                                log("removeSplashAd1 api success")
                            }
                        }
                        //启动页广告
                        url.contains("ccs/v1/start/confsync") -> {
                            log("removeSplashAd2 api start")
                            val emptyResponse = OkHttp.createEmptyResponse(response)
                            if (emptyResponse != null) {
                                param.result = emptyResponse
                                log("removeSplashAd2 api success")
                            }
                        }
                        //检测更新
                        url.contains("cmd=301") -> {
                            log("removeAppUpgrade api start")
                            val emptyResponse = OkHttp.createEmptyResponse(response)
                            if (emptyResponse != null) {
                                param.result = emptyResponse
                                log("removeAppUpgrade api success")
                            }
                        }
                        //新闻详情页推荐广告
                        url.contains("cmd=104") -> {
                            log("removeDetailRelateAdItems api start")
                            val newResponse = removeDetailRelateAdItems(response)
                            if (newResponse != null) {
                                param.result = newResponse
                                log("removeDetailRelateAdItems api success")
                            }
                        }
                    }
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
            val string = OkHttp.getResponseString(response)
            val newString = removeFeedListAdString(string)
            return OkHttp.createNewResponse(response, newString)
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
                val resourceType = itemData.optString("resource_type")
                /**
                 * mode == "ad" 广告
                 * mode == "smartapp" 智能小程序
                 * resourceType == "answer" 百度问答
                 */
                if (mode == "ad" || mode == "smartapp" || resourceType == "answer") {
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
            val string = OkHttp.getResponseString(response)
            val newString = removeVideoDetailRecommendAdString(string)
            return OkHttp.createNewResponse(response, newString)
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

    /**
     * 新闻详情页推荐广告
     */
    private fun removeDetailRelateAdItems(response: Any): Any? {
        try {
            val string = OkHttp.getResponseString(response)
            val newString = removeDetailRelateAdString(string)
            return OkHttp.createNewResponse(response, newString)
        } catch (t: Throwable) {
            log("removeDetailRelateAdItems fail: ${t.getStackInfo()}")
        }
        return null
    }

    private fun removeDetailRelateAdString(string: String): String {
        try {
            val result = JSONObject(string)
            val data = result.optJSONObject("data") ?: return string
            val pageInfo = data.optJSONObject("pageInfo") ?: return string
            val smallAppNew = pageInfo.optJSONObject("smallAppNew")
            if (smallAppNew != null) {
                pageInfo.put("smallAppNew", "")
                log("removeDetailRelateAdString success")
            }
            return result.toString()
        } catch (t: Throwable) {
            log("removeDetailRelateAdString fail: ${t.getStackInfo()}")
        }
        return string
    }

    /**
     * 移除启动页广告
     */
    private fun removeSplashAd1(response: Any): Any? {
        try {
            val string = OkHttp.getResponseString(response)
            val newString = removeSplashAd1String(string)
            return OkHttp.createNewResponse(response, newString)
        } catch (t: Throwable) {
            log("removeSplashAd1 fail: ${t.getStackInfo()}")
        }
        return null
    }

    private fun removeSplashAd1String(string: String): String {
        try {
            val result = JSONObject(string)
            val data = result.optJSONObject("data") ?: return string
            val splash = data.optJSONObject("splash")
            if (splash != null) {
                data.put("splash", "")
                log("removeSplashAd1String success")
            }
            return result.toString()
        } catch (t: Throwable) {
            log("removeSplashAd1String fail: ${t.getStackInfo()}")
        }
        return string
    }

}