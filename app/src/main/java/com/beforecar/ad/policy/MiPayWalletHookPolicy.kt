package com.beforecar.ad.policy

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.ViewGroup
import com.beforecar.ad.policy.base.AbsHookPolicy
import com.beforecar.ad.policy.base.getStackInfo
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

/**
 * @author: wangpan
 * @email: p.wang@aftership.com
 * @date: 2021/6/29
 *
 * 小米钱包
 */
class MiPayWalletHookPolicy : AbsHookPolicy() {

    companion object {

        const val SplashFragment = "com.xiaomi.jr.app.splash.SplashFragment"

    }

    override val tag: String = "tag_mipay"

    override fun getPackageName(): String {
        return "com.mipay.wallet"
    }

    override fun onMainApplicationBeforeCreate(application: Application, classLoader: ClassLoader) {
        removeSplashAd(classLoader)
    }

    /**
     * 去除启动页广告并加速启动
     */
    private fun removeSplashAd(classLoader: ClassLoader) {
        /**
         * hook SplashFragment 中的 Handler, 当调用 sendEmptyMessageDelayed 时, 设置延迟为 1s
         */
        hookSplashFragment(classLoader)

        try {
            /**
             * hook SQLiteOpenHelper 的创建, 当创建 AdDatabaseHelper 时删除 ad 表
             */
            XposedHelpers.findAndHookConstructor(
                SQLiteOpenHelper::class.java, Context::class.java, String::class.java,
                SQLiteDatabase.CursorFactory::class.java, Int::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val sqLiteOpenHelper = param.thisObject as SQLiteOpenHelper
                        val name = param.args[1] as String
                        if (name == "ad.db") {
                            deleteAdDatabase(sqLiteOpenHelper)
                        }
                    }

                    private fun deleteAdDatabase(sqLiteOpenHelper: SQLiteOpenHelper) {
                        try {
                            sqLiteOpenHelper.writableDatabase.delete("ad", null, null)
                            log("deleteAdDatabase success")
                        } catch (t: Throwable) {
                            log("deleteAdDatabase fail: ${t.getStackInfo()}")
                        }
                    }
                }
            )

            /**
             * hook SQLiteDatabase 的 insert 方法, 当插入 ad 表数据时把数据清除
             */
            XposedHelpers.findAndHookMethod(
                SQLiteDatabase::class.java, "insert",
                String::class.java, String::class.java, ContentValues::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val name = param.args[0] as String
                        val contentValues = param.args[2] as? ContentValues
                        if (name == "ad" && contentValues != null) {
                            contentValues.clear()
                            log("clear insertAdDatabase success")
                        }
                    }
                }
            )
        } catch (t: Throwable) {
            log("removeSplashAd fail: ${t.getStackInfo()}")
        }
    }

    private fun hookSplashFragment(classLoader: ClassLoader): Class<*>? {
        try {
            XposedHelpers.findAndHookMethod(
                SplashFragment, classLoader, "onCreateView",
                LayoutInflater::class.java, ViewGroup::class.java, Bundle::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val fragment = param.thisObject as Any
                        val splashHandler = findSplashHandler(fragment)
                        log("findSplashHandler: $splashHandler")
                        if (splashHandler != null) {
                            hookSendMessageDelayed(splashHandler)
                        }
                    }

                    private fun findSplashHandler(fragment: Any): Handler? {
                        try {
                            for (field in fragment.javaClass.declaredFields) {
                                if (field.type == Handler::class.java) {
                                    field.isAccessible = true
                                    return field.get(fragment) as? Handler
                                }
                            }
                        } catch (t: Throwable) {
                            log("findHandlerField fail: ${t.getStackInfo()}")
                        }
                        return null
                    }

                    private fun hookSendMessageDelayed(splashHandler: Handler) {
                        try {
                            XposedHelpers.findAndHookMethod(
                                Handler::class.java, "sendEmptyMessageDelayed",
                                Int::class.java, Long::class.java,
                                object : XC_MethodHook() {
                                    override fun beforeHookedMethod(param: MethodHookParam) {
                                        val handler = param.thisObject as Handler
                                        val delayMillis = param.args[1] as Long
                                        if (handler == splashHandler && delayMillis > 1000) {
                                            param.args[1] = 1000
                                            log("hookSendMessageDelayed success")
                                        }
                                    }
                                }
                            )
                        } catch (t: Throwable) {
                            log("hookSendMessageDelayed fail: ${t.getStackInfo()}")
                        }
                    }
                }
            )
        } catch (t: Throwable) {
            log("hookSplashFragment fail: ${t.getStackInfo()}")
        }
        return null
    }

}