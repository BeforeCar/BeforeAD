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
 * @date: 2021/6/9
 *
 * 欢太商城
 */
class OPPOStoreHookPolicy : AbsHookPolicy() {

    override val tag: String = "tag_ostore"

    private var okHttpHelper: OkHttpHelper? = null

    override fun getPackageName(): String {
        return "com.oppo.store"
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
                getResponseWithInterceptorChain = "e",
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
                            //启动页广告
                            url.contains("/configs/v1/screens/010001") -> {
                                param.throwable = IOException("disable splash ad")
                                log("removeSplashAd success")
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