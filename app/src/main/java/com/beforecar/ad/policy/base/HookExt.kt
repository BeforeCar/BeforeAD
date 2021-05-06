package com.beforecar.ad.policy.base

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageInfo
import android.os.Process

fun Throwable.getStackInfo(): String {
    val sb = StringBuilder(this.toString())
    for (s in this.stackTrace) {
        sb.append("\n").append(s.toString())
    }
    return sb.toString()
}

fun Context.getPackageInfo(): PackageInfo? {
    try {
        return this.packageManager.getPackageInfo(this.packageName, 0)
    } catch (e: Exception) {
        //no op
    }
    return null
}

fun Context.getVersionName(): String {
    return this.getPackageInfo()?.versionName ?: ""
}

fun Context.getVersionCode(): Int {
    return this.getPackageInfo()?.versionCode ?: -1
}

fun Context.getProcessName(): String {
    val pid = Process.myPid()
    return (this.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)?.let { am ->
        val processes = am.runningAppProcesses ?: emptyList()
        var result = ""
        for (process in processes) {
            if (process.pid == pid) {
                result = process.processName
                break
            }
        }
        result
    } ?: ""
}