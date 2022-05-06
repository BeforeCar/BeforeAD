package com.beforecar.ad.policy

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.beforecar.ad.policy.base.AbsHookPolicy
import com.beforecar.ad.policy.base.getStackInfo
import com.beforecar.ad.utils.OkHttp
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class DrmfabHookPolicy : AbsHookPolicy() {

    override val tag: String
        get() = "DrmfabHookPolicy"

    override fun onFirstValidActivityPreOnCreate(
        lpparam: XC_LoadPackage.LoadPackageParam,
        application: Application,
        activity: Activity,
        classLoader: ClassLoader
    ) {
        super.onFirstValidActivityPreOnCreate(lpparam, application, activity, classLoader)
        XposedHelpers.findAndHookMethod(
            Activity::class.java, "onCreate", Bundle::class.java, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (param.thisObject.javaClass.simpleName.contains("MainActivity")) {
                        (param.thisObject as? Activity)?.let { firstActivity ->
                            firstActivity.application?.let { firstActivityApplication ->
                                firstActivityApplication.classLoader?.let { applicationClassLoader ->
//                                    apply1(applicationClassLoader)
                                    hookBridgeInterceptor(applicationClassLoader)
                                }
                            }
                        }
                    }
                }
            }
        )
    }

    private fun hookBridgeInterceptor(classLoader: ClassLoader) {
        try {
            log("hookBridgeInterceptor start")
            val interceptorCls =
                XposedHelpers.findClass("okhttp3.internal.http.BridgeInterceptor", classLoader)
            val chainCls = XposedHelpers.findClass("okhttp3.Interceptor\$Chain", classLoader)
            XposedHelpers.findAndHookMethod(
                interceptorCls,
                "intercept",
                chainCls,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val chain = param.args[0] as Any
                        val url = OkHttp.getUrlFromChain(chain)
                        val response = param.result ?: return
                        when {
                            url.contains("index.php") -> {
                                log("index.php")
                                val newString =
                                    "{\"code\":\"200\",\"result\":\"成功播放\",\"play_key\":\"a3944e3ac807b99c57723c802b6432d9\",\"group_id\":\"xx01\",\"stu_validity\":\"2050-11-30\",\"watermark\":\"0\",\"watermarkInfo\":{}}"
                                val newResponse = OkHttp.createNewResponse(response, newString)
                                if (newResponse != null) {
                                    param.result = newResponse
                                    log("removeFeedListAdItems api success")
                                }
                            }
                        }
                    }
                })
        } catch (t: Throwable) {
            log("hookBridgeInterceptor fail: ${t.getStackInfo()}")
        }
    }


    override fun getPackageName(): String {
        return "com.drmfab.sks.phoneplayer.debug"
    }

}