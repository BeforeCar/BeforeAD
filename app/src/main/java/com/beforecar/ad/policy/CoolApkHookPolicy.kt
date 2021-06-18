package com.beforecar.ad.policy

import android.app.Application
import android.content.Context
import com.beforecar.ad.policy.base.AbsHookPolicy
import com.beforecar.ad.policy.base.getStackInfo
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * @author: wangpan
 * @email: p.wang@aftership.com
 * @date: 2021/6/15
 *
 * 酷安
 */
class CoolApkHookPolicy : AbsHookPolicy() {

    override val tag: String = "tag_coolapk"

    override fun getPackageName(): String {
        return "com.coolapk.market"
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        super.handleLoadPackage(lpparam)
        hookMyWrapperProxyApplication(lpparam.classLoader!!)
    }

    private fun hookMyWrapperProxyApplication(classLoader: ClassLoader) {
        try {
            XposedHelpers.findAndHookMethod(
                MyWrapperProxyApplication, classLoader,
                "initProxyApplication", Context::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val application = param.thisObject as Application
                        //hook 今日头条穿山甲 SDK
                        //hookHttpStack(application.classLoader!!)
                        hookCoolMarketApplication(application.classLoader!!)
                    }

                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val application = param.thisObject as Application
                        try {
                            val mainActivity = XposedHelpers.findClass(
                                "com.coolapk.market.view.main.MainActivity",
                                application.classLoader
                            )
                            log("mainActivity success")
                        } catch (t: Throwable) {
                            log("mainActivity fail")
                        }
                    }
                }
            )
        } catch (t: Throwable) {
            log("hookMyWrapperProxyApplication fail: ${t.getStackInfo()}")
        }
    }

    private fun hookCoolMarketApplication(classLoader: ClassLoader) {
        try {
            XposedHelpers.findAndHookMethod(
                CoolMarketApplication, classLoader, "onCreate", object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val application = param.thisObject as Application
                        //hook 今日头条穿山甲 SDK
                        hookHttpStack(application.classLoader!!)
                    }

                    override fun beforeHookedMethod(param: MethodHookParam) {

                    }

                }
            )
        } catch (t: Throwable) {
            log("hookCoolMarketApplication fail: ${t.getStackInfo()}")
        }
    }

    /**
     * hook 今日头条穿山甲 SDK
     */
    private fun hookHttpStack(classLoader: ClassLoader) {
        try {
            val httpStackCls = XposedHelpers.findClass(HttpStackImpl, classLoader)
            val requestCls = XposedHelpers.findClass(Request, classLoader)
            XposedHelpers.findAndHookMethod(
                httpStackCls, "performRequest",
                requestCls, Map::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val request = param.args[0] ?: return
                        val url = getUrlFromRequest(request)
                        log("url: $url")
                    }
                }
            )
        } catch (t: Throwable) {
            log("hookHttpStack fail: ${t.getStackInfo()}")
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

    companion object {
        const val MyWrapperProxyApplication = "com.coolapk.market.MyWrapperProxyApplication"
        const val CoolMarketApplication = "com.coolapk.market.CoolMarketApplication"
        const val HttpStackImpl = "com.bytedance.sdk.adnet.core.j"
        const val Request = "com.bytedance.sdk.adnet.core.Request"
    }

}