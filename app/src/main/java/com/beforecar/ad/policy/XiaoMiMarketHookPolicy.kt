package com.beforecar.ad.policy

import android.app.Activity
import android.app.Application
import com.beforecar.ad.policy.base.IHookPolicy
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedHelpers

/**
 * @author: wangpan
 * @emial: p.wang@aftership.com
 * @date: 2021/5/6
 * 小米应用商店
 */
class XiaoMiMarketHookPolicy : IHookPolicy() {

    override val TAG: String = "tag_xiaomi_market"

    override fun getPackageName(): String {
        return "com.xiaomi.market"
    }

    override fun onMainApplicationCreate(application: Application, classLoader: ClassLoader) {
        removeSplashAd(classLoader)
    }

    /**
     * 移除启动页广告
     */
    private fun removeSplashAd(classLoader: ClassLoader) {
        try {
            log("removeSplashAd start")
            XposedHelpers.findAndHookMethod(
                "com.xiaomi.market.ui.BaseActivity",
                classLoader, "needShowSplash", Activity::class.java,
                object : XC_MethodReplacement() {
                    override fun replaceHookedMethod(param: MethodHookParam): Any {
                        log("removeSplashAd success")
                        return false
                    }
                }
            )
        } catch (t: Throwable) {
            log("removeSplashAd fail: ${t.getStackInfo()}")
        }
    }

}