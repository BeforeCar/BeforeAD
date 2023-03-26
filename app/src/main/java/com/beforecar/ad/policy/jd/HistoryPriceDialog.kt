package com.beforecar.ad.policy.jd

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ProgressBar
import com.beforecar.ad.utils.AppUtils
import com.beforecar.ad.utils.setVisible

/**
 * @author: wangpan
 * @email: p.wang@aftership.com
 * @date: 2021/5/18
 */
class HistoryPriceDialog(context: Context) : AlertDialog(context),
    DialogInterface.OnDismissListener {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar

    private var currentProgress: Int = 0

    private val hideProgressBar: Runnable by lazy {
        Runnable {
            progressBar.setVisible(false)
        }
    }

    var url: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val contentView = createContentView(context)
        setContentView(contentView)
        initWebView()
        setCancelable(true)
        setCanceledOnTouchOutside(false)
        setOnDismissListener(this)
    }

    override fun onStart() {
        super.onStart()
        window?.apply {
            val params = attributes
            params.width = (AppUtils.getScreenWidth(context) * 0.9f).toInt()
            params.height = (AppUtils.getScreenHeight(context) * 0.8f).toInt()
            params.gravity = Gravity.CENTER
            attributes = params
        }
    }

    private fun createContentView(context: Context): ViewGroup {
        val rootView = FrameLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            setBackgroundColor(Color.WHITE)
        }
        val webView = WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        }
        val progressBar =
            ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
                val dp2 = AppUtils.dp2px(context, 2f).toInt()
                layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, dp2)
                max = 100
                progress = 0
            }
        rootView.addView(webView)
        rootView.addView(progressBar)
        this.webView = webView
        this.progressBar = progressBar
        return rootView
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() {
        progressBar.setVisible(false)
        webView.settings.apply {
            javaScriptEnabled = true
            setSupportZoom(true)
            loadWithOverviewMode = true
            useWideViewPort = true
            defaultTextEncodingName = "utf-8"
            loadsImagesAutomatically = true
            domStorageEnabled = true
            databaseEnabled = true
//            setAppCacheEnabled(true)
//            setAppCachePath(context.cacheDir.absolutePath)
            allowFileAccess = true
            cacheMode = WebSettings.LOAD_DEFAULT
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                updateLoadingProgress(newProgress)
            }
        }
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                updateLoadingProgress(0, true)
            }

            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    view.loadUrl(url)
                    return true
                }
                try {
                    val intent: Intent = when {
                        url.startsWith("intent://") -> {
                            Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                        }
                        url.startsWith("android-app://") -> {
                            Intent.parseUri(url, Intent.URI_ANDROID_APP_SCHEME)
                        }
                        else -> {
                            Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        }
                    }
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return true
            }
        }
        webView.loadUrl(url)
    }

    private fun updateLoadingProgress(newProgress: Int, force: Boolean = false) {
        //更新进度条
        if (newProgress > currentProgress || force) {
            currentProgress = newProgress
            progressBar.progress = newProgress
        }
        if (newProgress in 1..99) {
            setProgressBarVisibility(true)
        } else {
            setProgressBarVisibility(false)
        }
    }

    private fun setProgressBarVisibility(show: Boolean) {
        progressBar.removeCallbacks(hideProgressBar)
        if (show) {
            progressBar.setVisible(true)
        } else {
            progressBar.postDelayed(hideProgressBar, 200)
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        AppUtils.destroyWebView(webView)
    }

}