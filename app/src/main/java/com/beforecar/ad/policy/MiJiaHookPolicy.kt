package com.beforecar.ad.policy

import android.app.Application
import com.beforecar.ad.policy.base.IHookPolicy
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

/**
 * @author: wangpan
 * @emial: p.wang@aftership.com
 * @date: 2021/5/6
 * 米家
 */
class MiJiaHookPolicy : IHookPolicy() {

    override val TAG: String = "tag_mijia"

    override fun getPackageName(): String {
        return "com.xiaomi.smarthome"
    }

    override fun onMainApplicationCreate(application: Application, classLoader: ClassLoader) {
        removeHomePageAd(classLoader)
        removeMinePageAd(classLoader)
    }

    /**
     * 移除米家首页 Banner 广告
     */
    private fun removeHomePageAd(classLoader: ClassLoader) {
        try {
            log("removeHomePageAd start")
            XposedHelpers.findAndHookMethod(
                "_m_j.hdy", classLoader, "O000000o", String::class.java, object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        param.result = emptyList<Any>()
                        log("removeHomePageAd success")
                    }
                }
            )
        } catch (t: Throwable) {
            log("removeHomePageAd fail: ${t.getStackInfo()}")
        }
    }

    /**
     * 移除我的页面广告
     */
    private fun removeMinePageAd(classLoader: ClassLoader) {
        try {
            log("removeMinePageAd start")
            XposedHelpers.findAndHookMethod(
                "_m_j.hea", classLoader, "O000000o", String::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        param.result = emptyList<Any>()
                        log("removeMinePageAd success")
                    }
                }
            )
        } catch (t: Throwable) {
            log("removeMinePageAd fail: ${t.getStackInfo()}")
        }
    }

}