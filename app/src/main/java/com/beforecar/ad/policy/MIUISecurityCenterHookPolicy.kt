package com.beforecar.ad.policy

import android.app.Activity
import android.app.Application
import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.widget.Button
import android.widget.CheckBox
import com.beforecar.ad.policy.base.AbsHookPolicy
import com.beforecar.ad.policy.base.getStackInfo
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import java.lang.reflect.Field
import java.lang.reflect.Method

/**
 * @author: wangpan
 * @email: p.wang@aftership.com
 * @date: 2021/09/28
 *
 * MIUI 手机管家
 */
object MIUISecurityCenterHookPolicy : AbsHookPolicy() {

    override val tag: String = "tag_security_center"

    /**
     * 拦截特殊权限界面
     */
    private const val SpecialPermissionInterceptActivity =
        "com.miui.permcenter.privacymanager.SpecialPermissionInterceptActivity"

    /**
     * adb 安装 app 界面
     */
    private const val AdbInstallActivity = "com.miui.permcenter.install.AdbInstallActivity"

    /**
     * USB 调试警告页面
     */
    private const val AdbInputApplyActivity = "com.miui.permcenter.install.AdbInputApplyActivity"

    private var getSupportFragmentManagerMethod: Method? = null
    private var findFragmentByIdMethod: Method? = null
    private var countDownHandler: Handler? = null
    private var setCountDownMethod: Method? = null

    override fun getPackageName(): String {
        return "com.miui.securitycenter"
    }

    override fun onMainApplicationAfterCreate(application: Application, classLoader: ClassLoader) {
        //解除限制: adb install apk 弹窗
        hookAdbInstall(classLoader)
        //接触限制: adb 调试
        hookAdbInput(classLoader)
        //解除限制: 特殊权限拦截
        hookSpecialPermissionIntercept(classLoader)
    }

    /**
     * 解除限制: adb install apk 弹窗
     */
    private fun hookAdbInstall(classLoader: ClassLoader) {
        try {
            val activityClass = XposedHelpers.findClassIfExists(
                AdbInstallActivity, classLoader
            )
            if (activityClass == null) {
                log("hookAdbInstall cancel: $AdbInstallActivity not found")
                return
            }
            val onCreateMethod = XposedHelpers.findMethodBestMatch(
                activityClass, "onCreate", Bundle::class.java
            )
            XposedBridge.hookMethod(onCreateMethod, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val activity = param.thisObject as Activity
                    if (clickContinueInstallButton(activity)) {
                        activity.finish()
                        log("hookAdbInstall success")
                    }
                }
            })
        } catch (t: Throwable) {
            log("hookAdbInstall fail: ${t.getStackInfo()}")
        }
    }

    /**
     * 模拟点击继续安装按钮
     */
    private fun clickContinueInstallButton(activity: Activity): Boolean {
        var result = false
        try {
            val onClickMethod = XposedHelpers.findMethodExactIfExists(
                activity::class.java, "onClick",
                DialogInterface::class.java, Int::class.java
            )
            if (onClickMethod != null) {
                onClickMethod.invoke(activity, null, DialogInterface.BUTTON_NEGATIVE)
                result = true
            }
        } catch (t: Throwable) {
            log("clickContinueInstallButton fail: ${t.getStackInfo()}")
        }
        return result
    }

    /**
     * 解除限制: 特殊权限拦截 1
     */
    private fun hookSpecialPermissionIntercept(classLoader: ClassLoader) {
        try {
            val activityClass = XposedHelpers.findClassIfExists(
                SpecialPermissionInterceptActivity, classLoader
            )
            if (activityClass == null) {
                log("hookSpecialPermissionIntercept cancel: $SpecialPermissionInterceptActivity not found")
                return
            }
            val onStartMethod = XposedHelpers.findMethodBestMatch(
                activityClass, "onStart", *emptyArray()
            )
            XposedBridge.hookMethod(onStartMethod, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val activity = param.thisObject as Activity
                    hookSpecialPermissionInterceptInternal(activity)
                }
            })
        } catch (t: Throwable) {
            log("hookSpecialPermissionIntercept fail: ${t.getStackInfo()}")
        }
    }

    /**
     * 解除限制: 特殊权限拦截 2
     */
    private fun hookSpecialPermissionInterceptInternal(activity: Activity) {
        try {
            val getSupportFragmentManagerMethod = findGetSupportFragmentManagerMethod(activity)
            if (getSupportFragmentManagerMethod == null) {
                log("hookSpecialPermissionInterceptInternal cancel: method getSupportFragmentManager not found")
                return
            }
            val findFragmentByIdMethod = findFindFragmentByIdMethod(activity)
            if (findFragmentByIdMethod == null) {
                log("hookSpecialPermissionInterceptInternal cancel: method findFragmentById not found")
                return
            }
            val fragmentManager = getSupportFragmentManagerMethod.invoke(activity)
            //获取 SpecialPermissionInterceptActivity 页面的 Fragment
            val fragment = findFragmentByIdMethod.invoke(fragmentManager, android.R.id.content)
            if (fragment == null) {
                log("hookSpecialPermissionInterceptInternal cancel: findFragmentById return null")
                return
            }
            //获取 Fragment 父类的 Handler 对象(用来倒计时的)
            val countDownHandler = findCountDownHandler(fragment)
            if (countDownHandler == null) {
                log("hookSpecialPermissionInterceptInternal cancel: countDownHandler not found")
                return
            }
            //获取设置倒计时方法
            val setCountDownMethod = findSetCountDownMethod(countDownHandler)
            if (setCountDownMethod == null) {
                log("hookSpecialPermissionInterceptInternal cancel: setCountDownMethod not found")
                return
            }
            //设置倒计时为 0, 并立即结束倒计时
            setCountDownMethod.invoke(countDownHandler, 0)
            countDownHandler.sendEmptyMessage(1)
            //自动勾选 [CheckBox] 并设置 [确认] 按钮可点击
            setCheckBoxChecked(fragment)
            setButtonEnabled(fragment)
        } catch (t: Throwable) {
            log("hookSpecialPermissionInterceptInternal fail: ${t.getStackInfo()}")
        }
    }

    private fun setCheckBoxChecked(fragment: Any) {
        try {
            for (field in fragment.javaClass.declaredFields) {
                field.isAccessible = true
                if (field.type == CheckBox::class.java) {
                    val checkBox = field.get(fragment) as CheckBox
                    checkBox.isChecked = true
                }
            }
            log("setCheckBoxChecked success")
        } catch (t: Throwable) {
            log("setCheckBoxChecked fail: ${t.getStackInfo()}")
        }
    }

    private fun setButtonEnabled(fragment: Any) {
        try {
            for (field in fragment.javaClass.superclass.declaredFields) {
                field.isAccessible = true
                if (field.type == Button::class.java) {
                    val button = field.get(fragment) as Button
                    button.isEnabled = true
                }
            }
            log("setButtonEnabled success")
        } catch (t: Throwable) {
            log("setButtonEnabled fail: ${t.getStackInfo()}")
        }
    }

    private fun findSetCountDownMethod(countDownHandler: Handler): Method? {
        return setCountDownMethod ?: kotlin.run {
            try {
                var targetMethod: Method? = null
                for (method in countDownHandler.javaClass.declaredMethods) {
                    method.isAccessible = true
                    if (method.returnType != Void.TYPE) {
                        continue
                    }
                    val parameterTypes = method.parameterTypes
                    if (parameterTypes.size != 1) {
                        continue
                    }
                    if (parameterTypes[0] == Int::class.java) {
                        targetMethod = method
                        break
                    }
                }
                if (targetMethod != null) {
                    //hook 设置倒计时时间方法
                    XposedBridge.hookMethod(targetMethod, object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            val countDown = param.args[0] as Int
                            if (countDown > 0) {
                                param.args[0] = -1
                                log("hook setCountDownMethod before success")
                            }
                        }

                        override fun afterHookedMethod(param: MethodHookParam) {
                            if (param.args[0] == -1) {
                                val handler = param.thisObject as Handler
                                handler.sendEmptyMessage(1)
                                log("hook setCountDownMethod after success")
                            }
                        }
                    })
                }
                targetMethod
            } catch (t: Throwable) {
                null
            }
        }.also { method ->
            setCountDownMethod = method
        }
    }

    private fun findGetSupportFragmentManagerMethod(activity: Activity): Method? {
        return getSupportFragmentManagerMethod ?: kotlin.run {
            try {
                XposedHelpers.findMethodBestMatch(
                    activity.javaClass,
                    "getSupportFragmentManager",
                    *emptyArray()
                )
            } catch (t: Throwable) {
                null
            }
        }.also {
            getSupportFragmentManagerMethod = it
        }
    }

    private fun findFindFragmentByIdMethod(activity: Activity): Method? {
        return findFragmentByIdMethod ?: kotlin.run {
            try {
                val getSupportFragmentManagerMethod = findGetSupportFragmentManagerMethod(activity) ?: return null
                val fragmentManager = getSupportFragmentManagerMethod.invoke(activity, *emptyArray())
                var targetMethod: Method? = null
                for (method in fragmentManager.javaClass.methods) {
                    method.isAccessible = true
                    if ("androidx.fragment.app.Fragment" != method.returnType.name) {
                        continue
                    }
                    val parameterTypes = method.parameterTypes
                    if (parameterTypes.size != 1) {
                        continue
                    }
                    if (parameterTypes[0] == Int::class.java) {
                        targetMethod = method
                        break
                    }
                }
                targetMethod
            } catch (t: Throwable) {
                null
            }
        }.also {
            findFragmentByIdMethod = it
        }
    }

    private fun findCountDownHandler(fragment: Any): Handler? {
        return countDownHandler ?: kotlin.run {
            try {
                var handler: Handler? = null
                for (field in fragment.javaClass.superclass.declaredFields) {
                    field.isAccessible = true
                    if (Handler::class.java.isAssignableFrom(field.type)) {
                        handler = field.get(fragment) as? Handler
                        break
                    }
                }
                handler
            } catch (t: Throwable) {
                null
            }
        }.also {
            countDownHandler = it
        }
    }

    /**
     * 接触限制: adb 调试
     */
    private fun hookAdbInput(classLoader: ClassLoader) {
        try {
            val activityClass = XposedHelpers.findClassIfExists(
                AdbInputApplyActivity, classLoader
            )
            if (activityClass == null) {
                log("hookAdbInput cancel: $AdbInputApplyActivity not found")
                return
            }
            val onCreateMethod = XposedHelpers.findMethodBestMatch(
                activityClass, "onCreate", Bundle::class.java
            )
            XposedBridge.hookMethod(onCreateMethod, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val activity = param.thisObject as Activity
                    hookAdbInputInternal(activity)
                }
            })
        } catch (t: Throwable) {
            log("hookAdbInput fail: ${t.getStackInfo()}")
        }
    }

    private fun hookAdbInputInternal(activity: Activity) {
        try {
            //获取 [步骤字段] 和 [倒计时字段] 和 [handler字段]
            var stepField: Field? = null
            var countDownField: Field? = null
            var handlerField: Field? = null
            for (field in activity.javaClass.declaredFields) {
                field.isAccessible = true
                if (field.type == Int::class.java) {
                    when (field.get(activity) as Int) {
                        1 -> stepField = field
                        5 -> countDownField = field
                    }
                }
                if (Handler::class.java.isAssignableFrom(field.type)) {
                    handlerField = field
                }
            }
            if (stepField == null || countDownField == null || handlerField == null) {
                log("hookAdbInputInternal cancel: stepField=$stepField, countDownField=$countDownField, handlerField=$handlerField")
                return
            }
            //立即跳转到最后一步
            stepField.set(activity, 3)
            countDownField.set(activity, 1)
            val handler = handlerField.get(activity) as Handler
            handler.removeMessages(100)
            handler.sendEmptyMessage(100)
            log("hookAdbInputInternal success")
        } catch (t: Throwable) {
            log("hookAdbInputInternal fail: ${t.getStackInfo()}")
        }
    }

}