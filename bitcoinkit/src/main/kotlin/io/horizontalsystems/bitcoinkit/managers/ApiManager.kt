package io.horizontalsystems.bitcoinkit.managers

import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonObject
import java.net.URL

class ApiManager(private val host: String) {

    @Throws
    fun getJson(file: String): JsonObject {
        return URL("$host/$file").openConnection().apply {
            connectTimeout = 500
            readTimeout = 1000
            setRequestProperty("Accept", "application/json")
        }.getInputStream().use {
            Json.parse(it.bufferedReader()).asObject()
        }
    }

}

data class BlockResponse(val hash: String, val height: Int) {
    override fun equals(other: Any?): Boolean {
        return other is BlockResponse && other.height == this.height
    }

    override fun hashCode(): Int {
        return 31 * hash.hashCode() + height.hashCode()
    }
}
