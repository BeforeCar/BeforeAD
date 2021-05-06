package com.beforecar.ad.policy

import android.app.Application
import android.view.View
import android.view.ViewGroup
import com.beforecar.ad.policy.base.IHookPolicy
import com.beforecar.ad.policy.base.getStackInfo
import com.beforecar.ad.policy.base.getVersionName
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

/**
 * @author: wangpan
 * @emial: p.wang@aftership.com
 * @date: 2021/5/6
 * 微信
 */
class WeiXinHookPolicy : IHookPolicy() {

    override val TAG: String = "tag_weixin"

    override fun getPackageName(): String {
        return "com.tencent.mm"
    }

    override fun onMainApplicationCreate(application: Application, classLoader: ClassLoader) {
        removePYQAdItems(application, classLoader)
    }

    /**
     * 移除朋友圈广告
     */
    private fun removePYQAdItems(application: Application, classLoader: ClassLoader) {
        try {
            log("removePYQAdItems start")
            XposedHelpers.findAndHookMethod(
                "com.tencent.mm.plugin.sns.ui.a.c", classLoader,
                "getView", Int::class.java, View::class.java, ViewGroup::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val position = param.args[0] as Int
                        val parent = param.args[2] as ViewGroup
                        if (isAdItems(application, param.thisObject, position)) {
                            param.result = View(parent.context).apply {
                                layoutParams = ViewGroup.LayoutParams(0, 0)
                            }
                            log("removePYQAdItems success: $position")
                        }
                    }
                }
            )
        } catch (t: Throwable) {
            log("removePYQAdItems fail: ${t.getStackInfo()}")
        }
    }

    private fun isAdItems(application: Application, adapter: Any, position: Int): Boolean {
        try {
            return when (application.getVersionName()) {
                "8.0.2" -> {
                    val snsInfo = XposedHelpers.callMethod(adapter, "adF", position)
                    XposedHelpers.getObjectField(snsInfo, "adsnsinfo") != null
                }
                else -> {
                    val snsInfo = XposedHelpers.callMethod(adapter, "aeP", position)
                    XposedHelpers.getObjectField(snsInfo, "adsnsinfo") != null
                }
            }
        } catch (t: Throwable) {
            log("isAdItems fail: ${t.getStackInfo()}")
        }
        return false
    }
}