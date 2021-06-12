package com.beforecar.ad.policy

import android.app.Application
import com.beforecar.ad.policy.base.AbsHookPolicy
import com.beforecar.ad.policy.base.getStackInfo
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

/**
 * @author: wangpan
 * @email: p.wang@aftership.com
 * @date: 2021/6/11
 *
 * oppo 主题
 */
class HeytapThemeHookPolicy : AbsHookPolicy() {

    override val tag: String = "tag_otheme"

    override fun getPackageName(): String {
        return "com.heytap.themestore"
    }

    override fun onMainApplicationBeforeCreate(application: Application, classLoader: ClassLoader) {
        //移除启动页广告
        removeSplashAd(classLoader)
    }

    /**
     * 移除启动页广告
     */
    private fun removeSplashAd(classLoader: ClassLoader) {
        try {
            val splashDtoCls = XposedHelpers.findClass(SplashDto, classLoader)
            XposedHelpers.findAndHookMethod(splashDtoCls, "getStartTime", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    param.result = Long.MAX_VALUE
                    log("removeSplashAd getStartTime success")
                }
            })
            XposedHelpers.findAndHookMethod(splashDtoCls, "getEndTime", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    param.result = Long.MIN_VALUE
                    log("removeSplashAd getEndTime success")
                }
            })
        } catch (t: Throwable) {
            log("removeSplashAd fail: ${t.getStackInfo()}")
        }
    }

    companion object {

        const val SplashDto = "com.oppo.cdo.card.theme.dto.SplashDto"

    }
}