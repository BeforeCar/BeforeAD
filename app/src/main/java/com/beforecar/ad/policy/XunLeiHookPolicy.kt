package com.beforecar.ad.policy

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.beforecar.ad.policy.base.AbsHookPolicy
import com.beforecar.ad.policy.base.getStackInfo
import com.beforecar.ad.utils.JsonUtils
import com.beforecar.ad.utils.OkHttp
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import org.json.JSONObject

/**
 * @author: wangpan
 * @email: p.wang@aftership.com
 * @date: 2021/5/22
 *
 * 迅雷
 */
class XunLeiHookPolicy : AbsHookPolicy() {

    override val tag: String = "tag_xunlei"

    override fun getPackageName(): String {
        return "com.xunlei.downloadprovider"
    }

    override fun onMainApplicationBeforeCreate(application: Application, classLoader: ClassLoader) {
        //hook okhttp BridgeInterceptor
        hookBridgeInterceptor(classLoader)
        //removeHomeTabs
        removeHomeTabs(classLoader)
        //移除用户中心页面福利入口
        removeUserCenterView(classLoader)
        //移除首页点击返回弹窗
        hookMainTabActivityBack(classLoader)
        //移除用户中心更多服务
        removeUserCenterServices(classLoader)
    }

    private fun removeUserCenterServices(classLoader: ClassLoader) {
        try {
            XposedHelpers.findAndHookMethod(
                "com.xunlei.downloadprovider.personal.usercenter.model.b", classLoader,
                "c", object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        doRemove(param.thisObject)
                    }

                    @Suppress("UNCHECKED_CAST")
                    private fun doRemove(userCenterDataManager: Any) {
                        try {
                            val list = XposedHelpers.getObjectField(userCenterDataManager, "b") as MutableList<Any?>
                            val iterator = list.iterator()
                            val removeList = listOf(8, 6, 12)
                            while (iterator.hasNext()) {
                                val item = iterator.next() ?: continue
                                val itemType = XposedHelpers.getIntField(item, "a") as Int
                                if (itemType in removeList) {
                                    iterator.remove()
                                }
                            }
                            log("removeUserCenterServices success")
                        } catch (t: Throwable) {
                            log("removeUserCenterServices doRemove fail: ${t.getStackInfo()}")
                        }
                    }
                }
            )
        } catch (t: Throwable) {
            log("removeUserCenterServices fail: ${t.getStackInfo()}")
        }
    }

    /**
     * 移除首页点击返回确认退出弹窗
     */
    private fun hookMainTabActivityBack(classLoader: ClassLoader) {
        try {
            XposedHelpers.findAndHookMethod(
                "com.xunlei.downloadprovider.frame.MainTabActivity", classLoader,
                "r", object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val activity = param.thisObject as Activity
                        activity.finish()
                        param.result = null
                    }
                })
        } catch (t: Throwable) {
            log("hookMainTabActivityBack fail: ${t.getStackInfo()}")
        }
    }

    /**
     * 移除用户中心页面福利入口
     */
    private fun removeUserCenterView(classLoader: ClassLoader) {
        try {
            val fragmentCls = XposedHelpers.findClass(
                "com.xunlei.downloadprovider.personal.UserCenterFragment",
                classLoader
            )
            val holderCls = XposedHelpers.findClass(
                "com.xunlei.downloadprovider.personal.usercenter.info.UserCenterInfoViewHolder",
                classLoader
            )
            XposedHelpers.findAndHookConstructor(
                holderCls, ViewGroup::class.java, fragmentCls, Activity::class.java, object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        doRemove(param.thisObject)
                    }

                    @SuppressLint("ResourceType")
                    private fun doRemove(viewHolder: Any) {
                        try {
                            val itemView = XposedHelpers.getObjectField(viewHolder, "itemView") as View
                            val welfarePrizeIv = itemView.findViewById<View>(2131368058)
                            if (welfarePrizeIv != null) {
                                welfarePrizeIv.isVisible = false
                            }
                        } catch (t: Throwable) {
                            log("removeUserCenterView1 fail: ${t.getStackInfo()}")
                        }
                    }
                }
            )
            XposedHelpers.findAndHookMethod(holderCls, "a", Any::class.java, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    doRemove(param.thisObject)
                }

                @SuppressLint("ResourceType")
                private fun doRemove(viewHolder: Any) {
                    try {
                        log("doRemove start")
                        val itemView = XposedHelpers.getObjectField(viewHolder, "itemView") as ViewGroup
                    } catch (t: Throwable) {
                        log("removeUserCenterView2 fail: ${t.getStackInfo()}")
                    }
                }
            })
        } catch (t: Throwable) {
            log("removeUserCenterView fail: ${t.getStackInfo()}")
        }
    }

    /**
     * removeHomeTabs
     */
    private fun removeHomeTabs(classLoader: ClassLoader) {
        try {
            XposedHelpers.findAndHookMethod(
                "com.xunlei.downloadprovider.d.c.g", classLoader, "a", object : XC_MethodHook() {
                    @Suppress("UNCHECKED_CAST")
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val list = param.result as MutableList<Any?>
                        doRemove(list)
                    }

                    private fun doRemove(list: MutableList<Any?>) {
                        try {
                            val iterator = list.iterator()
                            while (iterator.hasNext()) {
                                val item = iterator.next() ?: continue
                                val keyField = XposedHelpers.findFieldIfExists(item.javaClass, "f511a") ?: continue
                                val key = keyField.get(item) as String
                                if (key != "square") {
                                    iterator.remove()
                                }
                            }
                            log("removeHomeTabs success")
                        } catch (t: Throwable) {
                            log("removeHomeTabs fail: ${t.getStackInfo()}")
                        }
                    }
                }
            )
        } catch (t: Throwable) {
            log("removeHomeTabs fail: ${t.getStackInfo()}")
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
                        //启动页广告
                        url.contains("ad/fetch_splash_v2") -> {
                            log("removeSplashAd api start")
                            val emptyResponse = OkHttp.createEmptyResponse(response)
                            if (emptyResponse != null) {
                                param.result = emptyResponse
                                log("removeSplashAd api success")
                            }
                        }
                        //广场信息流
                        url.contains("square/v1/resources/feed") -> {
                            log("removeSquareFeeds api start")
                            val newResponse = removeSquareFeeds(response)
                            if (newResponse != null) {
                                param.result = newResponse
                                log("removeSquareFeeds api success")
                            }
                        }
                        //全局配置
                        url.contains("v1/global/get_cfg?config_type=global") -> {
                            log("globalConfig api start")
                            val newResponse = globalConfig(response)
                            if (newResponse != null) {
                                param.result = newResponse
                                log("globalConfig api success")
                            }
                        }
                        //信息流广告
                        url.contains("ad/fetch_feed_ad") -> {
                            log("removeFeedsAd api start")
                            val newResponse = removeFeedsAd(response)
                            if (newResponse != null) {
                                param.result = newResponse
                                log("removeFeedsAd api success")
                            }
                        }
                        //advertisement
                        url.contains("shoulei/advertisement") -> {
                            log("advertisement api start")
                            val newResponse = removeAdvertisement(response)
                            if (newResponse != null) {
                                param.result = newResponse
                                log("advertisement api success")
                            }
                        }
                        //用户中心页面会员推广
                        url.contains("config/shoulei/mytab.json") -> {
                            log("userCenterVipAd api start")
                            val emptyResponse = OkHttp.createEmptyResponse(response)
                            if (emptyResponse != null) {
                                param.result = emptyResponse
                                log("userCenterVipAd api success")
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
     * 全局配置
     */
    private fun globalConfig(response: Any): Any? {
        try {
            val string = OkHttp.getResponseString(response)
            val newString = globalConfigString(string)
            return OkHttp.createNewResponse(response, newString)
        } catch (t: Throwable) {
            log("globalConfig fail: ${t.getStackInfo()}")
        }
        return null
    }

    private fun globalConfigString(string: String): String {
        try {
            val result = JSONObject(string)
            val values = result.optJSONObject("values") ?: return string
            //pushConfig
            pushConfig(values)
            //tabs
            mainTabs(values)
            //myTab
            myTab(values)
            return result.toString()
        } catch (t: Throwable) {
            log("globalConfigString fail: ${t.getStackInfo()}")
        }
        return string
    }

    /**
     * myTab
     */
    private fun myTab(values: JSONObject) {
        try {
            val myTab = values.optJSONObject("my_tab") ?: return
            val myTool = myTab.optJSONArray("my_tool")
            if (myTool != null) {
                val removeList = listOf("score_center", "live_welfare")
                JsonUtils.removeJSONArrayElements(myTool) block@{ item ->
                    val keyword = item.optString("keyword")
                    return@block keyword in removeList
                }
                myTab.put("tool_size", myTool.length())
            }
            val myService = myTab.optJSONArray("my_service")
            if (myService != null) {
                myTab.put("my_service", "")
            }
        } catch (t: Throwable) {
            log("myTab fail: ${t.getStackInfo()}")
        }
    }

    /**
     * mainTabs
     */
    private fun mainTabs(values: JSONObject) {
        try {
            val tabs = values.optJSONArray("tabs") ?: return
            JsonUtils.removeJSONArrayElements(tabs) block@{ item ->
                val tabKey = item.optString("tabKey")
                return@block tabKey != "square"
            }
        } catch (t: Throwable) {
            log("mainTabs fail: ${t.getStackInfo()}")
        }
    }

    /**
     * PushConfig
     */
    private fun pushConfig(values: JSONObject) {
        try {
            val push = values.optJSONObject("push") ?: return
            val types = push.optJSONArray("should_report_display_types")
            if (types != null) {
                push.put("should_report_display_types", "")
            }
        } catch (t: Throwable) {
            log("pushConfig fail: ${t.getStackInfo()}")
        }
    }

    /**
     * 信息流广告
     */
    private fun removeFeedsAd(response: Any): Any? {
        try {
            val string = OkHttp.getResponseString(response)
            val newString = removeFeedsAdString(string)
            return OkHttp.createNewResponse(response, newString)
        } catch (t: Throwable) {
            log("removeFeedsAd fail: ${t.getStackInfo()}")
        }
        return null
    }

    private fun removeFeedsAdString(string: String): String {
        try {
            val result = JSONObject(string)
            val data = result.optJSONObject("data")
            if (data != null) {
                result.put("data", "")
                log("removeFeedsAdString success")
            }
            return result.toString()
        } catch (t: Throwable) {
            log("removeFeedsAdString fail: ${t.getStackInfo()}")
        }
        return string
    }

    /**
     * 广场信息流
     */
    private fun removeSquareFeeds(response: Any): Any? {
        try {
            val string = OkHttp.getResponseString(response)
            val newString = removeSquareFeedsString(string)
            return OkHttp.createNewResponse(response, newString)
        } catch (t: Throwable) {
            log("removeSquareFeeds fail: ${t.getStackInfo()}")
        }
        return null
    }

    private fun removeSquareFeedsString(string: String): String {
        try {
            val result = JSONObject(string)
            val data = result.optJSONArray("data")
            if (data != null) {
                result.put("data", "")
                log("removeSquareFeedsString success")
            }
            return result.toString()
        } catch (t: Throwable) {
            log("removeFeedListAdString fail: ${t.getStackInfo()}")
        }
        return string
    }

    /**
     * advertisement
     */
    private fun removeAdvertisement(response: Any): Any? {
        try {
            if (!OkHttp.isSuccessful(response)) {
                return null
            }
            val string = OkHttp.getResponseString(response)
            val newString = removeAdvertisementString(string)
            return OkHttp.createNewResponse(response, newString)
        } catch (t: Throwable) {
            log("removeAdvertisement fail: ${t.getStackInfo()}")
        }
        return null
    }

    private fun removeAdvertisementString(string: String): String {
        try {
            val result = JSONObject(string)
            val data = result.optJSONObject("data")
            if (data != null) {
                result.put("data", "")
                log("removeAdvertisementString success")
            }
            return result.toString()
        } catch (t: Throwable) {
            log("removeAdvertisementString fail: ${t.getStackInfo()}")
        }
        return string
    }

}