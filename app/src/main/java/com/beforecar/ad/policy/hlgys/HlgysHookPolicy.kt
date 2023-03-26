package com.beforecar.ad.policy.hlgys

import android.app.Activity
import android.app.Application
import com.beforecar.ad.policy.base.AbsHookPolicy
import de.robv.android.xposed.XC_MethodHook
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

        //强制去掉广告，将 brand 设置为 TEST
        val buildClazz = XposedHelpers.findClass("android.os.Build", classLoader)
        XposedHelpers.setStaticObjectField(buildClazz, "BRAND", "TEST")


//        //下载强制打开
//        XposedHelpers.findAndHookMethod(
//            "com.junyue.repository.config.ConfigBean",
//            classLoader,
//            "L",
//            object : XC_MethodHook() {
//                override fun beforeHookedMethod(param: MethodHookParam) {
//                    param.result = false
////                    printStackInfo()
//                }
//
//            }
//        )

        val configBeanClazz =
            XposedHelpers.findClass("com.junyue.repository.config.ConfigBean", classLoader)

        val hookMethods = mutableMapOf<Int, XC_MethodHook.Unhook?>()
        configBeanClazz.declaredMethods.forEach {
            log(it.name + " ")
            XposedHelpers.findAndHookMethod(configBeanClazz, it.name, *it.parameterTypes,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam?) {
                        super.afterHookedMethod(param)
                        param?.thisObject?.let { configBean ->
                            val hasHooked =
                                XposedHelpers.getAdditionalStaticField(configBean, "hasHooked")
                            if (hasHooked == true) {
                                return@let
                            }
                            XposedHelpers.setBooleanField(configBean, "downloadEnable", true)
                            XposedHelpers.setBooleanField(configBean, "screencastSwitch", true)
                            XposedHelpers.setBooleanField(configBean, "p2pSwitch", false)
                            XposedHelpers.setIntField(configBean, "p2pNewSwitchAndroid", 0)

                            log("=====ConfigBean=========:${configBean}")
                            XposedHelpers.setAdditionalStaticField(configBean, "hasHooked", true)
                            hookMethods.values.forEach { hook ->
                                hook?.unhook()
                            }
                        }
                    }
                }).apply {
                hookMethods[it.hashCode()] = this
            }
        }

        hookOkHttpCall(classLoader, beforeHookedMethod = { param, url ->
            when {
                //检测更新
                url?.contains("checkupdate") == true -> {
                    param.throwable = IOException("disable check update")
                    log("disableCheckUpdate success")
                }
            }
        }, afterHookedMethod = { param, url ->
            when {
                url?.contains("config?platform") == true -> {
                    hookHLGYSConfigAPI(url, classLoader, param)
                }
            }
        })

        //强制去掉 no_proxy
//        XposedHelpers.findAndHookMethod(
//            "d.g.d.c.c",
//            classLoader,
//            "c",
//            object : XC_MethodHook() {
//                override fun beforeHookedMethod(param: MethodHookParam) {
//                    param.result = true
//                }
//
//            }
//        )

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
//        XposedHelpers.findAndHookMethod(
//            "c.a.b.h.a",
//            classLoader,
//            "b",
//            object : XC_MethodHook() {
//                override fun beforeHookedMethod(param: MethodHookParam?) {
//                    val builderCls = XposedHelpers.findClass("okhttp3.OkHttpClient.b", classLoader)
//                    val newBuilder = builderCls.getDeclaredConstructor().also {
//                        it.isAccessible = true
//                    }.newInstance()
//                    param?.result = newBuilder
//                }
//            }
//        )

//        XposedHelpers.findAndHookMethod(
//            "com.dueeeke.videoplayer.player.VideoView",
//            classLoader,
//            "setUrl",
//            String::class.java,
//            Map::class.java,
//            object : XC_MethodHook() {
//                override fun beforeHookedMethod(param: MethodHookParam?) {
////                    super.beforeHookedMethod(param)
//                    log("setUrl:${param?.args?.get(0)}")
//                    printStackInfo()
//                }
//            }
//        )
//        XposedHelpers.findAndHookMethod(
//            "cn.fxlcy.anative.Native",
//            classLoader,
//            "a",
//            String::class.java,
//            object : XC_MethodHook() {
//                override fun beforeHookedMethod(param: MethodHookParam?) {
//                    super.beforeHookedMethod(param)
//                    log("加密内容:${param?.args?.get(0)}")
//                }
//            }
//        )

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

//        findHttpServerLogMethod(classLoader).forEach {
//            XposedBridge.hookMethod(it, object : XC_MethodHook() {
//                override fun beforeHookedMethod(param: MethodHookParam?) {
////                    super.beforeHookedMethod(param)
//                    log("HttpServerLog-${it.name}:${param?.args?.get(0)}")
//                }
//            })
//        }


    }

    private fun hookHLGYSConfigAPI(
        url: String?,
        classLoader: ClassLoader,
        param: XC_MethodHook.MethodHookParam
    ) {
        log("url=====${url}")
        val newString =
            "{\"code\":200,\"data\":{\"iosTrailer\":false,\"androidTrailer\":false,\"videoParserUrl\":\"Ov4AlG2Sn3vfzH2mxBKpPsgJvuAjdc2c0xmGSBJ5hoqbMJt3eg4PFxBwfvJZfzrKGCGzgBuE8Sklwdvq+m6mAmRfcz4jy56pZCbo9mCYNBpLfEmPAMQ4bT9uXNKuafaChAt7RClqXwrj9++KyV1wOsPJWHUXQWe6nQV9TwTZe/fvoZnIfCMWuPM/ee7UyMa2ZEIbOjljuTxnCRLa5ba/nTAw2EnK8pCOX/LuYtOvJxX+IiBtP9VMbmq7+dfqdw8Ttqd+wQz/2iTrB5zfL5Mzg2G7wsM59pEwA/r5JRsA1k8zDOPczm0nAvp+V2cQ9B4Ouk1YSrcG+yEQVQzuSI4yLoJ7LEyQ==\",\"promotionDesc\":[],\"p2pSwitch\":false,\"p2pSwitchIos\":false,\"p2pNewSwitchAndroid\":\"\",\"p2pNewSwitchIos\":\"\",\"iosShareSwitch\":true,\"configAdvertise\":{\"ios\":null,\"android\":null,\"adSwitch\":false,\"installAfterAd\":0,\"installAfterAdNew\":0,\"installAfterAdOrigin\":0,\"infoAdvertisingDuration\":90,\"readFullAdInterval\":0,\"backgroundTailTime\":300,\"unlockedVideoTime\":30000000,\"homePageInterstitialTime\":5,\"detailPageJumpTime\":0},\"thirdParty\":{\"androidWeChatShareAppId\":\"101968124\",\"androidQQShareAppId\":\"101968124\"},\"replaceText\":[],\"scoreList\":[],\"signalingUrl\":\"\",\"screencastSwitch\":true,\"downloadEnable\":true},\"msg\":\"获取成功\"}"
        val gsonClazz =
            XposedHelpers.findClass("com.junyue.basic.util.z\$a", classLoader)
        val gson =
            XposedHelpers.getStaticObjectField(gsonClazz, "a")
        val configBean =
            XposedHelpers.findClass(
                "com.junyue.repository.config.ConfigBean",
                classLoader
            )
        val baseResponse =
            XposedHelpers.findClass("com.junyue.basic.bean.BaseResponse", classLoader)
        val parameterizedType: Utils.ParameterizedTypeImpl =
            Utils.ParameterizedTypeImpl(
                null,
                baseResponse, configBean
            )
        log("parameterizedType========${parameterizedType}")
        val newBaseResponse =
            XposedHelpers.callMethod(gson, "fromJson", newString, parameterizedType)
        param.result = newBaseResponse
        log("hook config success")
    }

    private fun findHttpServerLogMethod(classLoader: ClassLoader): List<Method> {
        val interceptorCls = XposedHelpers.findClass("c.a.b.o.t", classLoader)
        return interceptorCls.declaredMethods.asList()
    }

    override fun getPackageName(): String {
        return "com.hlgys.hlg"
    }

}