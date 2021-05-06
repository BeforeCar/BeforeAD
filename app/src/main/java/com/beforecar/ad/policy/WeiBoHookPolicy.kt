package com.beforecar.ad.policy

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Intent
import com.beforecar.ad.policy.base.IHookPolicy
import com.beforecar.ad.policy.base.getStackInfo
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import java.lang.reflect.Method

/**
 * @author: wangpan
 * @emial: p.wang@aftership.com
 * @date: 2021/5/6
 * 微博
 */
object WeiBoHookPolicy : IHookPolicy() {

    override val TAG: String = "tag_weibo"

    override fun getPackageName(): String {
        return "com.sina.weibo"
    }

    override fun onMainApplicationCreate(application: Application, classLoader: ClassLoader) {
        //移除开屏广告
        removeSplashAd(classLoader)
        //移除列表页广告
        removeListAdItems(classLoader)
        //移除视频列表页广告
        removeVideoListAdItems(classLoader)
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
                if (activity != null && intent != null && !activity.isFinishing) {
                    isSkipedField.set(param.thisObject, true)
                    intent.flags = 268435456
                    activity.startActivity(intent)
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
     * 移除列表广告
     */
    private fun removeListAdItems(classLoader: ClassLoader) {
        try {
            log("removeListAdItems start")
            XposedHelpers.findAndHookMethod(
                "com.sina.weibo.streamservice.adapter.RecyclerViewAdapter", classLoader,
                "setData", List::class.java, Boolean::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val dataList = param.args[0] as? List<Any?> ?: emptyList()
                        if (dataList.isEmpty()) return
                        val newDataList = dataList.toMutableList()
                        val iterator = newDataList.iterator()
                        var hasAdItems = false
                        while (iterator.hasNext()) {
                            val item = iterator.next()
                            //移除广告item
                            if (item == null || isAdItem(item)) {
                                iterator.remove()
                                hasAdItems = true
                            }
                        }
                        param.args[0] = newDataList
                        if (hasAdItems) {
                            log("removeListAdItems success")
                        }
                    }

                    /**
                     * 判断微博列表的item是否是广告item
                     */
                    private fun isAdItem(item: Any): Boolean {
                        try {
                            val data = XposedHelpers.callMethod(item, "getData") ?: return false
                            val field = XposedHelpers.findFieldIfExists(data.javaClass, "mblogtype")
                            return field != null && field.get(data) == 1
                        } catch (t: Throwable) {
                            log("isAdItem fail: ${t.getStackInfo()}")
                        }
                        return false
                    }
                }
            )
        } catch (t: Throwable) {
            log("removeListAdItems fail: ${t.getStackInfo()}")
        }
    }

    /**
     * 移除视频列表页广告
     */
    private fun removeVideoListAdItems(classLoader: ClassLoader) {
        try {
            log("removeVideoListAdItems start")
            XposedHelpers.findAndHookMethod(
                "com.sina.weibo.story.stream.verticalnew.pagegroup.adapter.SVSAdapter",
                classLoader,
                "setData", List::class.java,
                RemoveVideoAdMethodHook("setData", classLoader)
            )
            XposedHelpers.findAndHookMethod(
                "com.sina.weibo.story.stream.verticalnew.pagegroup.adapter.SVSAdapter",
                classLoader,
                "appendData", List::class.java,
                RemoveVideoAdMethodHook("appendData", classLoader)
            )
            XposedHelpers.findAndHookMethod(
                "com.sina.weibo.story.stream.verticalnew.pagegroup.adapter.SVSAdapter",
                classLoader,
                "preAppendData", List::class.java,
                RemoveVideoAdMethodHook("preAppendData", classLoader)
            )
        } catch (t: Throwable) {
            log("removeVideoListAdItems fail: ${t.getStackInfo()}")
        }
    }

    private class RemoveVideoAdMethodHook(
        private val method: String,
        classLoader: ClassLoader
    ) : XC_MethodHook() {

        private val managerInstance: Any
        private val getStatusMethod: Method

        init {
            val managerClass = XposedHelpers.findClass(
                "com.sina.weibo.story.stream.vertical.core.SVSDataManager", classLoader
            )
            managerInstance = managerClass.getDeclaredMethod("getInstance").invoke(null)!!
            getStatusMethod = managerClass.getDeclaredMethod("getStatus", String::class.java)
        }

        override fun beforeHookedMethod(param: MethodHookParam) {
            val dataList = param.args[0] as? List<Any?> ?: emptyList()
            if (dataList.isEmpty()) return
            val newDataList = dataList.toMutableList()
            val iterator = newDataList.iterator()
            var hasAdItems = false
            while (iterator.hasNext()) {
                val blogId = iterator.next() as? String ?: ""
                if (isVideoAdItem(blogId)) {
                    iterator.remove()
                    hasAdItems = true
                }
            }
            param.args[0] = newDataList
            if (hasAdItems) {
                log("removeVideoListAdItems $method success")
            }
        }

        /**
         * 判断是否是视频广告item
         */
        private fun isVideoAdItem(blogId: String): Boolean {
            try {
                val status = getStatusMethod.invoke(managerInstance, blogId) ?: return false
                val videoInfo = XposedHelpers.getObjectField(status, "video_info") ?: return false
                return XposedHelpers.getObjectField(videoInfo, "ad_info") != null
            } catch (t: Throwable) {
                log("isVideoAdItem fail: ${t.getStackInfo()}")
            }
            return false
        }
    }

}