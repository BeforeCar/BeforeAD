package com.beforecar.ad.policy

import android.app.Application
import com.beforecar.ad.policy.base.AbsHookPolicy
import com.beforecar.ad.policy.base.getStackInfo
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

/**
 * @author: wangpan
 * @email: p.wang@aftership.com
 * @date: 2021/5/6
 * 12306
 */
class MobileTicketHookPolicy : AbsHookPolicy() {

    override val tag: String = "tag_12306"

    override fun getPackageName(): String {
        return "com.MobileTicket"
    }

    override fun onMainApplicationAfterCreate(application: Application, classLoader: ClassLoader) {
        removeMainPageAd(classLoader)
        removePopupAd(classLoader)
    }

    /**
     * 移除首页全屏广告
     */
    private fun removeMainPageAd(classLoader: ClassLoader) {
        try {
            log("removeMainPageAd start")
            XposedHelpers.findAndHookMethod(
                "com.bontai.mobiads.ads.splash.SplashAdView", classLoader, "isNeedShowAd",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        param.result = false
                        log("removeMainPageAd success")
                    }
                }
            )
        } catch (t: Throwable) {
            log("removeMainPageAd fail: ${t.getStackInfo()}")
        }
    }

    /**
     * 移除首页弹窗广告
     */
    private fun removePopupAd(classLoader: ClassLoader) {
        try {
            log("removePopupAd start")
            XposedHelpers.findAndHookMethod(
                "com.MobileTicket.netrequest.AdPopUpRequest", classLoader, "requestAdPopUp",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        param.result = null
                        log("removePopupAd success")
                    }
                }
            )
        } catch (t: Throwable) {
            log("removePopupAd fail: ${t.getStackInfo()}")
        }
    }
}