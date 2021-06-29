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

    companion object {

        const val SystemSplashAdService = "com.miui.systemAdSolution.splashAd.SystemSplashAdService"
        const val ISystemSplashAdService_Stub = "com.miui.systemAdSolution.splashAd.ISystemSplashAdService\$Stub"
        const val IAdListener = "com.miui.systemAdSolution.splashAd.IAdListener"

        const val SplashUIController = "com.miui.zeus.msa.app.splashad.SplashUIController"

    }

    override val tag: String = "tag_msa"

    override fun getPackageName(): String {
        return "com.miui.systemAdSolution"
    }

    override fun onMainApplicationBeforeCreate(application: Application, classLoader: ClassLoader) {
        hookSystemSplashAdService(classLoader)
        removeSplashUI(classLoader)
    }

    override fun onMinorApplicationBeforeCreate(application: Application, classLoader: ClassLoader) {
        hookSystemSplashAdService(classLoader)
        removeSplashUI(classLoader)
    }

    private fun hookSystemSplashAdService(classLoader: ClassLoader) {
        try {
            val serviceCls = XposedHelpers.findClass(SystemSplashAdService, classLoader)
            XposedHelpers.findAndHookMethod(serviceCls, "onCreate", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val service = param.thisObject as Any
                    val serviceBinderCls = findSystemSplashAdServiceBinder(service)
                    log("findSystemSplashAdServiceBinder: $serviceBinderCls")
                    if (serviceBinderCls != null) {
                        removeSplashAd(serviceBinderCls, serviceBinderCls.classLoader!!)
                    }
                }
            })
        } catch (t: Throwable) {
            log("hookSystemSplashAdService fail: ${t.getStackInfo()}")
        }
    }

    private fun findSystemSplashAdServiceBinder(service: Any): Class<*>? {
        try {
            val classLoader = service.javaClass.classLoader!!
            val serviceCls = XposedHelpers.findClass(SystemSplashAdService, classLoader)
            val stubCls = XposedHelpers.findClass(ISystemSplashAdService_Stub, classLoader)
            for (field in serviceCls.declaredFields) {
                field.isAccessible = true
                if (field.type == stubCls) {
                    return field.get(service).javaClass
                }
            }
        } catch (t: Throwable) {
            log("findSystemSplashAdServiceBinder fail: ${t.getStackInfo()}")
        }
        return null
    }

    /**
     * 移除闪屏页广告
     */
    private fun removeSplashAd(serviceBinderCls: Class<*>, classLoader: ClassLoader) {
        try {
            val adListenerClass = XposedHelpers.findClass(IAdListener, classLoader)
            XposedHelpers.findAndHookMethod(
                serviceBinderCls, "requestSplashAd", String::class.java, adListenerClass,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val packageName = param.args[0] as String
                        cancelSplashAd(serviceBinderCls, param.thisObject, packageName)
                        param.result = true
                        log("removeSplashAd success: packageName: $packageName")
                    }

                    private fun cancelSplashAd(binderClass: Class<*>, binder: Any, packageName: String) {
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

    private fun removeSplashUI(classLoader: ClassLoader) {
        try {
            val splashUIControllerCls = XposedHelpers.findClass(SplashUIController, classLoader)
            XposedHelpers.findAndHookMethod(
                splashUIControllerCls, "show", String::class.java, String::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val splashUIController = param.thisObject as Any
                        runOnUIThread { showAfter(splashUIController) }
                        param.result = null
                        log("removeSplashUI show success")
                    }

                    private fun showAfter(splashUIController: Any) {
                        try {
                            XposedHelpers.callMethod(splashUIController, "notifyViewShown")
                            XposedHelpers.callMethod(splashUIController, "dismissView")
                            log("showAfter success")
                        } catch (t: Throwable) {
                            log("showAfter fail: ${t.getStackInfo()}")
                        }
                    }
                }
            )
        } catch (t: Throwable) {
            log("removeSplashUI fail: ${t.getStackInfo()}")
        }
    }

}