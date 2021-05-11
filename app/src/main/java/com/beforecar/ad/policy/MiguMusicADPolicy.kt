package com.beforecar.ad.policy

import android.app.Activity
import android.app.Application
import com.beforecar.ad.policy.base.AbsHookPolicy
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * 咪咕音乐
 *
 * Author: minminaya  承接东风各式弹头打磨、抛光、刷漆等4S保养工程。
 * Email: minminaya@gmail.com
 * Date:
 *
 */
class MiguMusicADPolicy : AbsHookPolicy() {

    override val tag: String
        get() = "MiguMusicADPolicy"

    override fun onMainFirstValidActivityPreOnCreate(
        lpparam: XC_LoadPackage.LoadPackageParam,
        application: Application,
        activity: Activity,
        classLoader: ClassLoader
    ) {
        super.onMainFirstValidActivityPreOnCreate(lpparam, application, activity, classLoader)
        XposedHelpers.findAndHookMethod(
            "com.migu.music.module.api.HttpApiManager",
            classLoader,
            "getDefaultMapHeaders",
            object : XC_MethodHook() {

                override fun afterHookedMethod(param: MethodHookParam?) {
                    super.afterHookedMethod(param)
                    log("afterHookedMethod: " + param?.result)
                }
            }
        )
        //(com.migu.music.entity.Song, boolean, com.migu.music.entity.SongFormat)
        val song = XposedHelpers.findClassIfExists("com.migu.music.entity.Song", classLoader)
        val listenUrlUtils =
            XposedHelpers.findClassIfExists("com.migu.music.control.ListenUrlUtils", classLoader)
        val songFormat =
            XposedHelpers.findClassIfExists("com.migu.music.entity.SongFormat", classLoader)
        log("song:$song")
        log("songFormat:$songFormat")
        log("listenUrlUtils:$listenUrlUtils")
        XposedHelpers.findAndHookMethod(
            "com.migu.music.control.ListenUrlUtils",
            classLoader,
            "getRequestListenParam",
            song,
            Boolean::class.java,
            songFormat,
            object : XC_MethodHook() {

                override fun afterHookedMethod(param: MethodHookParam?) {
                    super.afterHookedMethod(param)
                    log("afterHookedMethod: " + param?.result)
                }
            }
        )
    }

    override fun getPackageName(): String {
        return "cmccwm.mobilemusic"
    }

}