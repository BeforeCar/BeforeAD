package com.beforecar.ad.policy

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.beforecar.ad.policy.base.AbsHookPolicy
import com.beforecar.ad.policy.base.getProcessName
import com.beforecar.ad.policy.base.getStackInfo
import com.beforecar.ad.policy.jd.EvaluateCenterMainActivity
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * @author: wangpan
 * @email: p.wang@aftership.com
 * @date: 2021/5/11
 * 京东
 */
class JDHookPolicy : AbsHookPolicy() {

    private var logFile: File? = null

    private val evaluateCenterMainActivity = EvaluateCenterMainActivity(this)

    private val timeFormat: SimpleDateFormat by lazy {
        SimpleDateFormat("MM-dd HH:mm:ss:SSS", Locale.getDefault())
    }

    override fun getPackageName(): String {
        return "com.jingdong.app.mall"
    }

    @SuppressLint("SdCardPath")
    private fun createLogFile(force: Boolean): File? {
        try {
            val logFile = File("/data/data/${getPackageName()}/files/", "log_file.txt")
            if (!logFile.exists()) {
                logFile.createNewFile()
                return logFile
            }
            if (force) {
                logFile.delete()
                logFile.createNewFile()
            }
            return logFile
        } catch (t: Throwable) {
            //no op
        }
        return null
    }

    override fun log(content: Any?) {
        logFile?.appendText("${timeFormat.format(System.currentTimeMillis())}: $content\n")
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        super.handleLoadPackage(lpparam)
        if (lpparam.processName == lpparam.packageName) {
            //创建日志输出文件
            logFile = createLogFile(true)
            callOnMainAppStart(lpparam)
        }
    }

    private fun callOnMainAppStart(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            XposedHelpers.findAndHookMethod(
                "com.jd.chappie.loader.ChappieApplication", lpparam.classLoader,
                "onCreate", object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val application = param.thisObject as Application
                        val classLoader = application.classLoader!!
                        if (application.getProcessName() == getPackageName()) {
                            log("onMainAppStart success")
                            onMainAppStart(application, classLoader)
                        }
                    }
                }
            )
        } catch (t: Throwable) {
            log("callOnMainAppStart fail: ${t.getStackInfo()}")
        }
    }

    /**
     * 主进程 application 已创建
     */
    private fun onMainAppStart(application: Application, classLoader: ClassLoader) {
        //监听评论中心模块加载
        callOnEvaluateCenterLoaded(classLoader)
    }

    /**
     * 监听评论中心模块加载
     */
    private fun callOnEvaluateCenterLoaded(classLoader: ClassLoader) {
        try {
            val helperCls = XposedHelpers.findClass("com.jingdong.common.utils.AuraPreLoadBundleHelper", classLoader)
            XposedHelpers.findAndHookMethod(helperCls, "preLoadClass", Runnable::class.java, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val runnable = param.args[0] as Runnable
                    if (runnable.javaClass.name == "com.jd.lib.evaluatecenter.a") {
                        log("callOnEvaluateCenterLoaded success")
                        onEvaluateCenterLoaded(runnable.javaClass.classLoader!!)
                    }
                }
            })
        } catch (t: Throwable) {
            log("callOnEvaluateCenterLoaded fail: ${t.getStackInfo()}")
        }
    }

    /**
     * 评论中心模块已加载
     */
    private fun onEvaluateCenterLoaded(classLoader: ClassLoader) {
        hookEvaluateCenterMainActivity(classLoader)
    }

    /**
     * hook EvaluateCenterMainActivity
     */
    private fun hookEvaluateCenterMainActivity(classLoader: ClassLoader) {
        try {
            val activityCls = XposedHelpers.findClass(
                "com.jd.lib.evaluatecenter.view.activity.EvaluateCenterMainActivity", classLoader
            )
            XposedHelpers.findAndHookMethod(activityCls, "onCreate", Bundle::class.java, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val activity = param.thisObject as Activity
                    evaluateCenterMainActivity.onCreate(activity)
                }
            })
            XposedHelpers.findAndHookMethod(activityCls, "onDestroy", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    evaluateCenterMainActivity.onDestroy()
                }
            })
        } catch (t: Throwable) {
            log("hookEvaluateCenterMainActivity fail: ${t.getStackInfo()}")
        }
    }

}