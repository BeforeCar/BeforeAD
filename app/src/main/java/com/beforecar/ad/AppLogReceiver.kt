package com.beforecar.ad

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * @author: wangpan
 * @email: p.wang@aftership.com
 * @date: 2021/5/17
 *
 * 接收 app log 的广播接收者
 */
class AppLogReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        //日志会在 BeforeAdHookPolicy 中输出
    }

}