package com.beforecar.ad.policy.base

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.annotation.CallSuper
import com.beforecar.ad.okhttp.OkHttpHelper
import com.beforecar.ad.utils.AppLogHelper
import com.beforecar.ad.utils.AppUtils.isAssignableFromKt
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.lang.reflect.Method

/**
 * Author: minminaya  承接东风各式弹头打磨、抛光、刷漆等4S保养工程。
 * Email: minminaya@gmail.com
 * Date:
 */
abstract class AbsHookPolicy {

    open val tag: String = "tag_hook"

    /**
     * 应用的主进程 application 实例
     */
    private var mainApplication: Application? = null

    /**
     * hook 应用的包名
     * @return String
     */
    abstract fun getPackageName(): String

    private var unHookByActivityForMainThread: XC_MethodHook.Unhook? = null
    private var unHookByActivity: XC_MethodHook.Unhook? = null

    @CallSuper
    open fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        callApplicationCreate(lpparam)
        callFirstActivityOnCreate(lpparam)
        callMainFirstActivityOnCreate(lpparam)
    }

    /**
     * 获取 application 的 onCreate 方法
     */
    private fun findApplicationOnCreateMethod(
        appClassName: String,
        classLoader: ClassLoader
    ): Method {
        var appClass = XposedHelpers.findClass(appClassName, classLoader)
        var onCreateMethod: Method? = XposedHelpers.findMethodExactIfExists(appClass, "onCreate")
        var superClass: Class<*>? = appClass.superclass
        while (onCreateMethod == null && Application::class.java.isAssignableFromKt(superClass)) {
            appClass = superClass
            onCreateMethod = XposedHelpers.findMethodExactIfExists(appClass, "onCreate")
            superClass = appClass.superclass
        }
        return onCreateMethod
            ?: throw NoSuchMethodException("onCreate method not found: $appClassName")
    }

    private fun callApplicationCreate(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val appClassName = lpparam.appInfo.className ?: Application::class.java.name
            val method = findApplicationOnCreateMethod(appClassName, lpparam.classLoader)
            XposedBridge.hookMethod(method, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val application = param.thisObject as Application
                    val classLoader = application.classLoader!!
                    onApplicationBeforeCreate(application, classLoader)
                    if (application.getProcessName() == getPackageName()) {
                        mainApplication = application
                        onMainApplicationBeforeCreate(application, classLoader)
                    } else {
                        onMinorApplicationBeforeCreate(application, classLoader)
                    }
                }

                override fun afterHookedMethod(param: MethodHookParam) {
                    val application = param.thisObject as Application
                    val classLoader = application.classLoader!!
                    onApplicationAfterCreate(application, classLoader)
                    if (application.getProcessName() == getPackageName()) {
                        onMainApplicationAfterCreate(application, classLoader)
                    } else {
                        onMinorApplicationAfterCreate(application, classLoader)
                    }
                }
            })
        } catch (t: Throwable) {
            log("callApplicationCreate fail: ${t.getStackInfo()}")
        }
    }

    /**
     * 应用 Application 的 onCreate 之前调用
     */
    open fun onApplicationBeforeCreate(application: Application, classLoader: ClassLoader) {

    }

    /**
     * 应用的主进程 Application 的 onCreate 之前调用
     */
    open fun onMainApplicationBeforeCreate(application: Application, classLoader: ClassLoader) {

    }

    /**
     * 应用 Application 的 onCreate 之后调用
     */
    open fun onApplicationAfterCreate(application: Application, classLoader: ClassLoader) {

    }

    /**
     * 应用的主进程 Application 的 onCreate 之后调用
     */
    open fun onMainApplicationAfterCreate(application: Application, classLoader: ClassLoader) {

    }

    /**
     * 应用子进程 Application 的 onCreate 之前调用
     */
    open fun onMinorApplicationBeforeCreate(application: Application, classLoader: ClassLoader) {

    }

    /**
     * 应用子进程 Application 的 onCreate 之后调用, 每个进程都会执行
     */
    open fun onMinorApplicationAfterCreate(application: Application, classLoader: ClassLoader) {

    }

    private fun callFirstActivityOnCreate(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            unHookByActivity = XposedHelpers.findAndHookMethod(
                Activity::class.java, "onCreate", Bundle::class.java, object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        (param.thisObject as? Activity)?.let { firstActivity ->
                            firstActivity.application?.let { firstActivityApplication ->
                                firstActivityApplication.classLoader?.let { applicationClassLoader ->
                                    unHookByActivity?.unhook()
                                    onFirstValidActivityPreOnCreate(
                                        lpparam,
                                        firstActivityApplication,
                                        firstActivity,
                                        applicationClassLoader
                                    )
                                }
                            }
                        }
                    }
                }
            )
        } catch (t: Throwable) {
            log("callApplicationCreate fail: ${getPackageName()}")
            log(t.getStackInfo())
        }
    }

    private fun callMainFirstActivityOnCreate(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.processName != getPackageName()) return
        try {
            unHookByActivityForMainThread = XposedHelpers.findAndHookMethod(
                Activity::class.java, "onCreate", Bundle::class.java, object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        (param.thisObject as? Activity)?.let { firstActivity ->
                            firstActivity.application?.let { firstActivityApplication ->
                                firstActivityApplication.classLoader?.let { applicationClassLoader ->
                                    unHookByActivityForMainThread?.unhook()
                                    if (firstActivityApplication.getProcessName() == getPackageName()) {
                                        onMainFirstValidActivityPreOnCreate(
                                            lpparam,
                                            firstActivityApplication,
                                            firstActivity,
                                            applicationClassLoader
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            )
        } catch (t: Throwable) {
            log("callApplicationCreate fail: ${getPackageName()}")
            log(t.getStackInfo())
        }
    }

    /**
     * 首个成功被 hook 的 Activity onCreate 的回调【任意线程】
     * 通常用于加固包或者某些延迟加载 dex 分包的 APP 的 hook
     *
     * note: 若要 hook application 的内容不要使用此方法
     *
     * @param lpparam LoadPackageParam 原始 xposed lpparam
     * @param application Application APP 的 application
     * @param activity Activity 首个成功被 hook 的 Activity
     * @param classLoader ClassLoader application 的 classloader 对象，注意这个可能和 LoadPackageParam.classLoader 不同
     * @return Unit
     */
    @CallSuper
    open fun onFirstValidActivityPreOnCreate(
        lpparam: XC_LoadPackage.LoadPackageParam,
        application: Application,
        activity: Activity,
        classLoader: ClassLoader
    ) {

    }

    /**
     * 首个成功被 hook 的 Activity onCreate 的回调【 APP 主线程】
     * 通常用于加固包或者某些延迟加载 dex 分包的 APP 的 hook
     *
     * note: 若要 hook application 的内容不要使用此方法
     *
     * @param lpparam LoadPackageParam 原始 xposed lpparam
     * @param application Application APP 的 application
     * @param activity Activity 首个成功被 hook 的 Activity
     * @param classLoader ClassLoader application 的 classloader 对象，注意这个可能和 LoadPackageParam.classLoader 不同
     * @return Unit
     */
    @CallSuper
    open fun onMainFirstValidActivityPreOnCreate(
        lpparam: XC_LoadPackage.LoadPackageParam,
        application: Application,
        activity: Activity,
        classLoader: ClassLoader
    ) {

    }

    /**
     * 是否使用广播发送 log
     */
    open fun isSendBroadcastLog(): Boolean {
        return false
    }

    fun log(content: Any?) {
        val context = mainApplication
        if (isSendBroadcastLog() && context != null) {
            AppLogHelper.sendLogBroadcast(context, tag, content)
        } else {
            XposedBridge.log("$tag: $content")
        }
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
    fun runOnUIThread(delayMillis: Long = 0, runnable: Runnable) {
        Handler(Looper.getMainLooper()).postDelayed(runnable, delayMillis)
    }

    fun showToast(msg: String, duration: Int = Toast.LENGTH_SHORT) {
        val context = mainApplication ?: return
        runOnUIThread {
            Toast.makeText(context, msg, duration).show()
        }
    }

    fun findParseResponseMethod(classLoader: ClassLoader): Method? {
        try {
            val okHttpCallCls = XposedHelpers.findClass(OkHttpCall, classLoader)
            val responseCls = XposedHelpers.findClass(Response, classLoader)
            for (method in okHttpCallCls.declaredMethods) {
                if (method.name == "parseResponse"
                    && method.returnType == responseCls
                ) {
                    return method
                }
            }
        } catch (t: Throwable) {
            log("findParseResponseMethod fail: ${t.getStackInfo()}")
        }
        return null
    }

    fun hookOkHttpCall(
        classLoader: ClassLoader,
        beforeHookedMethod: ((param: XC_MethodHook.MethodHookParam, url: String?) -> Unit)? = null,
        afterHookedMethod: ((param: XC_MethodHook.MethodHookParam, url: String?) -> Unit)? = null
    ) {
        try {
            val parseResponseMethod = findParseResponseMethod(classLoader)
            if (parseResponseMethod == null) {
                log("hookOkHttpCall cancel: parseResponseMethod is null")
                return
            }
            XposedBridge.hookMethod(parseResponseMethod, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    super.beforeHookedMethod(param)
                    beforeHookedMethod?.run {
                        val okHttpCall = param.thisObject as Any
                        val url = getUrlFromOkHttpCall(okHttpCall)
                        this.invoke(param, url)
                    }
                }

                override fun afterHookedMethod(param: MethodHookParam) {
                    super.afterHookedMethod(param)
                    afterHookedMethod?.run {
                        val okHttpCall = param.thisObject as Any
                        val url = getUrlFromOkHttpCall(okHttpCall)
                        this.invoke(param, url)
                    }
                }
            })
        } catch (t: Throwable) {
            log("hookOkHttpCall fail: ${t.getStackInfo()}")
        }
    }

    fun getUrlFromOkHttpCall(okHttpCall: Any): String {
        try {
            val request = XposedHelpers.callMethod(okHttpCall, "request")
            return OkHttpHelper.getUrlFromRequest(request)
        } catch (t: Throwable) {
            log("getUrlFromOkHttpCall fail: ${t.getStackInfo()}")
        }
        return ""
    }

    companion object {
        const val OkHttpCall = "retrofit2.OkHttpCall"
        const val Response = "retrofit2.Response"

    }

}