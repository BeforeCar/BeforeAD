package com.beforecar.ad.policy.jd

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.beforecar.ad.utils.AppUtils

/**
 * @author: wangpan
 * @email: p.wang@aftership.com
 * @date: 2021/5/14
 */
class LoadingDialog(context: Context) : AlertDialog(context) {

    companion object {
        private const val PROGRESS_BAR = "progress_bar"
        private const val TOTAL_TEXT_VIEW = "total_text_view"
        private const val DONE_TEXT_VIEW = "done_text_view"
    }

    private val loadingSize = AppUtils.dp2px(context, 200f).toInt()
    private val loadingView: ViewGroup by lazy { createLoadingView(context) }
    private var doneCount: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(loadingView)
        setCancelable(true)
        setCanceledOnTouchOutside(false)
        setLoadingText()
    }

    override fun onStart() {
        super.onStart()
        window?.apply {
            val params = attributes
            params.width = loadingSize
            params.height = loadingSize
            params.gravity = Gravity.CENTER
            attributes = params
        }
    }

    private fun findChildView(tag: Any): View? {
        for (index in 0 until loadingView.childCount) {
            val tv = loadingView.getChildAt(index)
            if (tv?.tag == tag) {
                return tv
            }
        }
        return null
    }

    private fun setLoadingText() {
        val totalTv = findChildView(TOTAL_TEXT_VIEW) as? TextView ?: return
        val doneTv = findChildView(DONE_TEXT_VIEW) as? TextView ?: return
        val loadingText = "加载中.."
        val loadingTextWidth = totalTv.paint.measureText(loadingText)
        val padding = ((loadingSize - loadingTextWidth) * 0.5f).toInt()
        totalTv.text = loadingText
        totalTv.setPadding(padding, 0, 0, 0)
        doneTv.isInvisible = true
    }

    @SuppressLint("SetTextI18n")
    fun setTotalAndDoneText(totalCount: Int, doneCount: Int) {
        val totalTv = findChildView(TOTAL_TEXT_VIEW) as? TextView ?: return
        val doneTv = findChildView(DONE_TEXT_VIEW) as? TextView ?: return
        val totalText = "待评论 $totalCount 个"
        val doneText = "已评论 $doneCount 个"
        val totalTextWidth = totalTv.paint.measureText(totalText)
        val padding = ((loadingSize - totalTextWidth) * 0.5f).toInt()
        totalTv.text = totalText
        totalTv.setPadding(padding, 0, 0, 0)
        doneTv.isVisible = true
        doneTv.text = doneText
        doneTv.setPadding(padding, 0, 0, 0)
        this.doneCount = doneCount
    }

    @SuppressLint("SetTextI18n")
    fun increaseDoneText() {
        val doneTv = findChildView(DONE_TEXT_VIEW) as? TextView ?: return
        doneTv.text = "已评论 ${++doneCount} 个"
    }

    private fun createLoadingView(context: Context): ViewGroup {
        val progressBar = ProgressBar(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            isIndeterminate = true
            tag = PROGRESS_BAR
        }
        val totalTv = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).also {
                it.topMargin = AppUtils.dp2px(context, 20f).toInt()
            }
            isSingleLine = true
            setTextColor(Color.BLACK)
            setPadding(0, 0, 0, 0)
            tag = TOTAL_TEXT_VIEW
        }
        val doneTv = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).also {
                it.topMargin = AppUtils.dp2px(context, 10f).toInt()
            }
            isSingleLine = true
            setTextColor(Color.BLACK)
            setPadding(0, 0, 0, 0)
            tag = DONE_TEXT_VIEW
        }
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            gravity = Gravity.CENTER
            layoutParams = ViewGroup.LayoutParams(loadingSize, loadingSize)
            addView(progressBar)
            addView(totalTv)
            addView(doneTv)
        }
    }
}