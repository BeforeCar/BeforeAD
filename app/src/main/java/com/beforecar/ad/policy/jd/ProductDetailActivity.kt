package com.beforecar.ad.policy.jd

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import com.beforecar.ad.policy.JDHookPolicy
import com.beforecar.ad.policy.base.getStackInfo
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import java.net.URLEncoder

/**
 * @author: wangpan
 * @email: p.wang@aftership.com
 * @date: 2021/5/17
 *
 * 商品详情
 */
@SuppressLint("StaticFieldLeak")
object ProductDetailActivity {

    private var activity: Activity? = null

    private var historyPriceDialog: HistoryPriceDialog? = null

    private fun log(content: Any?) {
        JDHookPolicy.log(content)
    }

    private fun showToast(msg: String) {
        val currentActivity = activity ?: return
        currentActivity.runOnUiThread {
            Toast.makeText(currentActivity, msg, Toast.LENGTH_SHORT).show()
        }
    }

    fun startHook(application: Application, classLoader: ClassLoader) {
        //监听生命周期
        registerLifecycle(classLoader)
        //创建历史价格 item
        addHistoryPriceItem(classLoader)
        //设置历史价格 item 点击监听
        setHistoryPriceItemClickListener(classLoader)
    }

    private fun registerLifecycle(classLoader: ClassLoader) {
        try {
            val activityCls = XposedHelpers.findClass("com.jd.lib.productdetail.ProductDetailActivity", classLoader)
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
        this.activity = activity
    }

    private fun onDestroy() {
        this.activity = null
        dismissHistoryPriceDialog()
    }

    /**
     * 设置历史价格 item 点击监听
     */
    private fun setHistoryPriceItemClickListener(classLoader: ClassLoader) {
        try {
            val itemCls = XposedHelpers.findClass("com.jingdong.sdk.jdshare.a.b", classLoader)
            XposedHelpers.findAndHookMethod(
                "com.jingdong.app.mall.basic.ShareActivity", classLoader, "a", itemCls, object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val item = param.args[0] as Any
                        val channelName = XposedHelpers.getObjectField(item, "channelName")
                        if (channelName == "HistoryPrice") {
                            val shareActivity = param.thisObject as Activity
                            val historyPriceUrl = getHistoryPriceUrl(shareActivity)
                            log("getHistoryPriceUrl: $historyPriceUrl")
                            showHistoryPriceDialog(historyPriceUrl)
                            shareActivity.finish()
                            param.result = null
                        }
                    }

                    private fun getHistoryPriceUrl(shareActivity: Activity): String {
                        try {
                            val shareInfo = XposedHelpers.getObjectField(shareActivity, "shareInfo")
                            val shareUrl = XposedHelpers.callMethod(shareInfo, "getUrl") as String
                            return "http://p.zwjhl.com/price.aspx?url=${URLEncoder.encode(shareUrl, "UTF-8")}"
                        } catch (t: Throwable) {
                            log("getHistoryPriceUrl fail: ${t.getStackInfo()}")
                        }
                        return ""
                    }
                }
            )
        } catch (t: Throwable) {
            log("setHistoryPriceItemClickListener fail: ${t.getStackInfo()}")
        }
    }

    private fun showHistoryPriceDialog(historyPriceUrl: String) {
        val currentActivity = activity
        if (currentActivity == null || historyPriceUrl.isEmpty()) {
            return
        }
        dismissHistoryPriceDialog()
        val dialog = HistoryPriceDialog(currentActivity)
        dialog.url = historyPriceUrl
        dialog.show()
        this.historyPriceDialog = dialog
    }

    private fun dismissHistoryPriceDialog() {
        historyPriceDialog?.dismiss()
        historyPriceDialog = null
    }

    /**
     * 创建历史价格 item
     */
    private fun addHistoryPriceItem(classLoader: ClassLoader) {
        try {
            XposedHelpers.findAndHookConstructor(
                "com.jingdong.sdk.jdshare.cell.ChannelAdapter", classLoader,
                Context::class.java, List::class.java, object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        addHistoryPriceItemInternal(param)
                    }

                    @Suppress("UNCHECKED_CAST")
                    private fun addHistoryPriceItemInternal(param: MethodHookParam) {
                        try {
                            val list = param.args[1] as? ArrayList<Any>
                            if (list.isNullOrEmpty()) return
                            //复制链接 item
                            var copyUrlItem: Any? = null
                            for (item in list) {
                                val channelName = XposedHelpers.getObjectField(item, "channelName")
                                if (channelName == "CopyURL") {
                                    copyUrlItem = item
                                    break
                                }
                            }
                            if (copyUrlItem == null) return
                            //创建历史价格 item
                            val historyPriceItem = XposedHelpers.newInstance(
                                copyUrlItem.javaClass, "HistoryPrice", 2130838366, "", "历史价格", false
                            )
                            list.add(historyPriceItem)
                            log("addHistoryPriceItemInternal success")
                        } catch (t: Throwable) {
                            log("addHistoryPriceItemInternal fail: ${t.getStackInfo()}")
                        }
                    }
                }
            )
        } catch (t: Throwable) {
            log("addHistoryPriceItem fail: ${t.getStackInfo()}")
        }
    }

}