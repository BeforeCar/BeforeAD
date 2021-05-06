package com.beforecar.ad.init

import com.beforecar.ad.policy.*
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
        appPolicies.add(XiaoMiMarketHookPolicy())
        appPolicies.add(XiaoMiShopHookPolicy())
        appPolicies.add(MiJiaHookPolicy())
        appPolicies.add(MobileTicketHookPolicy())
        appPolicies.add(WeiXinHookPolicy())
        appPolicies.add(WeiBoHookPolicy)
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        appPolicies.forEach {
            if (lpparam.packageName == it.getPackageName()) {
                it.handleLoadPackage(lpparam)
            }
        }
    }
}