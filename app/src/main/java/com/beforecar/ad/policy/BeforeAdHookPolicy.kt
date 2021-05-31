package com.beforecar.ad.policy

import android.app.Application
import android.content.Context
import android.content.Intent
import com.beforecar.ad.policy.base.AbsHookPolicy
import com.beforecar.ad.policy.base.getStackInfo
import com.beforecar.ad.utils.AppLogHelper
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

/**
 * @author: wangpan
 * @email: p.wang@aftership.com
 * @date: 2021/5/19
 */
class BeforeAdHookPolicy : AbsHookPolicy() {

    override val tag: String = "tag_before_ad"

    override fun getPackageName(): String {
        return "com.beforecar.ad"
    }

    override fun onMainApplicationBeforeCreate(application: Application, classLoader: ClassLoader) {
        hookAppLogReceiver(classLoader)
    }

    private fun hookAppLogReceiver(classLoader: ClassLoader) {
        try {
            XposedHelpers.findAndHookMethod(
                "com.beforecar.ad.AppLogReceiver", classLoader,
                "onReceive", Context::class.java, Intent::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val intent = param.args[1] as Intent
                        AppLogHelper.parseLogAndPrint(intent)
                    }
                }
            )
        } catch (t: Throwable) {
            log("hookAppLogReceiver fail: ${t.getStackInfo()}")
        }
    }

}