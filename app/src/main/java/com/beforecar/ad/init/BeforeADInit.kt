package com.beforecar.ad.init

import com.beforecar.ad.policy.*
import com.beforecar.ad.policy.base.AbsHookPolicy
import com.beforecar.ad.policy.douyin.DouyinHookPolicy
import com.beforecar.ad.policy.hlgys.HlgysHookPolicy
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

    private val appPolicies = mutableListOf<AbsHookPolicy>()

    init {
        appPolicies.add(BeforeAdHookPolicy())
        appPolicies.add(MiguMusicADPolicy())
        appPolicies.add(MiAppMarketHookPolicy())
        appPolicies.add(MiShopHookPolicy())
        appPolicies.add(MiJiaHookPolicy())
        appPolicies.add(MobileTicketHookPolicy())
        appPolicies.add(WeiXinHookPolicy())
        appPolicies.add(WeiBoHookPolicy)
        appPolicies.add(MIUIMSAHookPolicy())
        appPolicies.add(BaiduHookPolicy())
        appPolicies.add(TouTiaoHookPolicy())
        appPolicies.add(JDHookPolicy)
        appPolicies.add(XunLeiHookPolicy())
        appPolicies.add(PiPiXiaHookPolicy())
        appPolicies.add(OPPOStoreHookPolicy())
        appPolicies.add(HeytapMarketHookPolicy())
        appPolicies.add(HeytapThemeHookPolicy())
        appPolicies.add(CoolApkHookPolicy())
        appPolicies.add(IdleFishHookPolicy())
        appPolicies.add(MiPayWalletHookPolicy())
        appPolicies.add(BilibiliHookPolicy())
        appPolicies.add(MiUpdaterHookPolicy())
        appPolicies.add(MIUISecurityCenterHookPolicy)
        appPolicies.add(BiqugeHookPolicy())
        appPolicies.add(FengCheCartoonHookPolicy())
        appPolicies.add(DrmfabHookPolicy())
        appPolicies.add(HlgysHookPolicy())
        appPolicies.add(DouyinHookPolicy())
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        appPolicies.forEach {
            if (lpparam.packageName == it.getPackageName()) {
                it.handleLoadPackage(lpparam)
            }
        }
    }
}