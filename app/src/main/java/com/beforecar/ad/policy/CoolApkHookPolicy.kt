package com.beforecar.ad.policy

import android.app.Application
import android.content.Context
import com.beforecar.ad.policy.base.AbsHookPolicy
import com.beforecar.ad.policy.base.getProcessName
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
            val appCls = XposedHelpers.findClass(MyWrapperProxyApplication, classLoader)
            XposedHelpers.findAndHookMethod(
                appCls, "initProxyApplication", Context::class.java, object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val application = param.thisObject as Application
                        hookCoolMarketApplication(application.classLoader!!)
                    }
                }
            )
        } catch (t: Throwable) {
            log("hookMyWrapperProxyApplication fail: ${t.getStackInfo()}")
        }
    }

    private fun hookCoolMarketApplication(classLoader: ClassLoader) {
        try {
            val appCls = XposedHelpers.findClass(CoolMarketApplication, classLoader)
            XposedHelpers.findAndHookMethod(appCls, "onCreate", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val application = param.thisObject as Application
                    onCoolMarketApplicationCreate(application, application.classLoader!!)
                }
            })
        } catch (t: Throwable) {
            log("hookCoolMarketApplication fail: ${t.getStackInfo()}")
        }
    }

    private fun onCoolMarketApplicationCreate(application: Application, classLoader: ClassLoader) {
        if (application.getProcessName() != getPackageName()) return
        //hook 穿山甲 SDK
        hookTTAdSdk(classLoader)
        //hook 广点通 SDK
        hookGDTAdSDK(classLoader)
    }

    /**
     * hook 今日头条穿山甲 SDK
     */
    private fun hookTTAdSdk(classLoader: ClassLoader) {
        try {
            val ttAdConfigCls = XposedHelpers.findClass(TTAdConfig, classLoader)
            XposedHelpers.findAndHookMethod(
                ttAdConfigCls, "setAppId",
                String::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val appId = param.args[0] as String
                        param.args[0] = System.currentTimeMillis().toString()
                        log("hookTTAdSdk success: $appId")
                    }
                }
            )
        } catch (t: Throwable) {
            log("hookTTAdSdk fail: ${t.getStackInfo()}")
        }
    }

    private fun hookGDTAdSDK(classLoader: ClassLoader) {
        try {
            val gdtADManagerCls = XposedHelpers.findClass(GDTADManager, classLoader)
            XposedHelpers.findAndHookMethod(
                gdtADManagerCls, "initWith",
                Context::class.java, String::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val appId = param.args[1] as String
                        param.args[1] = System.currentTimeMillis().toString()
                        log("hookGDTAdSDK success: $appId")
                    }
                }
            )
        } catch (t: Throwable) {
            log("hookGDTAdSDK fail: ${t.getStackInfo()}")
        }
    }

    companion object {
        const val MyWrapperProxyApplication = "com.coolapk.market.MyWrapperProxyApplication"
        const val CoolMarketApplication = "com.coolapk.market.CoolMarketApplication"

        //穿山甲SDK
        const val TTAdConfig = "com.bytedance.sdk.openadsdk.TTAdConfig"

        //广点通SDK
        const val GDTADManager = "com.qq.e.comm.managers.GDTADManager"
    }

}