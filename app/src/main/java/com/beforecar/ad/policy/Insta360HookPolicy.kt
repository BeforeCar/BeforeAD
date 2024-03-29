package com.beforecar.ad.policy

import android.app.Application
import android.util.Log
import com.beforecar.ad.policy.base.AbsHookPolicy
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

class Insta360HookPolicy : AbsHookPolicy() {

    override val tag: String = "tag_insta360"

    override fun getPackageName(): String {
        return "com.arashivision.insta360akiko"
    }

    override fun onApplicationBeforeCreate(application: Application, classLoader: ClassLoader) {
        super.onApplicationBeforeCreate(application, classLoader)

        val destClass = XposedHelpers.findClass(
            "com.arashivision.insta360.frameworks.ui.view.progressbar.ProgressThumbProvider",
            classLoader
        )
        val videoParamsClass = XposedHelpers.findClass(
            "com.arashivision.insta360.basemedia.ui.player.video.IVideoParams",
            classLoader
        )
        val destMethodName = "getVideoParamsMD5Key"
        XposedHelpers.findAndHookMethod(
            destClass,
            destMethodName,
            videoParamsClass,
            object : XC_MethodHook() {
                var time = 0L
                var workName: String? = ""
                override fun beforeHookedMethod(param: MethodHookParam?) {
                    super.beforeHookedMethod(param)
                    log("param?.thisObject:" + param?.thisObject)
//                val workObject = XposedHelpers.getObjectField(param?.thisObject, "work")
//                workName = XposedHelpers.callMethod(workObject, "getName") as? String
//                    time = System.currentTimeMillis()
                }

                override fun afterHookedMethod(param: MethodHookParam?) {
                    super.afterHookedMethod(param)
//                log("initWorkEdit-workName:${workName}, time:${System.currentTimeMillis() - time}ms")
//                    log("time:${System.currentTimeMillis() - time}ms")
                    log(Log.getStackTraceString(RuntimeException("getVideoParamsMD5Key")))
                }
            })
        val secureClass = XposedHelpers.findClass(
            "android.provider.Settings.Secure",
            classLoader
        )
        val contentResolverClass = XposedHelpers.findClass(
            "android.content.ContentResolver",
            classLoader
        )
        val getStringMethodName = "getString"
        XposedHelpers.findAndHookMethod(
            secureClass,
            getStringMethodName,
            contentResolverClass,
            String::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam?) {
                    super.beforeHookedMethod(param)
                    log("param?.thisObject:" + param?.thisObject)
                }

                override fun afterHookedMethod(param: MethodHookParam?) {
                    super.afterHookedMethod(param)
                    if (param?.args?.getOrNull(1) == "android_id") {
                        log(Log.getStackTraceString(RuntimeException("getString")))
                    }
                }
            })

    }

}