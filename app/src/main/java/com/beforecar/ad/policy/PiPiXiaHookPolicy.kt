package com.beforecar.ad.policy

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import com.beforecar.ad.policy.base.AbsHookPolicy
import com.beforecar.ad.policy.base.getStackInfo
import com.beforecar.ad.utils.FileUtils
import com.beforecar.ad.utils.JsonUtils
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * @author: wangpan
 * @email: p.wang@aftership.com
 * @date: 2021/6/1
 *
 * 皮皮虾
 */
class PiPiXiaHookPolicy : AbsHookPolicy() {

    override val tag: String = "tag_ppx"

    override fun getPackageName(): String {
        return "com.sup.android.superb"
    }

    override fun onMainApplicationBeforeCreate(application: Application, classLoader: ClassLoader) {
        //hook bytedance retrofit2
        hookBytedanceRetrofit2(classLoader)
        //hook HttpService
        hookHttpService(classLoader)
        //启动页广告
        removeSplashAd(application)
        //hook startActivity
        hookStartActivity()
        //保存视频去除水印
        saveVideoWithoutWatermark(classLoader)
        //保存图片去除水印
        saveImageWithoutWatermark(classLoader)
    }

    /**
     * hook bytedance retrofit2
     */
    private fun hookBytedanceRetrofit2(classLoader: ClassLoader) {
        try {
            val interceptorCls = XposedHelpers.findClass("com.bytedance.retrofit2.b", classLoader)
            val responseCls = XposedHelpers.findClass("com.bytedance.retrofit2.client.Response", classLoader)
            val mCls = XposedHelpers.findClass("com.bytedance.retrofit2.m", classLoader)
            XposedHelpers.findAndHookMethod(interceptorCls, "a", responseCls, mCls, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val response = param.args[0] ?: return
                    val body = getBodyFromResponse(response)
                    val url = getUrlFromResponse(response)
                    if (body == null || url.isEmpty()) {
                        return
                    }
                    when {
                        //搜索
                        url.contains("bds/search/?keyword") -> {
                            removeSearchListAdItems(body)
                        }
                    }
                }

                private fun getBodyFromResponse(response: Any): Any? {
                    try {
                        val bodyClass = XposedHelpers.findClass(
                            "com.bytedance.retrofit2.mime.TypedByteArray",
                            response.javaClass.classLoader
                        )
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
            })
        } catch (t: Throwable) {
            log("hookBytedanceRetrofit2 fail: ${t.getStackInfo()}")
        }
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

    /**
     * 移除搜索列表广告
     */
    private fun removeSearchListAdItems(body: Any) {
        try {
            log("removeSearchListAdItems start")
            val string = getBodyString(body)
            val newString = removeSearchListAdItemString(string)
            setBodyString(body, newString)
        } catch (t: Throwable) {
            log("removeSearchListAdItems fail: ${t.getStackInfo()}")
        }
    }

    private fun removeSearchListAdItemString(string: String): String {
        try {
            val result = JSONObject(string)
            val data = result.optJSONObject("data") ?: return string
            val dataItems = data.optJSONArray("data") ?: JSONArray()
            var adItemCount = 0
            JsonUtils.removeJSONArrayElements(dataItems) { item ->
                val adInfo = item.optJSONObject("ad_info")
                val isAdItem = adInfo != null
                if (isAdItem) {
                    adItemCount++
                }
                return@removeJSONArrayElements isAdItem
            }
            log("removeSearchListAdItemString success: $adItemCount")
            return result.toString()
        } catch (t: Throwable) {
            log("removeSearchListAdItemString fail: ${t.getStackInfo()}")
        }
        return string
    }

    /**
     * hook startActivity
     */
    private fun hookStartActivity() {
        try {
            XposedHelpers.findAndHookMethod(
                Activity::class.java, "startActivity",
                Intent::class.java, Bundle::class.java, object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val intent = param.args[0] as Intent
                        val className = intent.component?.className ?: ""
                        when {
                            //禁用青少年弹窗
                            className.contains("EnterTeenagerModeDialogActivity") -> {
                                param.result = null
                                log("disableTeenagerMode success")
                            }
                            //禁用开启推送通知
                            className.contains("AcquireAuthActivity") -> {
                                param.result = null
                                log("disableNotificationDialog success")
                            }
                        }
                    }
                }
            )
        } catch (t: Throwable) {
            log("hookStartActivity fail: ${t.getStackInfo()}")
        }
    }

    /**
     * 去除启动页广告
     */
    private fun removeSplashAd(application: Application) {
        try {
            val parentPath = application.externalCacheDir?.parent ?: ""
            val splashCachePath = "$parentPath/splashCache/"
            FileUtils.deleteFilesInDir(File(splashCachePath))
            log("removeSplashAd success")
        } catch (t: Throwable) {
            log("removeSplashAd fail: ${t.getStackInfo()}")
        }
    }

    private fun hookHttpService(classLoader: ClassLoader) {
        try {
            val httpServiceCls = XposedHelpers.findClass(
                "com.ss.android.socialbase.basenetwork.HttpService",
                classLoader
            )
            val httpRequestCls = XposedHelpers.findClass(
                "com.ss.android.socialbase.basenetwork.HttpRequest",
                classLoader
            )

            XposedHelpers.findAndHookMethod(
                "com.sup.android.module.publish.viewmodel.AtViewModel", classLoader, "a", String::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val keyword = param.args[0] as String
                        log("keyword: $keyword")
                    }
                })


            XposedHelpers.findAndHookMethod(httpServiceCls, "doGet", httpRequestCls, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val request = param.args[0] as Any
                    doHook(request, param)
                }

                private fun doHook(request: Any, param: MethodHookParam) {
                    try {
                        val url = getRequestUrl(request)
                        //log("url: $url")
                        val string = param.result as? String
                        if (string.isNullOrEmpty()) return
                        when {
                            //推荐列表, 视频列表, 关注列表, 收藏列表
                            url.contains("bds/feed/stream")
                                    or url.contains("bds/feed/follow_feed")
                                    or url.contains("bds/user/favorite") -> {
                                log("removeFeedListAdItems api start")
                                val newString = removeFeedListAdItems(string)
                                if (newString != null) {
                                    param.result = newString
                                    log("removeFeedListAdItems api success")
                                }
                            }
                            //视频播放完后的广告
                            url.contains("api/ad/v1/patch") -> {
                                log("removeFeedListAdItems2 api start")
                                val newString = removeFeedListAdItems2(string)
                                if (newString != null) {
                                    param.result = newString
                                    log("removeFeedListAdItems2 api success")
                                }
                            }
                            //评论列表广告1
                            url.contains("bds/cell/cell_comment") -> {
                                log("removeCommentListAdItems1 api start")
                                val newString = removeCommentListAdItems1(string)
                                if (newString != null) {
                                    param.result = newString
                                    log("removeCommentListAdItems1 api success")
                                }
                            }
                            //评论列表广告2
                            url.contains("api/ad/v1/comment") -> {
                                log("removeCommentListAdItems2 api start")
                                val newString = removeCommentListAdItems2(string)
                                if (newString != null) {
                                    param.result = newString
                                    log("removeCommentListAdItems2 api success")
                                }
                            }
                            //回复列表广告
                            url.contains("bds/comment/cell_reply") -> {
                                log("removeReplyListAdItems api start")
                                val newString = removeReplyListAdItems(string)
                                if (newString != null) {
                                    param.result = newString
                                    log("removeReplyListAdItems api success")
                                }
                            }
                            //屏蔽更新
                            url.contains("check_version/v6") -> {
                                log("disableCheckUpgrade api start")
                                val newString = disableCheckUpgrade(string)
                                if (newString != null) {
                                    param.result = newString
                                    log("disableCheckUpgrade api success")
                                }
                            }
                            //即玩小游戏推荐
                            url.contains("tfe/route/micro_recommend/list/v1") -> {
                                log("removeMicroRecommendAdItems api start")
                                val newString = removeMicroRecommendAdItems(string)
                                if (newString != null) {
                                    param.result = newString
                                    log("removeMicroRecommendAdItems api success")
                                }
                            }
                            //启动页广告
                            url.contains("api/ad/splash/super/v14") -> {
                                log("removeSplashAdItems api start")
                                val newString = removeSplashAdItems(string)
                                if (newString != null) {
                                    param.result = newString
                                    log("removeSplashAdItems api success")
                                }
                            }
                        }
                    } catch (t: Throwable) {
                        log("hookHttpService doHook fail: ${t.getStackInfo()}")
                    }
                }
            })
        } catch (t: Throwable) {
            log("hookHttpService fail: ${t.getStackInfo()}")
        }
    }

    @Throws(Throwable::class)
    private fun getRequestUrl(request: Any): String {
        return XposedHelpers.callMethod(request, "getUrl") as String
    }

    /**
     * 去除列表广告
     */
    private fun removeFeedListAdItems(string: String): String? {
        try {
            val result = JSONObject(string)
            val data = result.optJSONObject("data") ?: return null
            val dataItems = data.optJSONArray("data") ?: JSONArray()
            var adItemCount = 0
            JsonUtils.removeJSONArrayElements(dataItems) { item ->
                val isAdItem = isFeedIdItem(item)
                if (isAdItem) {
                    adItemCount++
                }
                return@removeJSONArrayElements isAdItem
            }
            log("removeFeedListAdItems success: $adItemCount")
            return result.toString()
        } catch (t: Throwable) {
            log("removeFeedListAdItems fail: ${t.getStackInfo()}")
        }
        return null
    }

    /**
     * 是否是列表广告
     */
    private fun isFeedIdItem(dataItem: JSONObject): Boolean {
        try {
            //单纯的广告 item
            val adInfo = dataItem.optJSONObject("ad_info")
            if (adInfo != null) {
                return true
            }
            //item 中包含推荐商品
            val promotionInfo = dataItem.optJSONObject("item")?.optJSONObject("promotion_info")
            if (promotionInfo != null) {
                return true
            }
        } catch (t: Throwable) {
            log("isFeedIdItem fail: ${t.getStackInfo()}")
        }
        return false
    }

    /**
     * 去除列表广告
     */
    private fun removeFeedListAdItems2(string: String): String? {
        try {
            val result = JSONObject(string)
            val adItems = result.optJSONArray("ad_item") ?: JSONArray()
            var adItemCount = 0
            if (adItems.length() > 0) {
                result.put("ad_item", "")
                adItemCount = adItems.length()
            }
            log("removeFeedListAdItems2 success: $adItemCount")
            return result.toString()
        } catch (t: Throwable) {
            log("removeFeedListAdItems2 fail: ${t.getStackInfo()}")
        }
        return null
    }

    /**
     * 去除评论列表广告1
     */
    private fun removeCommentListAdItems1(string: String): String? {
        try {
            val result = JSONObject(string)
            val data = result.optJSONObject("data") ?: return null
            val cellComments = data.optJSONArray("cell_comments") ?: JSONArray()
            var adItemCount = 0
            JsonUtils.removeJSONArrayElements(cellComments) { item ->
                val adInfo = item.optJSONObject("ad_info")
                val isAdItem = adInfo != null
                if (isAdItem) {
                    adItemCount++
                }
                return@removeJSONArrayElements isAdItem
            }
            log("removeCommentListAdItems1 success: $adItemCount")
            return result.toString()
        } catch (t: Throwable) {
            log("removeCommentListAdItems1 fail: ${t.getStackInfo()}")
        }
        return null
    }

    /**
     * 去除评论列表广告2
     */
    private fun removeCommentListAdItems2(string: String): String? {
        try {
            val result = JSONObject(string)
            val data = result.optJSONObject("data") ?: return null
            val adItems = data.optJSONArray("ad_item")
            var adItemCount = 0
            if (adItems != null) {
                data.put("ad_item", "")
                adItemCount = adItems.length()
            }
            log("removeCommentListAdItems2 success: $adItemCount")
            return result.toString()
        } catch (t: Throwable) {
            log("removeCommentListAdItems2 fail: ${t.getStackInfo()}")
        }
        return null
    }

    /**
     * 去除回复列表广告
     */
    private fun removeReplyListAdItems(string: String): String? {
        try {
            val result = JSONObject(string)
            val data = result.optJSONObject("data") ?: return null
            val replies = data.optJSONArray("replies") ?: JSONArray()
            var adItemCount = 0
            JsonUtils.removeJSONArrayElements(replies) { item ->
                val adInfo = item.optJSONObject("ad_info")
                val isAdItem = adInfo != null
                if (isAdItem) {
                    adItemCount++
                }
                return@removeJSONArrayElements isAdItem
            }
            log("removeReplyListAdItems success: $adItemCount")
            return result.toString()
        } catch (t: Throwable) {
            log("removeReplyListAdItems fail: ${t.getStackInfo()}")
        }
        return null
    }

    /**
     * 屏蔽更新
     */
    private fun disableCheckUpgrade(string: String): String? {
        try {
            val result = JSONObject(string)
            val data = result.optJSONObject("data")
            if (data != null) {
                result.put("data", "")
                log("disableCheckUpgrade success")
            }
            return result.toString()
        } catch (t: Throwable) {
            log("disableCheckUpgrade fail: ${t.getStackInfo()}")
        }
        return null
    }

    /**
     * 去除即玩小游戏推荐广告
     */
    private fun removeMicroRecommendAdItems(string: String): String? {
        try {
            val result = JSONObject(string)
            val data = result.optJSONArray("data")
            var adItemCount = 0
            if (data != null) {
                result.put("data", "")
                adItemCount = data.length()
            }
            log("removeMicroRecommendAdItems success: $adItemCount")
            return result.toString()
        } catch (t: Throwable) {
            log("removeMicroRecommendAdItems fail: ${t.getStackInfo()}")
        }
        return null
    }

    /**
     * 去除启动页广告
     */
    private fun removeSplashAdItems(string: String): String? {
        try {
            val result = JSONObject(string)
            val data = result.optJSONObject("data")
            if (data != null) {
                result.put("data", "")
                log("removeSplashAdItems success")
            }
            return result.toString()
        } catch (t: Throwable) {
            log("removeSplashAdItems fail: ${t.getStackInfo()}")
        }
        return null
    }

    /**
     * 下载视频去除水印
     */
    private fun saveVideoWithoutWatermark(classLoader: ClassLoader) {
        try {
            //信息流列表视频
            XposedHelpers.findAndHookMethod(
                "com.sup.android.mi.feed.repo.bean.cell.VideoFeedItem", classLoader, "getVideoDownload",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val videoFeedItem = param.thisObject as Any
                        val originDownloadVideoModel = getOriginDownloadVideoModel(videoFeedItem)
                        if (originDownloadVideoModel != null) {
                            param.result = originDownloadVideoModel
                            log("saveVideoWithoutWatermark feedList success")
                        }
                    }

                    private fun getOriginDownloadVideoModel(videoFeedItem: Any): Any? {
                        try {
                            val result1 = XposedHelpers.getObjectField(videoFeedItem, "videoDownload")
                            val result2 = XposedHelpers.getObjectField(videoFeedItem, "originDownloadVideoModel")
                            if (result1 != null && result2 != null && result1.javaClass == result2.javaClass) {
                                return result2
                            }
                        } catch (t: Throwable) {
                            log("getOriginDownloadVideoModel fail: ${t.getStackInfo()}")
                        }
                        return null
                    }
                }
            )
            //评论列表视频
            XposedHelpers.findAndHookMethod(
                "com.sup.android.mi.feed.repo.bean.comment.Comment", classLoader, "getVideoDownloadInfo",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val comment = param.thisObject as Any
                        val playVideoModel = getPlayVideoModel(comment)
                        if (playVideoModel != null) {
                            param.result = playVideoModel
                            log("saveVideoWithoutWatermark commentList success")
                        }
                    }

                    private fun getPlayVideoModel(comment: Any): Any? {
                        try {
                            val result1 = XposedHelpers.getObjectField(comment, "videoDownloadInfo")
                            val result2 = XposedHelpers.getObjectField(comment, "videoInfo")
                            if (result1 != null && result2 != null && result1.javaClass == result2.javaClass) {
                                return result2
                            }
                        } catch (t: Throwable) {
                            log("getPlayVideoModel fail: ${t.getStackInfo()}")
                        }
                        return null
                    }
                }
            )
        } catch (t: Throwable) {
            log("saveVideoWithoutWatermark fail: ${t.getStackInfo()}")
        }
    }

    /**
     * 保存图片去除水印
     */
    private fun saveImageWithoutWatermark(classLoader: ClassLoader) {
        try {
            XposedHelpers.findAndHookMethod(
                "com.sup.android.base.model.ImageModel", classLoader, "getDownloadList",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val imageModel = param.thisObject as Any
                        val urlList = getUrlList(imageModel)
                        if (urlList != null) {
                            param.result = urlList
                            log("saveImageWithoutWatermark success")
                        }
                    }

                    private fun getUrlList(imageModel: Any): Any? {
                        try {
                            val result1 = XposedHelpers.getObjectField(imageModel, "downloadList")
                            val result2 = XposedHelpers.getObjectField(imageModel, "urlList")
                            if (result1 != null && result2 != null && result1.javaClass == result2.javaClass) {
                                return result2
                            }
                        } catch (t: Throwable) {
                            log("getUrlList fail: ${t.getStackInfo()}")
                        }
                        return null
                    }
                }
            )
        } catch (t: Throwable) {
            log("saveImageWithoutWatermark fail: ${t.getStackInfo()}")
        }
    }

}