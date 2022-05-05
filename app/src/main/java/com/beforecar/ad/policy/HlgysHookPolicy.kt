package com.beforecar.ad.policy

import android.app.Application
import com.beforecar.ad.policy.base.AbsHookPolicy
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers


class HlgysHookPolicy : AbsHookPolicy() {

    override val tag: String
        get() = "HlgysHookPolicy"

    override fun onMainApplicationBeforeCreate(application: Application, classLoader: ClassLoader) {
        super.onMainApplicationBeforeCreate(application, classLoader)
        log("onFirstValidActivityPreOnCreate")
        val l = XposedHelpers.findClass("h.d0.c.l", classLoader)
        XposedHelpers.findAndHookMethod(
            "com.junyue.repository.config.ConfigBean",
            classLoader,
            "V",
            Int::class.java,
            l,
            Boolean::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    log("params:" + param.args[0])
                    param.result = false
                }
            }
        )
    }

    override fun getPackageName(): String {
        return "com.hlgys.hlg"
    }

}