package com.beforecar.ad.policy.douyin

import android.app.Application
import com.beforecar.ad.policy.base.AbsHookPolicy
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers


class DouyinHookPolicy : AbsHookPolicy() {

    override val tag: String
        get() = "DouyinHookPolicy"

    override fun onMainApplicationBeforeCreate(application: Application, classLoader: ClassLoader) {
        super.onMainApplicationBeforeCreate(application, classLoader)
        log("onFirstValidActivityPreOnCreate")
        val context = XposedHelpers.findClass("android.content.Context", classLoader)
        XposedHelpers.findAndHookMethod(
            "com.bytedance.frameworks.baselib.network.http.util.a",
            classLoader,
            "LIZ",
            context,
            String::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam?) {
                    super.afterHookedMethod(param)
                    log("result:" + param?.result.toString())
                    log("args0:" + param?.args?.get(0))
                    log("args1:" + param?.args?.get(1))
                }
            }
        )

        XposedHelpers.findAndHookMethod(
            "org.json.JSONObject",
            classLoader,
            "optBoolean",
            String::class.java,
            Boolean::class.java,
            object : XC_MethodHook() {

                override fun beforeHookedMethod(param: MethodHookParam?) {
                    if (param?.args?.get(0) == "enable_encrypt") {
                        param.result = false
                    }
                    if (param?.args?.get(0) == "disabledInDebug") {
                        param.result = true
                    }
                }

            }
        )
    }

    override fun getPackageName(): String {
        return "com.ss.android.ugc.aweme"
    }

}