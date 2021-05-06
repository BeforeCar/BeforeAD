package com.beforecar.ad.init

import com.beforecar.ad.policy.MiguMusicADPolicy
import com.beforecar.ad.policy.base.IHookPolicy
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 *
 * Hook 模块初始化入口
 * Author: minminaya  承接东风各式弹头打磨、抛光、刷漆等4S保养工程。
 * Email: minminaya@gmail.com
 * Date:
 *
 */
class BeforeADInit : IXposedHookLoadPackage {

    private val appPolicies = mutableListOf<IHookPolicy>()

    init {
        appPolicies.add(MiguMusicADPolicy())
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam?) {
        lpparam?.run {
            appPolicies.forEach {
                if (this.packageName == it.getPackageName()) {
                    it.handleLoadPackage(lpparam)
                }
            }
        }
    }
}