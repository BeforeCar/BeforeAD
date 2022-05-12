package com.beforecar.ad.policy.hlgys

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
        //强制去掉广告
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
        //投屏强制打开
        XposedHelpers.findAndHookMethod(
            "com.junyue.repository.config.ConfigBean",
            classLoader,
            "S",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    param.result = true
                }
            }
        )
        //下载强制打开
        XposedHelpers.findAndHookMethod(
            "com.junyue.repository.config.ConfigBean",
            classLoader,
            "K",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    param.result = true
                }
            }
        )
        //打印host链接
        XposedHelpers.findAndHookMethod(
            "d.g.d.b.d.a",
            classLoader,
            "u2",
            String::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    log("host-url:${param.args[0]}")
                }
            }
        )
        //强制去掉 no_proxy
        XposedHelpers.findAndHookMethod(
            "d.g.d.c.c",
            classLoader,
            "c",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    param.result = true
                }

            }
        )
        //强制可以下载
        XposedHelpers.findAndHookMethod(
            "com.junyue.repository.config.ConfigBean",
            classLoader,
            "R",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    param.result = false
                }

            }
        )
        XposedHelpers.findAndHookMethod(
            "com.junyue.bean2.VideoEpisode",
            classLoader,
            "b",
            object : XC_MethodHook() {

                override fun afterHookedMethod(param: MethodHookParam?) {
                    super.afterHookedMethod(param)
                    log("result:${param?.result}")
                }
            }
        )


    }

    override fun getPackageName(): String {
        return "com.hlgys.hlg"
    }

}