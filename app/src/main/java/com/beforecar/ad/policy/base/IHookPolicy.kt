package com.beforecar.ad.policy.base

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.annotation.CallSuper
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
        callMainApplicationCreate(lpparam)
        callApplicationCreate(lpparam)
    }

    private fun callMainApplicationCreate(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.processName != getPackageName()) return
        try {
            XposedHelpers.findAndHookMethod(Application::class.java, "onCreate", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val application = param.thisObject as Application
                    val classLoader = application.classLoader!!
                    if (application.getProcessName() == getPackageName()) {
                        //log("onMainApplicationCreate: ${getPackageName()}")
                        onMainApplicationCreate(application, classLoader)
                    }
                }
            })
        } catch (t: Throwable) {
            log("callMainApplicationCreate fail: ${getPackageName()}")
            log(t.getStackInfo())
        }
    }

    private fun callApplicationCreate(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            XposedHelpers.findAndHookMethod(Application::class.java, "onCreate", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val application = param.thisObject as Application
                    val classLoader = application.classLoader!!
                    //log("callApplicationCreate: ${getPackageName()}")
                    onApplicationCreate(application, classLoader)
                }
            })
        } catch (t: Throwable) {
            log("callApplicationCreate fail: ${getPackageName()}")
            log(t.getStackInfo())
        }
    }

    /**
     * 应用的主进程 Application 的 onCreate 调用
     */
    open fun onMainApplicationCreate(application: Application, classLoader: ClassLoader) {

    }

    /**
     * 应用的每个进程 Application 的 onCreate 调用
     */
    open fun onApplicationCreate(application: Application, classLoader: ClassLoader) {

    }

    fun log(content: Any?) {
        XposedBridge.log("$TAG: $content")
    }

    /**
     * 打印当前堆栈信息
     */
    fun printStackInfo() {
        try {
            throw Exception()
        } catch (e: Exception) {
            log(e.getStackInfo())
        }
    }

    /**
     * 获取当前进程的 Looper.getMainLooper() 并执行任务
     * 虽然每次都创建 Handler 实例, 但是会比较安全
     * todo 注意这里可能会有问题 wangpan
     */
    fun runOnUIThread(runnable: Runnable) {
        Handler(Looper.getMainLooper()).post(runnable)
    }

    fun showToast(context: Context, msg: String, duration: Int = Toast.LENGTH_SHORT) {
        runOnUIThread {
            Toast.makeText(context, msg, duration).show()
        }
    }

}