package com.beforecar.ad.utils

import android.app.ActivityManager
import android.content.Context
import android.os.Process

/**
 * @author: wangpan
 * @emial: p.wang@aftership.com
 * @date: 2021/5/6
 */
object AppUtils {

    fun getProcessName(context: Context): String {
        val pid = Process.myPid()
        return (context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)?.let { am ->
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

}