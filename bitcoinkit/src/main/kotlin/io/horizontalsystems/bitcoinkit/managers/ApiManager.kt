package io.horizontalsystems.bitcoinkit.managers

import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonArray
import com.eclipsesource.json.JsonObject
import com.eclipsesource.json.JsonValue
import java.net.URL
import java.util.logging.Logger

class ApiManager(private val host: String) {
    private val logger = Logger.getLogger("ApiManager")

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

        logger.info("Fetching $resource")

        return URL(resource)
                .openConnection()
                .apply {
                    connectTimeout = 5000
                    readTimeout = 10000
                    setRequestProperty("Accept", "application/json")
                }.getInputStream()
                .use {
                    Json.parse(it.bufferedReader())
                }
    }
}

data class BlockResponse(val hash: String, val height: Int) {
    override fun equals(other: Any?): Boolean {
        return other is BlockResponse && other.height == this.height
    }

    override fun hashCode(): Int {
        return 31 * hash.hashCode() + height
    }
}
