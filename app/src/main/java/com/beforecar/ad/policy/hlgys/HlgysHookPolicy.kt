package com.beforecar.ad.policy.hlgys

import android.app.Activity
import android.app.Application
import com.beforecar.ad.policy.base.AbsHookPolicy
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.io.IOException
import java.lang.reflect.Method


class HlgysHookPolicy : AbsHookPolicy() {

    override val tag: String
        get() = "HlgysHookPolicy"

    override fun onFirstValidActivityPreOnCreate(
        lpparam: XC_LoadPackage.LoadPackageParam,
        application: Application,
        activity: Activity,
        classLoader: ClassLoader
    ) {
        super.onFirstValidActivityPreOnCreate(lpparam, application, activity, classLoader)
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
//        XposedHelpers.findAndHookMethod(
//            "d.g.d.b.d.a",
//            classLoader,
//            "u2",
//            String::class.java,
//            object : XC_MethodHook() {
//                override fun beforeHookedMethod(param: MethodHookParam) {
//                    log("host-url:${param.args[0]}")
//                }
//            }
//        )
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

        //打开 HttpConnection 的抓包开关
        XposedHelpers.findAndHookMethod(
            "java.net.URL",
            classLoader,
            "openConnection",
            XposedHelpers.findClass("java.net.Proxy", classLoader),
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam?) {
                    param?.result = XposedHelpers.callMethod(param?.thisObject, "openConnection")
                }
            }
        )

        //hook simpleClient 不要设置为 No_proxy，将 Builder 直接初始化返回
        XposedHelpers.findAndHookMethod(
            "c.a.b.h.a",
            classLoader,
            "b",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam?) {
                    val builderCls = XposedHelpers.findClass("okhttp3.OkHttpClient.b", classLoader)
                    val newBuilder = builderCls.getDeclaredConstructor().also {
                        it.isAccessible = true
                    }.newInstance()
                    param?.result = newBuilder
                }
            }
        )

        XposedHelpers.findAndHookMethod(
            "com.dueeeke.videoplayer.player.VideoView",
            classLoader,
            "setUrl",
            String::class.java,
            Map::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam?) {
//                    super.beforeHookedMethod(param)
                    log("setUrl:${param?.args?.get(0)}")
                    printStackInfo()
                }
            }
        )
        XposedHelpers.findAndHookMethod(
            "cn.fxlcy.anative.Native",
            classLoader,
            "a",
            String::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam?) {
                    super.beforeHookedMethod(param)
                    log("加密内容:${param?.args?.get(0)}")
                }
            }
        )

        //将弹窗设置为可以关闭
        XposedHelpers.findAndHookMethod(
            "android.app.Dialog",
            classLoader,
            "setCancelable",
            Boolean::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam?) {
                    param?.args?.set(0, true)
                    super.beforeHookedMethod(param)
                }
            }
        )

        //将弹窗设置为可以关闭
        XposedHelpers.findAndHookMethod(
            "android.app.Dialog",
            classLoader,
            "setCanceledOnTouchOutside",
            Boolean::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam?) {
                    param?.args?.set(0, true)
                    super.beforeHookedMethod(param)
                }
            }
        )

        //去除检测更新
        hookOkHttpCall(classLoader) { param, url ->
            when {
                //检测更新
                url?.contains("checkupdate") == true -> {
                    param.throwable = IOException("disable check update")
                    log("disableCheckUpdate success")
                }
            }
        }

//        XposedHelpers.findAndHookMethod(
//            "cn.fxlcy.anative.Native",
//            classLoader,
//            "c",
//            object : XC_MethodHook() {
//                override fun afterHookedMethod(param: MethodHookParam?) {
//                    super.afterHookedMethod(param)
//                    log("cn.fxlcy.anative.Native-c:${param?.result}")
//                }
//            }
//        )
//
//        XposedHelpers.findAndHookMethod(
//            "cn.fxlcy.anative.Native",
//            classLoader,
//            "b",
//            object : XC_MethodHook() {
//                override fun afterHookedMethod(param: MethodHookParam?) {
//                    super.afterHookedMethod(param)
//                    log("cn.fxlcy.anative.Native-b:${param?.result}")
//                }
//            }
//        )


//        XposedHelpers.findAndHookMethod(
//            "android.util.Base64",
//            classLoader,
//            "encodeToString",
//            ByteArray::class.java,
//            Int::class.java,
//            object : XC_MethodHook() {
//                override fun afterHookedMethod(param: MethodHookParam?) {
//                    super.afterHookedMethod(param)
//                    log("base64-encodeToString-:${param?.result}")
//                    printStackInfo()
//                }
//            }
//        )

        findHttpServerLogMethod(classLoader).forEach {
            XposedBridge.hookMethod(it, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam?) {
//                    super.beforeHookedMethod(param)
                    log("HttpServerLog-${it.name}:${param?.args?.get(0)}")
                }
            })
        }


    }

    private fun findHttpServerLogMethod(classLoader: ClassLoader): List<Method> {
        val interceptorCls = XposedHelpers.findClass("c.a.b.o.t", classLoader)
        return interceptorCls.declaredMethods.asList()
    }

    override fun getPackageName(): String {
        return "com.hlgys.hlg"
    }

}