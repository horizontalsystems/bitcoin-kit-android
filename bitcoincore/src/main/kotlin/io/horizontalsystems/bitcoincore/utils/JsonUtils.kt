package io.horizontalsystems.bitcoincore.utils

import org.json.JSONObject
import java.math.BigDecimal

object JsonUtils {

    fun objToString(obj: LineLock): String {
        val json = JSONObject()
        json.putOpt("lastHeight", obj.lastHeight)
        json.putOpt("lockedValue", obj.lockedValue)
        json.putOpt("startMonth", obj.startMonth)
        json.putOpt("intervalMonth", obj.intervalMonth)
        json.putOpt("outputSize", obj.outputSize)
        return json.toString()
    }

    fun stringToObj(str: String): LineLock {
        val json = JSONObject(str)
        val lastHeight = json.optLong("lastHeight")
        val lockedValue = json.optString("lockedValue")
        val startMonth = json.optInt("startMonth")
        val intervalMonth = json.optInt("intervalMonth")
        val outputSize = json.optInt("outputSize")
        return LineLock(lastHeight, lockedValue, startMonth, intervalMonth, outputSize)
    }

    class LineLock(
        var lastHeight: Long,
        var lockedValue: String,
        val startMonth: Int,
        val intervalMonth: Int,
        val outputSize: Int
    ) {
        override fun toString(): String {
            return "LineLock(lastHeight=$lastHeight, lockedValue='$lockedValue', startMonth=$startMonth, intervalMonth=$intervalMonth, outputSize=$outputSize)"
        }
    }

}
