package com.beforecar.ad.policy

import com.beforecar.ad.policy.base.AbsHookPolicy
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * 小米智能服务（广告）
 *
 * Author: minminaya  承接东风各式弹头打磨、抛光、刷漆等4S保养工程。
 * Email: minminaya@gmail.com
 * Date:
 *
 */
class MiSystemADPolicy : AbsHookPolicy() {

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        super.handleLoadPackage(lpparam)
    }

    override fun getPackageName(): String {
        return ""
    }

    /**
     * 移除闪屏页广告
     */
//    private fun removeSplashAd() {
//        try {
//            val binderClass = XposedHelpers.findClassIfExists(
//                "com.miui.systemAdSolution.splashAd.SystemSplashAdService\$2", classLoader
//            )
//            val adListenerClass = XposedHelpers.findClassIfExists(
//                "com.miui.systemAdSolution.splashAd.IAdListener", classLoader
//            )
//            if (binderClass == null || adListenerClass == null) {
//                log("removeSplashAd cancel: binderClass: $binderClass, adListenerClass: $adListenerClass")
//                return
//            }
//            log("removeSplashAd start")
//            XposedHelpers.findAndHookMethod(
//                binderClass, "requestSplashAd", String::class.java, adListenerClass, object : XC_MethodHook() {
//                    override fun beforeHookedMethod(param: MethodHookParam) {
//                        val packageName = param.args[0] as String
//                        log("requestSplashAd packageName: $packageName")
//                        cancelSplashAd(binderClass, param.thisObject, packageName)
//                        param.result = true
//                        if (XPrefsUtils.isSkipAdToastEnabled()) {
//                            runOnUIThread { showToast(application, "已成功为您去除启动页广告") }
//                        }
//                    }
//                    private fun cancelSplashAd(binderClass: Class<*>, binder: Any, packageName: String) {
//                        try {
//                            XposedHelpers.findMethodExact(
//                                binderClass, "cancelSplashAd", String::class.java
//                            ).invoke(binder, packageName)
//                        } catch (t: Throwable) {
//                            log("cancelSplashAd fail: ${t.getStackInfo()}")
//                        }
//                    }
//                }
//            )
//        } catch (t: Throwable) {
//            log("removeSplashAd fail: ${t.getStackInfo()}")
//        }
//    }
}