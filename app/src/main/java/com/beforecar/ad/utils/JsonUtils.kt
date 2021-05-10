package com.beforecar.ad.utils

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * @author: wangpan
 * @email: p.wang@aftership.com
 * @date: 2021/5/11
 */
object JsonUtils {

    /**
     * 循环移除 JSONArray 中满足条件的元素
     */
    @Throws(JSONException::class)
    fun removeJSONArrayElements(jsonArray: JSONArray, condition: (JSONObject) -> Boolean) {
        var index = 0
        var maxIndex = jsonArray.length()
        while (index < maxIndex) {
            if (condition(jsonArray.getJSONObject(index))) {
                jsonArray.remove(index)
                --maxIndex
            } else {
                ++index
            }
        }
    }

}