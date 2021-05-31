package com.beforecar.ad.policy

import android.app.Application
import com.beforecar.ad.policy.base.AbsHookPolicy
import com.beforecar.ad.policy.base.getStackInfo
import com.beforecar.ad.policy.jd.EvaluateCenterMainActivity
import com.beforecar.ad.policy.jd.ProductDetailActivity
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

/**
 * @author: wangpan
 * @email: p.wang@aftership.com
 * @date: 2021/5/11
 *
 * 京东
 */
object JDHookPolicy : AbsHookPolicy() {

    override val tag: String = "tag_jd"

    override fun getPackageName(): String {
        return "com.jingdong.app.mall"
    }

    override fun isSendBroadcastLog(): Boolean {
        return true
    }

    override fun onMainApplicationAfterCreate(application: Application, classLoader: ClassLoader) {
        //评论中心
        EvaluateCenterMainActivity.startHook(application, classLoader)
        //商品详情
        ProductDetailActivity.startHook(application, classLoader)
        //启动页广告
        removeSplashAd(classLoader)
        //屏蔽应用更新
        disableCheckUpgrade(classLoader)
    }

    /**
     * 屏蔽应用更新
     */
    private fun disableCheckUpgrade(classLoader: ClassLoader) {
        try {
            val updateCls = XposedHelpers.findClass("com.jingdong.app.mall.update.UpdateInitialization", classLoader)
            val activityCls = XposedHelpers.findClass("com.jingdong.common.frame.IMyActivity", classLoader)
            XposedHelpers.findAndHookMethod(updateCls, "checkVersion", activityCls, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    param.result = null
                    log("disableCheckUpgrade success")
                }
            })
        } catch (t: Throwable) {
            log("disableCheckUpgrade fail: ${t.getStackInfo()}")
        }
    }

    private fun removeSplashAd(classLoader: ClassLoader) {
        try {
            val baseFrameUtilCls = XposedHelpers.findClass("com.jingdong.common.BaseFrameUtil", classLoader)
            XposedHelpers.setStaticBooleanField(baseFrameUtilCls, "needStartImage", false)
            log("removeSplashAd success")
        } catch (t: Throwable) {
            log("removeSplashAd fail: ${t.getStackInfo()}")
        }
    }

}