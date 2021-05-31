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
 * 小米智能服务
 */
class MIUIMSAHookPolicy : AbsHookPolicy() {

    override val tag: String = "tag_msa"

    override fun getPackageName(): String {
        return "com.miui.systemAdSolution"
    }

    override fun onMainApplicationBeforeCreate(application: Application, classLoader: ClassLoader) {
        removeSplashAd(application, classLoader)
        removeSplashUI(application, classLoader)
    }

    override fun onMinorApplicationBeforeCreate(application: Application, classLoader: ClassLoader) {
        removeSplashAd(application, classLoader)
        removeSplashUI(application, classLoader)
    }

    /**
     * 移除闪屏页广告
     */
    private fun removeSplashAd(application: Application, classLoader: ClassLoader) {
        try {
            val binderClass = XposedHelpers.findClassIfExists(
                "com.miui.systemAdSolution.splashAd.SystemSplashAdService\$2", classLoader
            )
            val adListenerClass = XposedHelpers.findClassIfExists(
                "com.miui.systemAdSolution.splashAd.IAdListener", classLoader
            )
            if (binderClass == null || adListenerClass == null) {
                log("removeSplashAd cancel: $binderClass, $adListenerClass")
                return
            }
            log("removeSplashAd start")
            XposedHelpers.findAndHookMethod(
                binderClass, "requestSplashAd", String::class.java, adListenerClass,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val packageName = param.args[0] as String
                        cancelSplashAd(binderClass, param.thisObject, packageName)
                        param.result = true
                        log("removeSplashAd success: packageName: $packageName")
                    }

                    private fun cancelSplashAd(
                        binderClass: Class<*>,
                        binder: Any,
                        packageName: String
                    ) {
                        try {
                            XposedHelpers.findMethodExact(
                                binderClass, "cancelSplashAd", String::class.java
                            ).invoke(binder, packageName)
                            log("removeSplashAd cancelSplashAd success")
                        } catch (t: Throwable) {
                            log("removeSplashAd cancelSplashAd fail: ${t.getStackInfo()}")
                        }
                    }
                }
            )
        } catch (t: Throwable) {
            log("removeSplashAd fail: ${t.getStackInfo()}")
        }
    }

    private fun removeSplashUI(application: Application, classLoader: ClassLoader) {
        try {
            val sucClass = XposedHelpers.findClassIfExists(
                "com.miui.zeus.msa.app.splashad.SplashUIController", classLoader
            )
            if (sucClass == null) {
                log("removeSplashUI cancel: sucClass: $sucClass")
                return
            }
            log("removeSplashUI start")
            XposedHelpers.findAndHookMethod(
                sucClass, "show", String::class.java, String::class.java, object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val s1 = param.args[0] as String
                        val s2 = param.args[1] as String
                        runOnUIThread { showAfter(param.thisObject) }
                        param.result = null
                        log("removeSplashUI show success: $s1, $s2")
                    }

                    private fun showAfter(sucObj: Any) {
                        try {
                            XposedHelpers.callMethod(sucObj, "notifyViewShown")
                            XposedHelpers.callMethod(sucClass, "dismissView")
                        } catch (t: Throwable) {
                            log("removeSplashUI showAfter success")
                        }
                    }
                }
            )
        } catch (t: Throwable) {
            log("removeSplashUI fail: ${t.getStackInfo()}")
        }
    }

}