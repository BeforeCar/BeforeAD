package com.beforecar.ad.policy

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Intent
import com.beforecar.ad.policy.base.AbsHookPolicy
import com.beforecar.ad.policy.base.getStackInfo
import com.beforecar.ad.utils.JsonUtils
import com.beforecar.ad.utils.OkHttp
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import org.json.JSONArray
import org.json.JSONObject

/**
 * @author: wangpan
 * @email: p.wang@aftership.com
 * @date: 2021/5/6
 * 微博
 */
object WeiBoHookPolicy : AbsHookPolicy() {

    override val tag: String = "tag_weibo"

    override fun getPackageName(): String {
        return "com.sina.weibo"
    }

    override fun onMainApplicationAfterCreate(application: Application, classLoader: ClassLoader) {
        //hook okhttp BridgeInterceptor
        hookBridgeInterceptor(classLoader)
        //移除开屏广告
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
                    val url = OkHttp.getUrlFromChain(chain)
                    val response = param.result ?: return
                    when {
                        //关注列表, 推荐列表, 搜索结果列表
                        url.contains("statuses/unread_friends_timeline")
                                or url.contains("statuses/unread_hot_timeline") -> {
                            log("removeListAdItems api start")
                            val newResponse = removeListAdItems(response)
                            if (newResponse != null) {
                                param.result = newResponse
                                log("removeListAdItems api success")
                            }
                        }
                        //热点
                        url.contains("/cardlist?") -> {
                            log("removeCardListAdItems api start")
                            val newResponse = removeCardListAdItems(response)
                            if (newResponse != null) {
                                param.result = newResponse
                                log("removeCardListAdItems api success")
                            }
                        }
                        //视频详情页
                        url.contains("statuses/video_mixtimeline") -> {
                            log("removeVideoDetailAds api start")
                            val newResponse = removeVideoDetailAds(response)
                            if (newResponse != null) {
                                param.result = newResponse
                                log("removeVideoDetailAds api success")
                            }
                        }
                        //微博详情页
                        url.contains("statuses/extend") -> {
                            log("removeDetailWeiboAds api start")
                            val newResponse = removeDetailWeiboAds(response)
                            if (newResponse != null) {
                                param.result = newResponse
                                log("removeDetailWeiboAds api success")
                            }
                        }
                        //视频列表页
                        url.contains("video/tiny_stream_video_list") -> {
                            log("removeVideoListAdItems api start")
                            val newResponse = removeVideoListAdItems(response)
                            if (newResponse != null) {
                                param.result = newResponse
                                log("removeVideoListAdItems api success")
                            }
                        }
                        //评论列表
                        url.contains("/comments/build_comments") -> {
                            log("removeCommentListAdItems api start")
                            val newResponse = removeCommentListAdItems(response)
                            if (newResponse != null) {
                                param.result = newResponse
                                log("removeCommentListAdItems api success")
                            }
                        }
                        //检测更新
                        url.contains("client/version") -> {
                            log("checkUpgrade api start")
                            val newResponse = disableCheckUpgrade(response)
                            if (newResponse != null) {
                                param.result = newResponse
                                log("checkUpgrade api success")
                            }
                        }
                        //我 tab 页
                        url.contains("/page?") -> {
                            log("removeMinePageAds api start")
                            val newResponse = removeMinePageAds(response)
                            if (newResponse != null) {
                                param.result = newResponse
                                log("removeMinePageAds api success")
                            }
                        }
                        //我 tab 页2
                        url.contains("profile/me") -> {
                            log("removeMinePageAds2 api start")
                            val newResponse = removeMinePageAds2(response)
                            if (newResponse != null) {
                                param.result = newResponse
                                log("removeMinePageAds2 api success")
                            }
                        }
                        //搜索结果列表页
                        url.contains("/2/searchall") -> {
                            log("removeSearchListAdItems api start")
                            val newResponse = removeSearchListAdItems(response)
                            if (newResponse != null) {
                                param.result = newResponse
                                log("removeSearchListAdItems api success")
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
     * 移除启动页广告
     */
    private fun removeSplashAd(classLoader: ClassLoader) {
        try {
            log("removeSplashAd start")
            val adClass = XposedHelpers.findClass("com.weibo.mobileads.view.FlashAd", classLoader)
            //保险起见,show()方法和showFromLoadManager()方法都做hook处理
            XposedHelpers.findAndHookMethod(
                adClass, "show",
                Activity::class.java, Intent::class.java,
                SkipSplashAdMethodHook(adClass, "show")
            )
            XposedHelpers.findAndHookMethod(
                adClass, "showFromLoadManager",
                Activity::class.java, Intent::class.java,
                SkipSplashAdMethodHook(adClass, "showFromLoadManager")
            )
        } catch (t: Throwable) {
            log("removeSplashAd fail: ${t.getStackInfo()}")
        }
    }

    private class SkipSplashAdMethodHook(
        adClass: Class<*>,
        private val method: String
    ) : XC_MethodHook() {

        private val isSkipedField = XposedHelpers.findField(adClass, "isSkiped")
        private val dismissMethod = XposedHelpers.findMethodExact(
            adClass, "dismiss", Boolean::class.java
        )

        @SuppressLint("WrongConstant")
        override fun beforeHookedMethod(param: MethodHookParam) {
            try {
                val activity = param.args[0] as? Activity
                val intent = param.args[1] as? Intent
                if (activity != null && !activity.isFinishing) {
                    isSkipedField.set(param.thisObject, true)
                    /**
                     * 1.app启动的时候闪屏页广告的intent不为null
                     * 2.app从后台切回前台(超过一定时间)intent为null
                     */
                    if (intent != null) {
                        intent.flags = 268435456
                        activity.startActivity(intent)
                    }
                    dismissMethod.invoke(param.thisObject, false)
                    //阻止调用原方法
                    param.result = null
                    log("$method removeSplashAd success")
                } else {
                    log("$method removeSplashAd fail: params invalid")
                }
            } catch (t: Throwable) {
                log("$method removeSplashAd fail: ${t.getStackInfo()}")
            }
        }
    }

    /**
     * 移除列表(关注,推荐)广告
     */
    private fun removeListAdItems(response: Any): Any? {
        try {
            val string = OkHttp.getResponseString(response)
            val newString = removeListAdString(string)
            return OkHttp.createNewResponse(response, newString)
        } catch (t: Throwable) {
            log("removeListAdItems fail: ${t.getStackInfo()}")
        }
        return null
    }

    private fun removeListAdString(string: String): String {
        try {
            val result = JSONObject(string)
            //不知道是什么字段,反正设置为空准没错
            result.put("ad", "")
            val statuses = result.optJSONArray("statuses") ?: JSONArray()
            var adItemCount = 0
            JsonUtils.removeJSONArrayElements(statuses) block@{ item ->
                val isAdItem = item.optInt("mblogtype") == 1
                if (isAdItem) {
                    adItemCount++
                }
                return@block isAdItem
            }
            log("removeListAdString success: $adItemCount")
            return result.toString()
        } catch (t: Throwable) {
            log("removeListAdString fail: ${t.getStackInfo()}")
        }
        return string
    }

    /**
     * 移除热点列表广告
     */
    private fun removeCardListAdItems(response: Any): Any? {
        try {
            val string = OkHttp.getResponseString(response)
            val newString = removeCardListAdString(string)
            return OkHttp.createNewResponse(response, newString)
        } catch (t: Throwable) {
            log("removeCardListAdItems fail: ${t.getStackInfo()}")
        }
        return null
    }

    private fun removeCardListAdString(string: String): String {
        try {
            val result = JSONObject(string)
            val cards = result.optJSONArray("cards") ?: JSONArray()
            var adItemCount = 0
            JsonUtils.removeJSONArrayElements(cards) block@{ item ->
                val mblog = item.optJSONObject("mblog") ?: return@block false
                val isAdItem = mblog.optInt("mblogtype") == 1
                if (isAdItem) {
                    adItemCount++
                }
                return@block isAdItem
            }
            log("removeCardListAdString success: $adItemCount")
            return result.toString()
        } catch (t: Throwable) {
            log("removeCardListAdString fail: ${t.getStackInfo()}")
        }
        return string
    }

    /**
     * 视频详情页
     */
    private fun removeVideoDetailAds(response: Any): Any? {
        try {
            val string = OkHttp.getResponseString(response)
            val newString = removeVideoDetailAdString(string)
            return OkHttp.createNewResponse(response, newString)
        } catch (t: Throwable) {
            log("removeVideoDetailAds fail: ${t.getStackInfo()}")
        }
        return null
    }

    private fun removeVideoDetailAdString(string: String): String {
        try {
            val result = JSONObject(string)
            result.put("expandable_views", "")
            log("removeVideoDetailAdString success")
            return result.toString()
        } catch (t: Throwable) {
            log("removeVideoDetailAdString fail: ${t.getStackInfo()}")
        }
        return string
    }

    /**
     * 微博详情页
     */
    private fun removeDetailWeiboAds(response: Any): Any? {
        try {
            val string = OkHttp.getResponseString(response)
            val newString = removeDetailWeiboAdString(string)
            return OkHttp.createNewResponse(response, newString)
        } catch (t: Throwable) {
            log("removeDetailWeiboAds fail: ${t.getStackInfo()}")
        }
        return null
    }

    private fun removeDetailWeiboAdString(string: String): String {
        try {
            val result = JSONObject(string)
            result.put("trend", "")
            log("removeDetailWeiboAdString success")
            return result.toString()
        } catch (t: Throwable) {
            log("removeDetailWeiboAdString fail: ${t.getStackInfo()}")
        }
        return string
    }

    /**
     * 移除视频列表页广告
     */
    private fun removeVideoListAdItems(response: Any): Any? {
        try {
            val string = OkHttp.getResponseString(response)
            val newString = removeVideoListAdString(string)
            return OkHttp.createNewResponse(response, newString)
        } catch (t: Throwable) {
            log("removeVideoListAdItems fail: ${t.getStackInfo()}")
        }
        return null
    }

    private fun removeVideoListAdString(string: String): String {
        try {
            val result = JSONObject(string)
            val statuses = result.optJSONArray("statuses") ?: JSONArray()
            var adItemCount = 0
            JsonUtils.removeJSONArrayElements(statuses) block@{ item ->
                val videoInfo = item.optJSONObject("video_info") ?: return@block false
                val isAdItem = videoInfo.optJSONObject("ad_info") != null
                if (isAdItem) {
                    adItemCount++
                }
                return@block isAdItem
            }
            log("removeVideoListAdString success: $adItemCount")
            return result.toString()
        } catch (t: Throwable) {
            log("removeVideoListAdString fail: ${t.getStackInfo()}")
        }
        return string
    }

    /**
     * 评论列表
     */
    private fun removeCommentListAdItems(response: Any): Any? {
        try {
            val string = OkHttp.getResponseString(response)
            val newString = removeCommentListAdItemString(string)
            return OkHttp.createNewResponse(response, newString)
        } catch (t: Throwable) {
            log("removeCommentListAdItems fail: ${t.getStackInfo()}")
        }
        return null
    }

    private fun removeCommentListAdItemString(string: String): String {
        try {
            val result = JSONObject(string)
            val datas = result.optJSONArray("datas") ?: JSONArray()
            var adItemCount = 0
            JsonUtils.removeJSONArrayElements(datas) block@{ item ->
                val data = item.optJSONObject("data") ?: return@block false
                val isAdItem = data.optInt("mblogtype") == 1
                if (isAdItem) {
                    adItemCount++
                }
                return@block isAdItem
            }
            log("removeCommentListAdItemString success: $adItemCount")
            return result.toString()
        } catch (t: Throwable) {
            log("removeCommentListAdItemString fail: ${t.getStackInfo()}")
        }
        return string
    }

    /**
     * 检查更新
     */
    private fun disableCheckUpgrade(response: Any): Any? {
        try {
            val result = JSONObject().apply {
                put("version", "")
                put("download", "")
                put("wapurl", "")
                put("md5", "")
                put("desc", "")
                put("changelog", "")
                put("prompt", "")
                put("addtime", "")
                put("poptime", -1)
            }
            return OkHttp.createNewResponse(response, result.toString())
        } catch (t: Throwable) {
            log("disableCheckUpgrade fail: ${t.getStackInfo()}")
        }
        return null
    }

    /**
     * 移除我的 tab 页广告
     */
    private fun removeMinePageAds(response: Any): Any? {
        try {
            val string = OkHttp.getResponseString(response)
            val newString = removeMinePageAdString(string)
            return OkHttp.createNewResponse(response, newString)
        } catch (t: Throwable) {
            log("removeMinePageAds fail: ${t.getStackInfo()}")
        }
        return null
    }

    private fun removeMinePageAdString(string: String): String {
        try {
            val result = JSONObject(string)
            //去除我也不知道是什么的banner
            result.put("banners", "")
            val cards = result.optJSONArray("cards") ?: JSONArray()
            var adItemCount = 0
            JsonUtils.removeJSONArrayElements(cards) block@{ item ->
                val cardid = item.optString("cardid")
                //微公益
                if (cardid == "100505_-_publicwelfare") {
                    adItemCount++
                    return@block true
                }
                //追热点领红包
                if (cardid == "100505_-_sinanews2021") {
                    adItemCount++
                    return@block true
                }
                //更多功能卡片
                if (cardid == "100505_-_managecard") {
                    adItemCount++
                    return@block true
                }
                //免流量
                if (cardid == "100505_-_draft") {
                    adItemCount++
                    return@block true
                }
                return@block false
            }
            log("removeMinePageAdString success: $adItemCount")
            return result.toString()
        } catch (t: Throwable) {
            log("removeMinePageAdString fail: ${t.getStackInfo()}")
        }
        return string
    }

    /**
     * 移除我的 tab 页广告
     */
    private fun removeMinePageAds2(response: Any): Any? {
        try {
            val string = OkHttp.getResponseString(response)
            val newString = removeMinePageAdString2(string)
            return OkHttp.createNewResponse(response, newString)
        } catch (t: Throwable) {
            log("removeMinePageAds2 fail: ${t.getStackInfo()}")
        }
        return null
    }

    private fun removeMinePageAdString2(string: String): String {
        try {
            val result = JSONObject(string)
            val items = result.optJSONArray("items") ?: JSONArray()
            var adItemCount = 0
            val adItemIds = listOf(
                "100505_-_newusertask",
                "100505_-_sinanews2021",
                "100505_-_newexamination",
                "100505_-_manage",
                "100505_-_mianliuliang"
            )
            JsonUtils.removeJSONArrayElements(items) block@{ item ->
                val itemId = item.optString("itemId")
                val isAdItem = adItemIds.contains(itemId)
                if (isAdItem) {
                    adItemCount++
                }
                return@block isAdItem
            }
            log("removeMinePageAdString2 success: $adItemCount")
            return result.toString()
        } catch (t: Throwable) {
            log("removeMinePageAdString2 fail: ${t.getStackInfo()}")
        }
        return string
    }

    /**
     * 搜索结果列表页
     */
    private fun removeSearchListAdItems(response: Any): Any? {
        try {
            val string = OkHttp.getResponseString(response)
            val newString = removeSearchListAdItemString(string)
            return OkHttp.createNewResponse(response, newString)
        } catch (t: Throwable) {
            log("removeSearchListAdItems fail: ${t.getStackInfo()}")
        }
        return null
    }

    private fun removeSearchListAdItemString(string: String): String {
        try {
            val result = JSONObject(string)
            val cards = result.optJSONArray("cards") ?: JSONArray()
            var adItemCount = 0
            JsonUtils.removeJSONArrayElements(cards) block@{ item ->
                val isAdItem = item.optJSONObject("mblog")?.optInt("mblogtype") == 1
                if (isAdItem) {
                    adItemCount++
                }
                return@block isAdItem
            }
            log("removeSearchListAdItemString success: $adItemCount")
            return result.toString()
        } catch (t: Throwable) {
            log("removeSearchListAdItemString fail: ${t.getStackInfo()}")
        }
        return string
    }

}