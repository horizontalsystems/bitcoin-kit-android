package io.horizontalsystems.bitcoinkit.managers

import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonArray
import com.eclipsesource.json.JsonObject
import com.eclipsesource.json.JsonValue
import java.io.BufferedOutputStream
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.logging.Logger

class ApiManager(private val host: String) {
    private val logger = Logger.getLogger("ApiManager")

    fun getTransactions(addresses: List<String>): List<BCoinTransactionResponse> {
        val requestData = JsonObject().apply {
            this["addresses"] = Json.array(*addresses.toTypedArray())
        }

        logger.info("Request transactions for ${addresses.size} addresses: [${addresses.first()}, ...]")

        val response = post("$host/tx/address", requestData.toString()).asArray()

        logger.info("Got ${response.size()} transactions for requested addresses")

        val transactions = mutableListOf<BCoinTransactionResponse>()

        for (txItem in response) {
            val tx = txItem.asObject()

            val outputs = mutableListOf<BCoinTransactionOutput>()
            for (outputItem in tx["outputs"].asArray()) {
                val outputJson = outputItem.asObject()

                val scriptJson = outputJson["script"] ?: continue
                val addressJson = outputJson["address"] ?: continue

                outputs.add(BCoinTransactionOutput(scriptJson.asString(), addressJson.asString()))
            }

            transactions.add(BCoinTransactionResponse(tx["block"].asString(), tx["height"].asInt(), outputs))
        }

        return transactions
    }

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


    private fun post(resource: String, data: String): JsonValue {
        val url = URL(resource)
        val urlConnection = url.openConnection() as HttpURLConnection
        urlConnection.requestMethod = "POST"
        val out = BufferedOutputStream(urlConnection.outputStream)

        val writer = BufferedWriter(OutputStreamWriter(out, "UTF-8"))
        writer.write(data)
        writer.flush()
        writer.close()
        out.close()

        return urlConnection.inputStream.use {
            Json.parse(it.bufferedReader())
        }
    }
}

data class BCoinTransactionResponse(val blockHash: String, val blockHeight: Int, val txOutputs: List<BCoinTransactionOutput>)

data class BCoinTransactionOutput(val script: String, val address: String)

data class BlockResponse(val hash: String, val height: Int) {
    override fun equals(other: Any?): Boolean {
        return other is BlockResponse && other.height == this.height
    }

    override fun hashCode(): Int {
        return 31 * hash.hashCode() + height
    }
}
