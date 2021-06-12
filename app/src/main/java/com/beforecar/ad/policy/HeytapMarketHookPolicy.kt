package com.beforecar.ad.policy

import android.app.Application
import com.beforecar.ad.policy.base.AbsHookPolicy
import com.beforecar.ad.policy.base.getStackInfo
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import java.io.IOException

/**
 * @author: wangpan
 * @email: p.wang@aftership.com
 * @date: 2021/6/11
 *
 * oppo 应用商店
 */
class HeytapMarketHookPolicy : AbsHookPolicy() {

    override val tag: String = "tag_heytap"

    override fun getPackageName(): String {
        return "com.heytap.market"
    }

    override fun onMainApplicationBeforeCreate(application: Application, classLoader: ClassLoader) {
        //hook BaseNetRequireStore
        hookBaseNetRequireStore(classLoader)
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

    private fun getUrlFromRequest(request: Any): String {
        try {
            return XposedHelpers.callMethod(request, "getUrl") as? String ?: ""
        } catch (t: Throwable) {
            log("getUrlFromRequest fail: ${t.getStackInfo()}")
        }
        return ""
    }

    private fun hookBaseNetRequireStore(classLoader: ClassLoader) {
        try {
            val requireStoreCls = XposedHelpers.findClass("com.nearme.network.BaseNetRequireStore", classLoader)
            val requestCls = XposedHelpers.findClass("com.nearme.network.internal.BaseRequest", classLoader)
            XposedHelpers.findAndHookMethod(requireStoreCls, "execute", requestCls, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val request = param.args[0] as Any
                    val url = getUrlFromRequest(request)
                    when {
                        //启动页广告1
                        url.contains("/splash/actual") -> {
                            param.throwable = IOException("disable splash ad")
                            log("removeSplashAd1 success")
                        }
                        //启动页广告2
                        url.contains("/splash/prefetch") -> {
                            param.throwable = IOException("disable splash ad")
                            log("removeSplashAd2 success")
                        }
                        //启动页广告3
                        url.contains("/buoy/prefetch") -> {
                            param.throwable = IOException("disable splash ad")
                            log("removeSplashAd3 success")
                        }
                    }
                }
            })
        } catch (t: Throwable) {
            log("hookBaseNetRequireStore fail: ${t.getStackInfo()}")
        }
    }

    companion object {

        const val SplashDto = "com.heytap.cdo.splash.domain.dto.v2.SplashDto"

    }

}