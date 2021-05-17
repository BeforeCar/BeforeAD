package com.beforecar.ad

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import org.json.JSONException
import org.json.JSONObject

/**
 * @author: wangpan
 * @email: p.wang@aftership.com
 * @date: 2021/5/17
 *
 * 接收 app log 的广播接收者
 */
class AppLogReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val content = intent.data?.toString() ?: ""
        try {
            val jsonLog = JSONObject(content)
            Log.i(jsonLog.optString("tag"), jsonLog.optString("content"))
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

}