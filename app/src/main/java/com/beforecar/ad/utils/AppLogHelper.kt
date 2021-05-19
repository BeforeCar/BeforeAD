package com.beforecar.ad.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import de.robv.android.xposed.XposedBridge
import org.json.JSONException
import org.json.JSONObject

/**
 * @author: wangpan
 * @email: p.wang@aftership.com
 * @date: 2021/5/19
 */
object AppLogHelper {

    private const val ACTION_APP_LOG = "action_app_log_broadcast"
    private const val RECEIVER_PKG_NAME = "com.beforecar.ad"
    private const val RECEIVER_CLASS_NAME = "com.beforecar.ad.AppLogReceiver"

    private const val LOG_TAG = "tag"
    private const val LOG_CONTENT = "content"

    @JvmStatic
    fun sendLogBroadcast(context: Context, tag: String, content: Any?) {
        val jsonString = createJsonString(tag, content.toString())
        if (jsonString.isEmpty()) return
        val intent = Intent(ACTION_APP_LOG).apply {
            component = ComponentName(RECEIVER_PKG_NAME, RECEIVER_CLASS_NAME)
            data = Uri.parse(jsonString)
        }
        context.sendBroadcast(intent)
    }

    private fun createJsonString(tag: String, content: String): String {
        try {
            val jsonLog = JSONObject()
            jsonLog.put(LOG_TAG, tag)
            jsonLog.put(LOG_CONTENT, content)
            return jsonLog.toString()
        } catch (t: JSONException) {
            t.printStackTrace()
        }
        return ""
    }

    @JvmStatic
    fun parseLogAndPrint(intent: Intent) {
        try {
            val logString = intent.data?.toString() ?: ""
            if (logString.isEmpty()) return
            val jsonLog = JSONObject(logString)
            val tag = jsonLog.optString(LOG_TAG)
            val content = jsonLog.optString(LOG_CONTENT)
            XposedBridge.log("$tag: $content")
        } catch (t: JSONException) {
            t.printStackTrace()
        }
    }

}