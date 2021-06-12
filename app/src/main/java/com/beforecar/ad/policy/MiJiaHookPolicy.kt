package com.beforecar.ad.policy

import android.app.Application
import com.beforecar.ad.okhttp.OkHttpHelper
import com.beforecar.ad.policy.base.AbsHookPolicy
import com.beforecar.ad.policy.base.getStackInfo
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import java.io.IOException

/**
 * @author: wangpan
 * @email: p.wang@aftership.com
 * @date: 2021/5/6
 *
 * 米家
 */
class MiJiaHookPolicy : AbsHookPolicy() {

    override val tag: String = "tag_mijia"

    private var okHttpHelper: OkHttpHelper? = null

    override fun getPackageName(): String {
        return "com.xiaomi.smarthome"
    }

    override fun onMainApplicationBeforeCreate(application: Application, classLoader: ClassLoader) {
        //每次启动重置
        okHttpHelper = null
        //hook RealCall
        hookRealCall(classLoader)
    }

    private fun getOkHttpHelper(): OkHttpHelper {
        return okHttpHelper ?: kotlin.run {
            OkHttpHelper.create(
                realCall = "okhttp3.RealCall",
                getResponseWithInterceptorChain = "getResponseWithInterceptorChain",
                getRequest = "request"
            ).also {
                okHttpHelper = it
            }
        }
    }

    /**
     * hook RealCall
     */
    private fun hookRealCall(classLoader: ClassLoader) {
        try {
            XposedBridge.hookMethod(
                getOkHttpHelper().getResponseWithInterceptorChainMethod(classLoader),
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val realCall = param.thisObject as Any
                        val url = getOkHttpHelper().getUrl(realCall)
                        when {
                            //首页 banner 广告
                            url.contains("/recommendation/banner") -> {
                                param.throwable = IOException("disable banner ad")
                                log("removeBannerAd success")
                            }
                            //我的页面推荐广告
                            url.contains("/recommendation/myTab") -> {
                                param.throwable = IOException("disable myTab ad")
                                log("removeMyTabAd success")
                            }
                        }
                    }
                }
            )
        } catch (t: Throwable) {
            log("hookRealCall fail: ${t.getStackInfo()}")
        }
    }

}