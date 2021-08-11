package com.beforecar.ad.policy

import android.app.Application
import android.content.Context
import com.beforecar.ad.policy.base.AbsHookPolicy
import com.beforecar.ad.policy.base.getStackInfo
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import org.json.JSONException

/**
 * @author: wangpan
 * @email: p.wang@aftership.com
 * @date: 2021/8/11
 *
 * MIUI系统更新
 */
class MiUpdaterHookPolicy : AbsHookPolicy() {

    override val tag: String = "tag_mi_updater"

    override fun getPackageName(): String {
        return "com.android.updater"
    }

    override fun onApplicationAfterCreate(application: Application, classLoader: ClassLoader) {
        disableUpdate(classLoader)
    }

    /**
     * 禁用系统更新
     */
    private fun disableUpdate(classLoader: ClassLoader) {
        try {
            XposedHelpers.findAndHookConstructor(
                UpdateInfo, classLoader,
                String::class.java, Context::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        param.throwable = JSONException("disable update")
                        log("disableUpdate success")
                    }
                }
            )
        } catch (t: Throwable) {
            log("hookUpdater fail: ${t.getStackInfo()}")
        }
    }

    companion object {

        const val UpdateInfo = "com.android.updater.UpdateInfo"

    }

}