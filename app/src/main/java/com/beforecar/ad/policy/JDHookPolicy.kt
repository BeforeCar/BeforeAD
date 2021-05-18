package com.beforecar.ad.policy

import android.app.Application
import com.beforecar.ad.policy.base.AbsHookPolicy
import com.beforecar.ad.policy.base.getStackInfo
import com.beforecar.ad.policy.jd.EvaluateCenterMainActivity
import com.beforecar.ad.policy.jd.ProductDetailActivity
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

    override fun getMainApplicationName(): String {
        return "com.jd.chappie.loader.ChappieApplication"
    }

    override fun isSendBroadcastLog(): Boolean {
        return true
    }

    override fun onMainApplicationCreate(application: Application, classLoader: ClassLoader) {
        //评论中心
        EvaluateCenterMainActivity.startHook(application, classLoader)
        //商品详情
        ProductDetailActivity.startHook(application, classLoader)
        //启动页广告
        removeSplashAd(classLoader)
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