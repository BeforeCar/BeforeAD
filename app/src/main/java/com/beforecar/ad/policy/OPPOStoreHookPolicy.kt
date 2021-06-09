package com.beforecar.ad.policy

import android.app.Application
import com.beforecar.ad.policy.base.AbsHookPolicy
import com.beforecar.ad.policy.base.getStackInfo
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

/**
 * @author: wangpan
 * @email: p.wang@aftership.com
 * @date: 2021/6/9
 *
 * 欢太商城
 */
class OPPOStoreHookPolicy : AbsHookPolicy() {

    companion object {

        const val ProtoReader = "com.squareup.wire.ProtoReader"

        /**
         * 启动页 Banners 类解析器
         */
        const val ProtoAdapter_Banners = "com.oppo.store.protobuf.Banners\$ProtoAdapter_Banners"

    }

    override val tag: String = "tag_ostore"

    override fun getPackageName(): String {
        return "com.oppo.store"
    }

    override fun onMainApplicationBeforeCreate(application: Application, classLoader: ClassLoader) {
        //移除启动页广告
        removeSplashAd(classLoader)
    }

    /**
     * 移除启动页广告
     */
    private fun removeSplashAd(classLoader: ClassLoader) {
        try {
            val protoAdapterCls = XposedHelpers.findClass(ProtoAdapter_Banners, classLoader)
            val protoReaderCls = XposedHelpers.findClass(ProtoReader, classLoader)
            XposedHelpers.findAndHookMethod(protoAdapterCls, "decode", protoReaderCls, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    param.result = null
                    log("removeSplashAd success")
                }
            })
        } catch (t: Throwable) {
            log("removeSplashAd fail: ${t.getStackInfo()}")
        }
    }

}