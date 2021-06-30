package com.beforecar.ad.policy

import android.app.Application
import com.beforecar.ad.policy.base.AbsHookPolicy
import com.beforecar.ad.policy.base.getStackInfo
import com.beforecar.ad.utils.FileUtils
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import java.io.File

/**
 * @author: wangpan
 * @email: p.wang@aftership.com
 * @date: 2021/6/29
 *
 * 哔哩哔哩
 */
class BilibiliHookPolicy : AbsHookPolicy() {

    override val tag: String = "tag_bilibili"

    override fun getPackageName(): String {
        return "tv.danmaku.bili"
    }

    override fun onMainApplicationBeforeCreate(application: Application, classLoader: ClassLoader) {
        //hook fastjson JSON.parseObject() 方法
        hookJSONParseObject(classLoader)
        //删除启动页广告文件
        deleteSplashAdFile(application)
    }

    private fun hookJSONParseObject(classLoader: ClassLoader) {
        try {
            XposedHelpers.findAndHookMethod(
                JSON, classLoader, "parseObject", String::class.java, Class::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val clazz = param.args[1] as Class<*>
                        val data = param.result ?: return
                        //log("parseObject clazz: ${clazz.name}, data: $data")
                        when (clazz.name) {
                            SplashData -> {
                                //启动页广告数据
                                removeSplashAd(data)
                            }
                        }
                    }
                }
            )
        } catch (t: Throwable) {
            log("hookJSONParseObject fail: ${t.getStackInfo()}")
        }
    }

    /**
     * @param data tv.danmaku.bili.ui.splash.SplashData
     */
    private fun removeSplashAd(data: Any) {
        try {
            val splashList = XposedHelpers.getObjectField(data, "splashList") as? MutableList<*>
            if (!splashList.isNullOrEmpty()) {
                splashList.clear()
            }
            val strategyList = XposedHelpers.getObjectField(data, "strategyList") as? MutableList<*>
            if (!strategyList.isNullOrEmpty()) {
                strategyList.clear()
            }
            log("removeSplashAd success")
        } catch (t: Throwable) {
            log("removeSplashAd fail: ${t.getStackInfo()}")
        }
    }

    /**
     * 删除启动页广告文件
     */
    private fun deleteSplashAdFile(application: Application) {
        try {
            val file = File(application.filesDir, "splash2")
            FileUtils.delete(file)
            log("deleteSplashAdFile success")
        } catch (t: Throwable) {
            log("deleteSplashAdFile fail: ${t.getStackInfo()}")
        }
    }

    companion object {
        const val JSON = "com.alibaba.fastjson.JSON"

        /**
         * 启动页广告数据类
         */
        const val SplashData = "tv.danmaku.bili.ui.splash.SplashData"
    }

}