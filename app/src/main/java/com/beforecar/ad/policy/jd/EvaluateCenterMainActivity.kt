package com.beforecar.ad.policy.jd

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Toast
import com.beforecar.ad.policy.JDHookPolicy
import com.beforecar.ad.policy.base.getStackInfo
import com.beforecar.ad.utils.AppUtils
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

/**
 * @author: wangpan
 * @email: p.wang@aftership.com
 * @date: 2021/5/14
 *
 * 评论中心
 */
@SuppressLint("StaticFieldLeak")
object EvaluateCenterMainActivity {

    /**
     * EvaluateCenterMainActivity 实例
     */
    private var currentActivity: Activity? = null

    /**
     * 做延时操作用
     */
    private var handler: Handler? = null

    /**
     * button tag
     */
    private const val BUTTON_VIEW = "button_tag"

    /**
     * 发布评论帮助类
     */
    private val jdPushCommentHelper = JDPushCommentHelper(this)

    /**
     * loading dialog
     */
    private var loadingDialog: LoadingDialog? = null

    private fun log(content: Any?) {
        JDHookPolicy.log(content)
    }

    fun startHook(application: Application, classLoader: ClassLoader) {
        try {
            //等待评论中心模块加载成功后开始 hook
            val helperCls = XposedHelpers.findClass("com.jingdong.common.utils.AuraPreLoadBundleHelper", classLoader)
            XposedHelpers.findAndHookMethod(helperCls, "preLoadClass", Runnable::class.java, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val runnable = param.args[0] as Runnable
                    if (runnable.javaClass.name == "com.jd.lib.evaluatecenter.a") {
                        startHookInternal(application, classLoader)
                    }
                }
            })
        } catch (t: Throwable) {
            log("startHookInternal fail: ${t.getStackInfo()}")
        }
    }

    private fun startHookInternal(application: Application, classLoader: ClassLoader) {
        //监听生命周期
        registerLifecycle(classLoader)
    }

    /**
     * hook EvaluateCenterMainActivity
     */
    private fun registerLifecycle(classLoader: ClassLoader) {
        try {
            val activityCls = XposedHelpers.findClass(
                "com.jd.lib.evaluatecenter.view.activity.EvaluateCenterMainActivity",
                classLoader
            )
            XposedHelpers.findAndHookMethod(activityCls, "onCreate", Bundle::class.java, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val activity = param.thisObject as Activity
                    onCreate(activity)
                }
            })
            XposedHelpers.findAndHookMethod(activityCls, "onDestroy", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    onDestroy()
                }
            })
        } catch (t: Throwable) {
            log("registerLifecycle fail: ${t.getStackInfo()}")
        }
    }

    private fun onCreate(activity: Activity) {
        currentActivity = activity
        handler = Handler(Looper.getMainLooper())
        val button = addCommentButton(activity)
        button.setOnClickListener {
            startPushComment(activity)
        }
    }

    private fun onDestroy() {
        stopPushComment()
        handler?.removeCallbacksAndMessages(null)
        handler = null
        currentActivity = null
    }

    /**
     * 往界面中添加一键评论按钮
     */
    private fun addCommentButton(activity: Activity): Button {
        val contentLayout = activity.findViewById<FrameLayout>(android.R.id.content)
        findCommentButton(contentLayout)?.let {
            contentLayout.removeView(it)
        }
        val button = Button(activity).apply {
            text = "一键评论"
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.END
                bottomMargin = AppUtils.dp2px(activity, 32f).toInt()
                rightMargin = AppUtils.dp2px(activity, 16f).toInt()
            }
            tag = BUTTON_VIEW
        }
        contentLayout.addView(button)
        return button
    }

    private fun findCommentButton(contentLayout: FrameLayout): View? {
        for (index in 0 until contentLayout.childCount) {
            val child = contentLayout.getChildAt(index)
            if (child.tag == BUTTON_VIEW) {
                return child
            }
        }
        return null
    }

    fun runOnUIThread(runnable: Runnable, delayMillis: Long): Boolean {
        return handler?.postDelayed(runnable, delayMillis) ?: false
    }

    fun removeAllRunnable() {
        handler?.removeCallbacksAndMessages(null)
    }

    private fun showLoading(activity: Activity) {
        stopLoading()
        val dialog = LoadingDialog(activity)
        dialog.setOnCancelListener {
            stopPushComment()
        }
        dialog.show()
        loadingDialog = dialog
    }

    private fun stopLoading() {
        loadingDialog?.dismiss()
        loadingDialog = null
    }

    /**
     * 开始一键发布评论
     */
    private fun startPushComment(activity: Activity) {
        showLoading(activity)
        jdPushCommentHelper.startPushComment(activity, object : JDPushCommentHelper.IPushCommentCallback {
            override fun onStart(totalCount: Int) {
                val currentActivity = currentActivity ?: return
                currentActivity.runOnUiThread {
                    loadingDialog?.setTotalAndDoneText(totalCount, 0)
                }
            }

            override fun onSuccess() {
                val currentActivity = currentActivity ?: return
                currentActivity.runOnUiThread {
                    loadingDialog?.increaseDoneText()
                }
            }

            override fun onEmpty() {
                val currentActivity = currentActivity ?: return
                currentActivity.runOnUiThread {
                    stopLoading()
                    showToast(currentActivity, "没有待评论的订单了")
                    //刷新评论列表
                    refreshCommentList(currentActivity)
                }
            }

            override fun onError() {
                val currentActivity = currentActivity ?: return
                currentActivity.runOnUiThread {
                    stopLoading()
                    showToast(currentActivity, "发布评论错误")
                    //刷新评论列表
                    refreshCommentList(currentActivity)
                }
            }

            override fun onCancel() {
                val currentActivity = currentActivity ?: return
                currentActivity.runOnUiThread {
                    stopLoading()
                    showToast(currentActivity, "操作取消")
                    //刷新评论列表
                    refreshCommentList(currentActivity)
                }
            }
        })
    }

    /**
     * 停止发布评论
     */
    private fun stopPushComment() {
        jdPushCommentHelper.stopPushComment()
        stopLoading()
    }

    /**
     * 刷新评论列表
     */
    private fun refreshCommentList(activity: Activity) {
        try {
            val presenter = XposedHelpers.callMethod(activity, "getPresenter") as Any
            XposedHelpers.callMethod(presenter, "a", 0, false, false)
            log("refreshCommentList success")
        } catch (t: Throwable) {
            log("refreshCommentList fail: ${t.getStackInfo()}")
        }
    }

    fun showToast(activity: Activity, msg: String) {
        Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()
    }

}