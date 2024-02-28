package com.beforecar.ad.init

import com.beforecar.ad.policy.BaiduHookPolicy
import com.beforecar.ad.policy.BeforeAdHookPolicy
import com.beforecar.ad.policy.BilibiliHookPolicy
import com.beforecar.ad.policy.BiqugeHookPolicy
import com.beforecar.ad.policy.CoolApkHookPolicy
import com.beforecar.ad.policy.DrmfabHookPolicy
import com.beforecar.ad.policy.FengCheCartoonHookPolicy
import com.beforecar.ad.policy.HeytapMarketHookPolicy
import com.beforecar.ad.policy.HeytapThemeHookPolicy
import com.beforecar.ad.policy.IdleFishHookPolicy
import com.beforecar.ad.policy.Insta360HookPolicy
import com.beforecar.ad.policy.JDHookPolicy
import com.beforecar.ad.policy.MIUIMSAHookPolicy
import com.beforecar.ad.policy.MIUISecurityCenterHookPolicy
import com.beforecar.ad.policy.MiAppMarketHookPolicy
import com.beforecar.ad.policy.MiJiaHookPolicy
import com.beforecar.ad.policy.MiPayWalletHookPolicy
import com.beforecar.ad.policy.MiShopHookPolicy
import com.beforecar.ad.policy.MiUpdaterHookPolicy
import com.beforecar.ad.policy.MiguMusicADPolicy
import com.beforecar.ad.policy.MobileTicketHookPolicy
import com.beforecar.ad.policy.OPPOStoreHookPolicy
import com.beforecar.ad.policy.PiPiXiaHookPolicy
import com.beforecar.ad.policy.TouTiaoHookPolicy
import com.beforecar.ad.policy.WeiBoHookPolicy
import com.beforecar.ad.policy.WeiXinHookPolicy
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
        appPolicies.add(Insta360HookPolicy())
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        appPolicies.forEach {
            if (lpparam.packageName == it.getPackageName()) {
                it.handleLoadPackage(lpparam)
            }
        }
    }
}