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
        //移除 Splash 广告
        removeSplash(classLoader)
    }

    /**
     * 移除缓存的 splash 广告
     */
    private fun removeSplash(classLoader: ClassLoader) {
        try {
            XposedHelpers.findAndHookMethod(
                "com.nearme.splash.loader.plugin.net.SplashPluginTransaction", classLoader,
                "requestSplash", String::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        param.result = null
                        log("removeSplash requestSplash success")
                    }
                }
            )
            XposedHelpers.findAndHookMethod(
                "com.nearme.splash.util.SplashUtil", classLoader,
                "getCacheSplash", object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        param.result = null
                        log("removeSplash removeCacheSplash success")
                    }
                }
            )
        } catch (t: Throwable) {
            log("removeSplash fail: ${t.getStackInfo()}")
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

}