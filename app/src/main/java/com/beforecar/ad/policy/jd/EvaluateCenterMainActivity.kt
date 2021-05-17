package com.beforecar.ad.policy.jd

import android.app.Activity
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
import de.robv.android.xposed.XposedHelpers

/**
 * @author: wangpan
 * @email: p.wang@aftership.com
 * @date: 2021/5/14
 */
class EvaluateCenterMainActivity {

    /**
     * EvaluateCenterMainActivity 实例
     */
    private var currentActivity: Activity? = null

    /**
     * 做延时操作用
     */
    private var handler: Handler? = null

    /**
     * 发布评论帮助类
     */
    private val jdPushCommentHelper = JDPushCommentHelper(this)

    /**
     * loading dialog
     */
    private var loadingDialog: LoadingDialog? = null

    /**
     * button tag
     */
    private val buttonView = "button_tag"

    private fun log(content: Any?) {
        JDHookPolicy.log(content)
    }

    fun onCreate(activity: Activity) {
        log("EvaluateCenterMainActivity onCreate")
        currentActivity = activity
        handler = Handler(Looper.getMainLooper())
        val button = addCommentButton(activity)
        button.setOnClickListener {
            startPushComment(activity)
        }
    }

    fun onDestroy() {
        log("EvaluateCenterMainActivity onDestroy")
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
            tag = buttonView
        }
        contentLayout.addView(button)
        return button
    }

    private fun findCommentButton(contentLayout: FrameLayout): View? {
        for (index in 0 until contentLayout.childCount) {
            val child = contentLayout.getChildAt(index)
            if (child.tag == buttonView) {
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