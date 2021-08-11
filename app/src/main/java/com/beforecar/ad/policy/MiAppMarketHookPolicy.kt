package com.beforecar.ad.policy

import android.app.Application
import com.beforecar.ad.policy.base.AbsHookPolicy
import com.beforecar.ad.policy.base.getStackInfo
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

/**
 * @author: wangpan
 * @email: p.wang@aftership.com
 * @date: 2021/5/6
 *
 * 小米应用商店
 */
class MiAppMarketHookPolicy : AbsHookPolicy() {

    override val tag: String = "tag_mi_app"

    override fun getPackageName(): String {
        return "com.xiaomi.market"
    }

    override fun onMainApplicationAfterCreate(application: Application, classLoader: ClassLoader) {
        hookNeedShowSplash(classLoader)
        hookGetSplashAdInfo(classLoader)
    }

    private fun hookNeedShowSplash(classLoader: ClassLoader) {
        try {
            val baseActivityCls = XposedHelpers.findClassIfExists(BaseActivity, classLoader)
            if (baseActivityCls == null) {
                log("hookNeedShowSplash cancel: BaseActivity class not found")
                return
            }
            val needShowSplashMethod = baseActivityCls.declaredMethods.find {
                it.name == "needShowSplash"
            }
            if (needShowSplashMethod == null) {
                log("hookNeedShowSplash cancel: needShowSplash method not found")
                return
            }
            XposedBridge.hookMethod(needShowSplashMethod, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    param.result = false
                    log("hookNeedShowSplash success")
                }
            })
        } catch (t: Throwable) {
            log("hookNeedShowSplash fail: ${t.getStackInfo()}")
        }
    }

    private fun hookGetSplashAdInfo(classLoader: ClassLoader) {
        try {
            val adManagerCls = XposedHelpers.findClassIfExists(FocusVideoAdManager, classLoader)
            if (adManagerCls == null) {
                log("getSplashAdInfoMethod cancel: FocusVideoAdManager class not found")
                return
            }
            val getSplashAdInfoMethod = adManagerCls.declaredMethods.find {
                it.name == "getSplashAdInfo"
            }
            if (getSplashAdInfoMethod == null) {
                log("getSplashAdInfoMethod cancel: getSplashAdInfo method not found")
                return
            }
            XposedBridge.hookMethod(getSplashAdInfoMethod, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    param.result = null
                    log("getSplashAdInfoMethod success")
                }
            })
        } catch (t: Throwable) {
            log("hookGetSplashAdInfo fail: ${t.getStackInfo()}")
        }
    }

    companion object {
        const val BaseActivity = "com.xiaomi.market.ui.BaseActivity"
        const val FocusVideoAdManager = "com.xiaomi.market.h52native.FocusVideoAdManager"
    }

}