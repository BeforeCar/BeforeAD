package com.beforecar.ad.policy

import android.app.Application
import android.content.Context
import com.beforecar.ad.okhttp.OkHttpHelper
import com.beforecar.ad.policy.base.AbsHookPolicy
import com.beforecar.ad.policy.base.getStackInfo
import com.beforecar.ad.utils.FileUtils
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import java.io.File
import java.io.IOException

/**
 * @author: wangpan
 * @email: p.wang@aftership.com
 * @date: 2021/6/18
 *
 * 闲鱼
 */
class IdleFishHookPolicy : AbsHookPolicy() {

    override val tag: String = "tag_idlefinish"

    private var okHttpHelper: OkHttpHelper? = null

    override fun getPackageName(): String {
        return "com.taobao.idlefish"
    }

    override fun onMainApplicationBeforeCreate(application: Application, classLoader: ClassLoader) {
        //每次启动重置
        okHttpHelper = null
        //hook RealCall
        hookRealCall(classLoader)
        //删除启动页广告文件夹
        deleteSplashFiles(application)
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
                            //启动页广告
                            url.contains("/start/rt")
                                    or url.contains("/start/pre")
                                    or url.contains("/adv/startpage") -> {
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

    /**
     * 删除启动页广告文件夹
     */
    private fun deleteSplashFiles(context: Context) {
        try {
            val parentPath1 = context.getExternalFilesDir(null)
            val parentPath2 = context.externalCacheDir
            FileUtils.delete(File(parentPath1, "ad/splash"))
            FileUtils.delete(File(parentPath1, "ad/splash_ad_resp"))
            FileUtils.delete(File(parentPath2, "ad/splash"))
            FileUtils.delete(File(parentPath2, "ad/splash_ad_resp"))
            log("deleteSplashFiles success")
        } catch (t: Throwable) {
            log("deleteSplashFiles fail: ${t.getStackInfo()}")
        }
    }
}