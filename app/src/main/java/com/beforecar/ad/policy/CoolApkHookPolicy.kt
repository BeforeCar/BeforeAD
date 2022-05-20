package com.beforecar.ad.policy

import android.app.Application
import android.content.Context
import com.beforecar.ad.policy.base.AbsHookPolicy
import com.beforecar.ad.policy.base.getProcessName
import com.beforecar.ad.policy.base.getStackInfo
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.io.IOException

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
        if (lpparam.processName == lpparam.packageName) {
            hookCoolMarketApplication()
        }
    }

    private fun hookCoolMarketApplication() {
        try {
            XposedHelpers.findAndHookMethod(
                Application::class.java, "onCreate", object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val application = param.thisObject as Application
                        if (application.javaClass.name == CoolMarketApplication) {
                            onCoolMarketApplicationCreate(application, application.classLoader!!)
                        }
                    }
                }
            )
        } catch (t: Throwable) {
            log("hookCoolMarketApplication fail: ${t.getStackInfo()}")
        }
    }

    private fun onCoolMarketApplicationCreate(application: Application, classLoader: ClassLoader) {
        if (application.getProcessName() == getPackageName()) {
            onMainApplicationCreate(application, classLoader)
        } else {
            onMinorApplicationCreate(application, classLoader)
        }
    }


    private fun onMainApplicationCreate(application: Application, classLoader: ClassLoader) {
        log("onMainApplicationCreate")
        //hook OkHttpCall
        hookOkHttpCall(classLoader) { param, url ->
            when {
                //检测更新
                url?.contains("v6/apk/checkUpdate") == true -> {
                    param.throwable = IOException("disable check update")
                    log("disableCheckUpdate success")
                }
            }
        }
//        hookOkHttpCall(classLoader)
        //hook 穿山甲 SDK
        hookTTAdSdk(classLoader)
        //hook 广点通 SDK
        hookGDTAdSDK(classLoader)
    }

    private fun onMinorApplicationCreate(application: Application, classLoader: ClassLoader) {
        //no op
    }

    /**
     * hook OkHttpCall
     */
    private fun hookOkHttpCall(classLoader: ClassLoader) {
        try {
            val parseResponseMethod = findParseResponseMethod(classLoader)
            if (parseResponseMethod == null) {
                log("hookOkHttpCall cancel: parseResponseMethod is null")
                return
            }
            XposedBridge.hookMethod(parseResponseMethod, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val okHttpCall = param.thisObject as Any
                    val url = getUrlFromOkHttpCall(okHttpCall)
                    when {
                        //检测更新
                        url.contains("v6/apk/checkUpdate") -> {
                            param.throwable = IOException("disable check update")
                            log("disableCheckUpdate success")
                        }
                    }
                }
            })
        } catch (t: Throwable) {
            log("hookOkHttpCall fail: ${t.getStackInfo()}")
        }
    }

    /**
     * hook 穿山甲 SDK
     */
    private fun hookTTAdSdk(classLoader: ClassLoader) {
        try {
            XposedHelpers.findAndHookMethod(
                TTAdConfig, classLoader,
                "setAppId", String::class.java,
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

    /**
     * hook 广点通 SDK
     */
    private fun hookGDTAdSDK(classLoader: ClassLoader) {
        try {
            XposedHelpers.findAndHookMethod(
                GDTADManager, classLoader, "initWith",
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
        const val CoolMarketApplication = "com.coolapk.market.CoolMarketApplication"

        const val OkHttpCall = "retrofit2.OkHttpCall"
        const val Response = "retrofit2.Response"

        //穿山甲 SDK
        const val TTAdConfig = "com.bytedance.sdk.openadsdk.TTAdConfig"

        //广点通 SDK
        const val GDTADManager = "com.qq.e.comm.managers.GDTADManager"
    }

}