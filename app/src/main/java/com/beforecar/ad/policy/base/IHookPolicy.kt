package com.beforecar.ad.policy.base

import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 *
 *
 * Author: minminaya  承接东风各式弹头打磨、抛光、刷漆等4S保养工程。
 * Email: minminaya@gmail.com
 * Date:
 *
 */
interface IHookPolicy {

    fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam?)
}