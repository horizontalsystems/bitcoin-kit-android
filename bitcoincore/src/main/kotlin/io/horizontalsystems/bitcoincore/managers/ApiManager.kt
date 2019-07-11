package io.horizontalsystems.bitcoincore.managers

import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonArray
import com.eclipsesource.json.JsonObject
import com.eclipsesource.json.JsonValue
import io.horizontalsystems.bitcoincore.utils.HSLogger
import java.net.URL

class ApiManager(private val host: String) {
    private val logger = HSLogger("ApiManager")

    @Throws
    fun getJson(file: String): JsonObject {
        return getJsonValue(file).asObject()
    }

    @Throws(Exception::class)
    fun getJsonArray(file: String): JsonArray {
        return getJsonValue(file).asArray()
    }

    private fun getJsonValue(file: String): JsonValue {
        val resource = "$host/$file"

        logger.i("Fetching %s", resource)

        return URL(resource)
                .openConnection()
                .apply {
                    connectTimeout = 5000
                    readTimeout = 60000
                    setRequestProperty("Accept", "application/json")
                }.getInputStream()
                .use {
                    Json.parse(it.bufferedReader())
                }
    }

}
