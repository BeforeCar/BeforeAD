package com.beforecar.ad.policy.jd

import android.app.Activity
import android.view.ViewGroup
import com.beforecar.ad.policy.JDHookPolicy
import com.beforecar.ad.policy.base.getStackInfo
import com.beforecar.ad.policy.base.isSafety
import de.robv.android.xposed.XposedHelpers
import org.json.JSONArray
import org.json.JSONObject
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.*

/**
 * @author: wangpan
 * @email: p.wang@aftership.com
 * @date: 2021/5/13
 *
 * 京东自动发布评论
 */
class JDPushCommentHelper(private val evaluateActivity: EvaluateCenterMainActivity) {

    private val random: Random by lazy { Random() }

    /**
     * 上一个发布评论成功的时间
     */
    private var lastPushTimeMillis: Long = -1L

    /**
     * activity
     */
    private var activity: Activity? = null

    /**
     * IPushCommentCallback
     */
    private var pushCommentCallback: IPushCommentCallback? = null

    private fun log(content: Any?) {
        JDHookPolicy.log(content)
    }

    interface IPushCommentCallback {

        fun onStart(totalCount: Int)

        fun onSuccess()

        fun onEmpty()

        fun onError()

        fun onCancel()

    }

    /**
     * 自动发布评论
     * 1. 获取所有的待评论订单, 如果为空就终止, 否则继续
     * 2. 获取写一个待评论订单
     * 3. 发布评论
     */
    fun startPushComment(activity: Activity, callback: IPushCommentCallback) {
        this.activity = activity
        this.pushCommentCallback = callback
        lastPushTimeMillis = -1L
        evaluateActivity.removeAllRunnable()
        //1. 获取所有的待评论订单
        getTotalPushCommentCount()
    }

    /**
     * 停止发布评论
     */
    fun stopPushComment() {
        activity = null
        pushCommentCallback = null
        evaluateActivity.removeAllRunnable()
    }

    /**
     * 获取所有的带评论订单
     */
    private fun getTotalPushCommentCount() {
        val activity = this.activity
        if (activity == null || !activity.isSafety()) {
            log("getTotalPushCommentCount cancel")
            pushCommentCallback?.onCancel()
            return
        }
        try {
            log("getTotalPushCommentCount start")
            val listener = createOnAllListener(
                activity.classLoader,
                onSuccess = { response ->
                    log("getTotalPushCommentCount success")
                    prepareStartNextPushComment(response)
                },
                onFail = {
                    log("getTotalPushCommentCount fail")
                    pushCommentCallback?.onError()
                }
            )
            getCommentWareList(activity, 100, listener)
        } catch (t: Throwable) {
            log("getTotalPushCommentCount fail: ${t.getStackInfo()}")
            pushCommentCallback?.onError()
        }
    }

    /**
     * 解析所有待评论订单数据,准备开始下一个订单评论
     */
    private fun prepareStartNextPushComment(response: Any) {
        val activity = this.activity
        if (activity == null || !activity.isSafety()) {
            log("prepareStartNextPushComment cancel")
            pushCommentCallback?.onCancel()
            return
        }
        try {
            val string = XposedHelpers.callMethod(response, "getString") as? String
            //评论列表数据为空
            if (string.isNullOrEmpty()) {
                log("prepareStartNextPushComment string empty")
                pushCommentCallback?.onEmpty()
                return
            }
            //开始解析数据
            val result = JSONObject(string)
            val commentWareList = parseCommentWareList(result)
            val totalCount = commentWareList?.length() ?: 0
            if (totalCount > 0) {
                pushCommentCallback?.onStart(totalCount)
                //所有待评论订单数据不为空,开始评论下一个订单
                startNextPushComment()
            } else {
                log("prepareStartNextPushComment parse empty: $string")
                pushCommentCallback?.onEmpty()
            }
        } catch (t: Throwable) {
            log("prepareStartNextPushComment fail: ${t.getStackInfo()}")
            pushCommentCallback?.onError()
        }
    }

    /**
     * 继续评论下一个订单
     */
    private fun startNextPushComment() {
        val activity = this.activity
        if (activity == null || !activity.isSafety()) {
            log("startNextPushComment cancel")
            pushCommentCallback?.onCancel()
            return
        }
        val interval = System.currentTimeMillis() - lastPushTimeMillis
        //计算下一个评论发布的延迟时间
        val delay: Long = if (interval >= PUSH_COMMENT_DELAY) {
            0L
        } else {
            PUSH_COMMENT_DELAY - interval
        }
        evaluateActivity.runOnUIThread({ startPushCommentStep1() }, delay)
    }

    /**
     * 1. 获取待评论列表
     */
    private fun startPushCommentStep1() {
        val activity = this.activity
        if (activity == null || !activity.isSafety()) {
            log("startPushCommentStep1 cancel")
            pushCommentCallback?.onCancel()
            return
        }
        try {
            log("startPushCommentStep1 start")
            val listener = createOnAllListener(
                activity.classLoader,
                onSuccess = { response ->
                    log("startPushCommentStep1 success")
                    prepareStartPushCommentStep2(response)
                },
                onFail = {
                    log("startPushCommentStep1 fail")
                    pushCommentCallback?.onError()
                }
            )
            getCommentWareList(activity, 1, listener)
        } catch (t: Throwable) {
            log("startPushCommentStep1 fail: ${t.getStackInfo()}")
            pushCommentCallback?.onError()
        }
    }

    /**
     * 获取评论列表
     */
    @Throws(Throwable::class)
    private fun getCommentWareList(activity: Activity, pageSize: Int, listener: Any) {
        val classLoader = activity.classLoader
        val setting = createCommentWareListHttpSetting(classLoader).apply {
            XposedHelpers.callMethod(this, "setEffectState", 0)
            XposedHelpers.callMethod(this, "putJsonParam", "status", "1")
            XposedHelpers.callMethod(this, "putJsonParam", "pageIndex", "1")
            XposedHelpers.callMethod(this, "putJsonParam", "pageSize", pageSize.toString())
            XposedHelpers.callMethod(this, "putJsonParam", "planType", "1")
            val listenerCls = XposedHelpers.findClass(HttpTaskListener, classLoader)
            XposedHelpers.callMethod(this, "setListener", listenerCls.cast(listener))
        }
        asyncRequest(activity, setting)
    }

    @Throws(Throwable::class)
    private fun createCommentWareListHttpSetting(classLoader: ClassLoader): Any {
        val settingCls = XposedHelpers.findClass(HttpSetting, classLoader)
        return settingCls.newInstance().apply {
            XposedHelpers.callMethod(this, "setHost", getCommentHost(classLoader))
            XposedHelpers.callMethod(this, "setFunctionId", "getCommentWareList")
            XposedHelpers.callMethod(this, "putJsonParam", "evaAuraVersion", getVersionCode(classLoader))
            XposedHelpers.callMethod(this, "setUseFastJsonParser", false)
        }
    }

    /**
     * 解析评论列表数据,准备开始发布屏幕
     */
    private fun prepareStartPushCommentStep2(response: Any) {
        val activity = this.activity
        if (activity == null || !activity.isSafety()) {
            log("prepareStartPushCommentStep2 cancel")
            pushCommentCallback?.onCancel()
            return
        }
        try {
            val string = XposedHelpers.callMethod(response, "getString") as? String
            //评论列表数据为空
            if (string.isNullOrEmpty()) {
                log("prepareStartPushCommentStep2 string empty")
                pushCommentCallback?.onEmpty()
                return
            }
            //开始解析数据
            val result = JSONObject(string)
            val officerLevelInfo = parseOfficerLevelInfo(result)
            val commentWareList = parseCommentWareList(result)
            if (commentWareList != null && commentWareList.length() > 0) {
                //订单数据不为空,开始发布评论
                startPushCommentStep2(officerLevelInfo, commentWareList.getJSONObject(0))
            } else {
                log("prepareStartPushCommentStep2 parse empty")
                pushCommentCallback?.onEmpty()
            }
        } catch (t: Throwable) {
            log("prepareStartPushCommentStep2 fail: ${t.getStackInfo()}")
            pushCommentCallback?.onError()
        }
    }

    /**
     * 解析评论等级数据
     */
    private fun parseOfficerLevelInfo(result: JSONObject): Pair<String, String>? {
        try {
            val newCommentOfficerInfo = result.optJSONObject("newCommentOfficerInfo") ?: return null
            val officerLevelInfo = newCommentOfficerInfo.optJSONObject("officerLevelInfo") ?: return null
            val officerLevel = officerLevelInfo.optInt("officerLevel", 0)
            val growthScore = officerLevelInfo.optInt("growthScore", 0)
            return Pair(officerLevel.toString(), growthScore.toString())
        } catch (t: Throwable) {
            log("getOfficerLevelInfo fail: ${t.getStackInfo()}")
        }
        return null
    }

    /**
     * 解析评论数据
     */
    private fun parseCommentWareList(result: JSONObject): JSONArray? {
        try {
            val commentWareListInfo = result.optJSONObject("commentWareListInfo") ?: return null
            return commentWareListInfo.optJSONArray("commentWareList")
        } catch (t: Throwable) {
            log("getCommentWareList fail: ${t.getStackInfo()}")
        }
        return null
    }

    /**
     * 2. 发布评论
     */
    private fun startPushCommentStep2(officerLevelInfo: Pair<String, String>?, commentWare: JSONObject) {
        val activity = this.activity
        if (activity == null || !activity.isSafety()) {
            log("startPushCommentStep2 cancel")
            pushCommentCallback?.onCancel()
            return
        }
        try {
            log("startPushCommentStep2 start")
            val listener = createOnAllListener(
                activity.classLoader,
                onSuccess = {
                    log("startPushCommentStep2 success")
                    pushCommentCallback?.onSuccess()
                    //保存发布评论成功的时间
                    lastPushTimeMillis = System.currentTimeMillis()
                    //继续评论下一个订单
                    startNextPushComment()
                },
                onFail = {
                    log("startPushCommentStep2 fail")
                    pushCommentCallback?.onError()
                }
            )
            pushComment(activity, officerLevelInfo, commentWare, listener)
        } catch (t: Throwable) {
            log("startPushCommentStep2 fail: ${t.getStackInfo()}")
            pushCommentCallback?.onError()
        }
    }

    /**
     * 发布评论
     */
    @Throws(Throwable::class)
    private fun pushComment(
        activity: Activity,
        officerLevelInfo: Pair<String, String>?,
        commentWare: JSONObject,
        listener: Any
    ) {
        val setting = createPubCommentHttpSetting(activity.classLoader).apply {
            //orderId
            XposedHelpers.callMethod(this, "putJsonParam", "orderId", commentWare.optString("orderId"))
            //productId
            XposedHelpers.callMethod(this, "putJsonParam", "productId", commentWare.optString("wareId"))
            //5星
            XposedHelpers.callMethod(this, "putJsonParam", "commentScore", "5")
            //评论的内容
            val commentData = commentList[random.nextInt(commentList.size)]
            XposedHelpers.callMethod(this, "putJsonParam", "commentData", commentData)
            //anonymousFlag
            var anonymousFlag = commentWare.optString("anonymousFlag")
            if (anonymousFlag.isEmpty()) {
                anonymousFlag = "1"
            }
            XposedHelpers.callMethod(this, "putJsonParam", "anonymousFlag", anonymousFlag)
            //shop
            val shopInfo = commentWare.optJSONObject("shopInfo")
            if (shopInfo != null) {
                XposedHelpers.callMethod(this, "putJsonParam", "shopType", shopInfo.optString("shopType"))
                XposedHelpers.callMethod(this, "putJsonParam", "shopId", shopInfo.optString("shopId"))
            }
            //officerLevelInfo
            if (officerLevelInfo != null) {
                XposedHelpers.callMethod(this, "putJsonParam", "officerLevel", officerLevelInfo.first)
                XposedHelpers.callMethod(this, "putJsonParam", "officerScore", officerLevelInfo.second)
            }
            XposedHelpers.callMethod(this, "putJsonParam", "commentType", null)
            //图片
            XposedHelpers.callMethod(this, "putJsonParam", "pictureInfoList", emptyList<String>())
            XposedHelpers.callMethod(this, "putJsonParam", "addPictureFlag", "0")
            //视频
            XposedHelpers.callMethod(this, "putJsonParam", "videoInfoList", emptyList<String>())
            //extInfo
            val extInfo = JSONObject()
            extInfo.put("mediasExt", "[]")
            XposedHelpers.callMethod(this, "putJsonParam", "extInfo", extInfo)
            XposedHelpers.callMethod(this, "putJsonParam", "syncStoryFlag", "")
            XposedHelpers.callMethod(
                this, "putJsonParam", "voucherStatus", commentWare.optString("voucherStatus", "0")
            )
            //categoryList
            XposedHelpers.callMethod(
                this, "putJsonParam", "categoryList", commentWare.optString("categoryList")
            )
            XposedHelpers.callMethod(this, "putJsonParam", "jshowActivityId", "")
            XposedHelpers.callMethod(this, "putJsonParam", "verticalTagList", null)
            XposedHelpers.callMethod(this, "putJsonParam", "isCommentTagContent", "0")
            XposedHelpers.callMethod(this, "putJsonParam", "evaAuraVersion", getVersionCode(activity.classLoader))
            //listener
            val listenerCls = XposedHelpers.findClass(HttpTaskListener, activity.classLoader)
            XposedHelpers.callMethod(this, "setListener", listenerCls.cast(listener))
        }
        asyncRequest(activity, setting)
    }

    private fun getVersionCode(classLoader: ClassLoader): String {
        try {
            val bundleInfoCls = XposedHelpers.findClass("com.jingdong.jdsdk.auraSetting.AuraBundleInfos", classLoader)
            val bundleId = XposedHelpers.callStaticMethod(bundleInfoCls, "getBundleNameFromBundleId", 21) as String
            val configCls = XposedHelpers.findClass("com.jingdong.jdsdk.auraSetting.AuraBundleConfig", classLoader)
            val bundleConfig = XposedHelpers.callStaticMethod(configCls, "getInstance")
            return XposedHelpers.callMethod(bundleConfig, "getBundleVersionCode", bundleId).toString()
        } catch (t: Throwable) {
            log("getVersionCode fail: ${t.getStackInfo()}")
        }
        return ""
    }

    @Throws(Throwable::class)
    private fun createPubCommentHttpSetting(classLoader: ClassLoader): Any {
        val settingCls = XposedHelpers.findClass(HttpSetting, classLoader)
        return settingCls.newInstance().apply {
            XposedHelpers.callMethod(this, "setHost", getCommentHost(classLoader))
            XposedHelpers.callMethod(this, "setFunctionId", "pubComment")
            XposedHelpers.callMethod(this, "putJsonParam", "evaAuraVersion", getVersionCode(classLoader))
            XposedHelpers.callMethod(this, "setNotifyUser", false)
            XposedHelpers.callMethod(this, "setUseFastJsonParser", true)
            XposedHelpers.callMethod(this, "setEffect", 0)
        }
    }

    /**
     * 执行异步请求
     */
    @Throws(Throwable::class)
    private fun asyncRequest(activity: Activity, httpSetting: Any) {
        val classLoader = activity.classLoader
        val iMyActivityCls = XposedHelpers.findClass("com.jingdong.common.frame.IMyActivity", classLoader)
        val utilCls = XposedHelpers.findClass("com.jingdong.cleanmvp.engine.HttpGroupUtil", classLoader)
        val method = XposedHelpers.findMethodExact(
            utilCls, "getHttpGroupaAsynPool", iMyActivityCls, ViewGroup::class.java
        )
        val httpGroup = method.invoke(utilCls.newInstance(), iMyActivityCls.cast(activity), null)
        XposedHelpers.callMethod(httpGroup, "add", httpSetting)
    }

    @Throws(Throwable::class)
    private fun getCommentHost(classLoader: ClassLoader): String {
        val clazz = XposedHelpers.findClass("com.jingdong.jdsdk.config.Configuration", classLoader)
        return XposedHelpers.callStaticMethod(clazz, "getCommentHost") as String
    }

    /**
     * 创建接口的 listener
     */
    @Throws(Throwable::class)
    private fun createOnAllListener(
        classLoader: ClassLoader,
        onSuccess: ((Any) -> Unit)?,
        onFail: ((Any) -> Unit)?
    ): Any {
        val interfaceCls = XposedHelpers.findClass(OnAllListener, classLoader)
        return Proxy.newProxyInstance(classLoader, arrayOf(interfaceCls), object : InvocationHandler {
            override fun invoke(proxy: Any, method: Method, args: Array<Any>?): Any? {
                when {
                    //如果方法是 Object 中定义的, 必须要让 method 正常执行,
                    //否则 Proxy.newProxyInstance 方法会返回 null
                    method.declaringClass == Any::class.java -> {
                        //java 的可变参和 kotlin 的数组不能通用
                        return method.invoke(this, *(args ?: arrayOfNulls<Any>(0)))
                    }
                    method.name == "onEnd" && args != null -> {
                        onSuccess?.invoke(args[0])
                    }
                    method.name == "onError" && args != null -> {
                        onFail?.invoke(args[0])
                    }
                }
                return null
            }
        })
    }

    companion object {

        const val OnAllListener = "com.jingdong.jdsdk.network.toolbox.HttpGroup\$OnAllListener"
        const val HttpTaskListener = "com.jingdong.jdsdk.network.toolbox.HttpGroup\$HttpTaskListener"
        const val HttpSetting = "com.jingdong.jdsdk.network.toolbox.HttpSetting"

        const val PUSH_COMMENT_DELAY = 5000L

        private val commentList = mutableListOf<String>().apply {
            add("买了很多次了，真的很不错。后面会考虑回购的哦")
            add("好喜欢这个东西，买了很多次了，价格还比较划来")
        }
    }

}