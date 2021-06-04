package com.beforecar.ad.policy

import android.app.Application
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ListAdapter
import android.widget.ListView
import com.beforecar.ad.policy.base.AbsHookPolicy
import com.beforecar.ad.policy.base.getStackInfo
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import java.lang.reflect.Method

/**
 * @author: wangpan
 * @email: p.wang@aftership.com
 * @date: 2021/5/6
 *
 * 微信
 */
class WeiXinHookPolicy : AbsHookPolicy() {

    companion object {

        const val PYQ_ACTIVITY_CLASS_NAME = "com.tencent.mm.plugin.sns.ui.SnsTimeLineUI"

        const val GZH_ACTIVITY_CLASS_NAME = "com.tencent.mm.plugin.brandservice.ui.timeline.BizTimeLineUI"

        const val SNSINFO_CLASS_NAME = "com.tencent.mm.plugin.sns.storage.SnsInfo"

    }

    override val tag: String = "tag_weixin"

    /**
     * 朋友圈列表 adapter
     */
    private var pyqListAdapterClass: Class<*>? = null

    /**
     * 公众号列表 adapter
     */
    private var gzhListAdapterClass: Class<*>? = null

    override fun getPackageName(): String {
        return "com.tencent.mm"
    }

    override fun onMainApplicationAfterCreate(application: Application, classLoader: ClassLoader) {
        //hook ListView
        hookListView()
    }

    /**
     * hook ListView setAdapter
     */
    private fun hookListView() {
        try {
            pyqListAdapterClass = null
            gzhListAdapterClass = null
            XposedHelpers.findAndHookMethod(
                ListView::class.java, "setAdapter", ListAdapter::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val baseAdapter = param.args[0] as? BaseAdapter ?: return
                        val listView = param.thisObject as ListView
                        if (preparePYQListAdapter(listView, baseAdapter)) {
                            //去除朋友圈列表广告
                            removePYQAdItems()
                        }
                        if (prepareGZHListAdapter(listView, baseAdapter)) {
                            //去除公众号列表广告
                            removeGZHAdItems()
                        }
                    }
                }
            )
        } catch (t: Throwable) {
            pyqListAdapterClass = null
            gzhListAdapterClass = null
            log("hookListView fail: ${t.getStackInfo()}")
        }
    }

    private fun preparePYQListAdapter(listView: ListView, baseAdapter: BaseAdapter): Boolean {
        try {
            if (pyqListAdapterClass != null) return false
            val context = listView.context ?: return false
            val classLoader = listView.javaClass.classLoader!!
            val activityCls = XposedHelpers.findClassIfExists(PYQ_ACTIVITY_CLASS_NAME, classLoader)
            if (activityCls != null && context.javaClass == activityCls) {
                pyqListAdapterClass = baseAdapter.javaClass
                log("preparePYQListAdapter success: $pyqListAdapterClass")
                return true
            }
        } catch (t: Throwable) {
            log("preparePYQListAdapter fail: ${t.getStackInfo()}")
        }
        return false
    }

    private fun prepareGZHListAdapter(listView: ListView, baseAdapter: BaseAdapter): Boolean {
        try {
            if (gzhListAdapterClass != null) return false
            val context = listView.context ?: return false
            val classLoader = listView.javaClass.classLoader!!
            val activityCls = XposedHelpers.findClassIfExists(GZH_ACTIVITY_CLASS_NAME, classLoader)
            if (activityCls != null && context.javaClass == activityCls) {
                gzhListAdapterClass = baseAdapter.javaClass
                log("prepareGZHListAdapter success: $gzhListAdapterClass")
                return true
            }
        } catch (t: Throwable) {
            log("prepareGZHListAdapter fail: ${t.getStackInfo()}")
        }
        return false
    }

    private fun getPYQItemMethod(adapterClass: Class<*>): Method? {
        try {
            val snsInfoCls = XposedHelpers.findClass(SNSINFO_CLASS_NAME, adapterClass.classLoader)
            for (method in adapterClass.declaredMethods) {
                val parameterTypes = method.parameterTypes
                val returnType = method.returnType
                if (parameterTypes.size == 1 && parameterTypes[0] == Int::class.java && returnType == snsInfoCls) {
                    log("getPYQItemMethod success: $method")
                    return method
                }
            }
        } catch (t: Throwable) {
            log("prepareGetPYQItemMethod fail: ${t.getStackInfo()}")
        }
        log("getPYQItemMethod fail: null")
        return null
    }

    /**
     * 去除朋友圈列表广告
     */
    private fun removePYQAdItems() {
        try {
            val adapterClass = pyqListAdapterClass ?: return
            val getPYQItemMethod = getPYQItemMethod(adapterClass) ?: return
            XposedHelpers.findAndHookMethod(
                adapterClass, "getView",
                Int::class.java, View::class.java, ViewGroup::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val itemView = param.result as? View ?: return
                        val adapter = param.thisObject as BaseAdapter
                        val position = param.args[0] as Int
                        if (isPQYAdItem(adapter, position)) {
                            itemView.setSize(1, 1)
                            log("removePYQAdItems success: $position")
                        }
                    }

                    private fun isPQYAdItem(adapter: BaseAdapter, position: Int): Boolean {
                        try {
                            val snsInfo = getPYQItemMethod.invoke(adapter, position) ?: return false
                            return XposedHelpers.getObjectField(snsInfo, "adsnsinfo") != null
                        } catch (t: Throwable) {
                            log("isPQYAdItem: ${t.getStackInfo()}")
                        }
                        return false
                    }
                }
            )
        } catch (t: Throwable) {
            log("removePYQAdItems fail: ${t.getStackInfo()}")
        }
    }

    /**
     * 去除公众号列表广告
     */
    private fun removeGZHAdItems() {
        try {
            val adapterClass = gzhListAdapterClass ?: return
            XposedHelpers.findAndHookMethod(
                adapterClass, "getView",
                Int::class.java, View::class.java, ViewGroup::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val itemView = param.result as? View ?: return
                        val adapter = param.thisObject as BaseAdapter
                        val position = param.args[0] as Int
                        if (isGZHAdItem(adapter, position)) {
                            itemView.setSize(1, 1)
                            log("removeGZHAdItems success: $position")
                        }
                    }

                    private fun isGZHAdItem(adapter: BaseAdapter, position: Int): Boolean {
                        try {
                            return adapter.getItemViewType(position) != 1
                        } catch (t: Throwable) {
                            log("isGZHAdItem fail: ${t.getStackInfo()}")
                        }
                        return false
                    }
                }
            )
        } catch (t: Throwable) {
            log("removeGZHAdItems fail: ${t.getStackInfo()}")
        }
    }

    private fun createEmptyView(context: Context): View {
        return View(context).apply {
            layoutParams = ViewGroup.LayoutParams(0, 0)
        }
    }

    private fun View.setSize(width: Int, height: Int) {
        layoutParams = layoutParams?.also {
            it.width = width
            it.height = height
        } ?: ViewGroup.LayoutParams(width, height)
    }

}