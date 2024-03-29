package com.beforecar.ad.policy

import android.app.Activity
import android.app.Application
import com.beforecar.ad.policy.base.AbsHookPolicy
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class BiqugeHookPolicy : AbsHookPolicy() {

    override val tag: String
        get() = "BiqugeHookPolicy"

    override fun onMainFirstValidActivityPreOnCreate(
        lpparam: XC_LoadPackage.LoadPackageParam,
        application: Application,
        activity: Activity,
        classLoader: ClassLoader
    ) {
        super.onMainFirstValidActivityPreOnCreate(lpparam, application, activity, classLoader)
        XposedHelpers.findAndHookMethod(
            "com.jni.crypt.project.CryptDesManager",
            classLoader,
            "encodeContent",
            String::class.java,
            object : XC_MethodHook() {

                override fun afterHookedMethod(param: MethodHookParam?) {
                    super.afterHookedMethod(param)
                    log("afterHookedMethod: " + (param?.args?.get(0) ?: ""))
                }
            }
        )
        XposedHelpers.findAndHookMethod(
            "com.biquge.ebook.app.bean.User",
            classLoader,
            "isNoAd",
            object : XC_MethodHook() {

                override fun beforeHookedMethod(param: MethodHookParam?) {
                    super.beforeHookedMethod(param)
                    param?.result = true
                    log("isNoAd: 强行返回无广告")
                }
            }
        )
        XposedHelpers.findAndHookMethod(
            "org.json.JSONObject",
            classLoader,
            "optBoolean",
            String::class.java,
            object : XC_MethodHook() {

                override fun beforeHookedMethod(param: MethodHookParam?) {
                    log("optBoolean key: ${param?.args?.get(0)}")
                    if (param?.args?.get(0) ?: "" == "topmsg_close") {
                        param?.result = true
                    }
                }
            }
        )
        XposedHelpers.findAndHookMethod(
            "d.c.a.a.c.i",
            classLoader,
            "u0",
            object : XC_MethodHook() {

                override fun beforeHookedMethod(param: MethodHookParam?) {
                    param?.result = null
                }
            }
        )
        XposedHelpers.findAndHookMethod(
            "d.c.a.a.c.i",
            classLoader,
            "U0",
            object : XC_MethodHook() {

                override fun beforeHookedMethod(param: MethodHookParam?) {
                    param?.result = null
                }
            }
        )
    }

    override fun getPackageName(): String {
        return "com.kelaode.kuaidgebq.orange"
    }

}