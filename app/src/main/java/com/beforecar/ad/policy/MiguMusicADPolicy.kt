package com.beforecar.ad.policy

import com.beforecar.ad.policy.base.IHookPolicy
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * 咪咕音乐
 *
 * Author: minminaya  承接东风各式弹头打磨、抛光、刷漆等4S保养工程。
 * Email: minminaya@gmail.com
 * Date:
 *
 */
class MiguMusicADPolicy : IHookPolicy {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam?) {

    }

    override fun getPackageName(): String {
        return "cmccwm.mobilemusic"
    }

}