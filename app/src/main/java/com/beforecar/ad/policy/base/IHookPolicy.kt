package com.beforecar.ad.policy.base

import android.app.Application
import androidx.annotation.CallSuper
import com.beforecar.ad.utils.AppUtils
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * Author: minminaya  承接东风各式弹头打磨、抛光、刷漆等4S保养工程。
 * Email: minminaya@gmail.com
 * Date:
 */
abstract class IHookPolicy {

    open val TAG: String = "tag_hook"

    abstract fun getPackageName(): String

    @CallSuper
    open fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        callApplicationCreate(lpparam)
    }

    private fun callApplicationCreate(lpparam: XC_LoadPackage.LoadPackageParam) {
        val packageName = lpparam.packageName
        try {
            XposedHelpers.findAndHookMethod(
                Application::class.java, "onCreate", object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val application = param.thisObject as Application
                        val classLoader = application.classLoader!!
                        onApplicationCreate(application, classLoader)
                        if (AppUtils.getProcessName(application) == packageName) {
                            log("onMainApplicationCreate: $packageName")
                            onMainApplicationCreate(application, classLoader)
                        }
                    }
                }
            )
        } catch (t: Throwable) {
            log("callApplicationCreate fail: $packageName")
            log(t.getStackInfo())
        }
    }

    open fun onApplicationCreate(application: Application, classLoader: ClassLoader) {

    }

    open fun onMainApplicationCreate(application: Application, classLoader: ClassLoader) {

    }

    fun log(content: Any?) {
        XposedBridge.log("$TAG: $content")
    }

    fun Throwable.getStackInfo(): String {
        val sb = StringBuilder(this.toString())
        for (s in this.stackTrace) {
            sb.append("\n").append(s.toString())
        }
        return sb.toString()
    }
}