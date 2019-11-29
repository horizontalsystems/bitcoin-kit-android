package io.horizontalsystems.bitcoincore.models

import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonArray
import com.eclipsesource.json.JsonObject
import io.horizontalsystems.bitcoincore.core.IPluginOutputData

open class TransactionInfo {
    var uid: String = ""
    var transactionHash: String = ""
    var transactionIndex: Int = 0
    var from: List<TransactionAddress> = listOf()
    var to: List<TransactionAddress> = listOf()
    var amount: Long = 0
    var fee: Long? = null
    var blockHeight: Int? = null
    var timestamp: Long = 0
    var status: TransactionStatus = TransactionStatus.NEW

    constructor(uid: String,
                transactionHash: String,
                transactionIndex: Int,
                from: List<TransactionAddress>,
                to: List<TransactionAddress>,
                amount: Long,
                fee: Long?,
                blockHeight: Int?,
                timestamp: Long,
                status: TransactionStatus) {
        this.uid = uid
        this.transactionHash = transactionHash
        this.transactionIndex = transactionIndex
        this.from = from
        this.to = to
        this.amount = amount
        this.fee = fee
        this.blockHeight = blockHeight
        this.timestamp = timestamp
        this.status = status
    }

    @Throws
    constructor(serialized: String) {
        val jsonObject = Json.parse(serialized).asObject()
        uid = jsonObject.getString("uid", "")
        transactionHash = jsonObject.getString("transactionHash", "")
        transactionIndex = jsonObject.getInt("transactionIndex", 0)
        this.from = parseTransactionAddresses(jsonObject.get("from").asArray())
        this.to = parseTransactionAddresses(jsonObject.get("to").asArray())
        amount = jsonObject.getLong("amount", 0)
        fee = jsonObject.get("fee")?.asLong()
        blockHeight = jsonObject.get("blockHeight")?.asInt()
        timestamp = jsonObject.getLong("timestamp", 0)
        val statusCode = jsonObject.getInt("status", Transaction.Status.INVALID)
        status = TransactionStatus.getByCode(statusCode) ?: TransactionStatus.INVALID
    }

    private fun parseTransactionAddresses(jsonArray: JsonArray): List<TransactionAddress> {
        val from = mutableListOf<TransactionAddress>()

        for (address in jsonArray) {
            val addressObj = address.asObject()
            val pluginId = if (addressObj.get("pluginId").isNull) null else addressObj.get("pluginId").asString().toByte()
            val pluginDataString = if (addressObj.get("pluginDataString").isNull) null else addressObj.get("pluginDataString").asString()

            val transactionAddress = TransactionAddress(
                    address = addressObj.getString("address", ""),
                    mine = addressObj.getBoolean("mine", false),
                    pluginId = pluginId,
                    pluginDataString = pluginDataString)

            from.add(transactionAddress)
        }

        return from
    }

    private fun transactionAddressesToJson(addresses: List<TransactionAddress>): JsonArray {
        val jsonArray = JsonArray()
        addresses.forEach {
            val address = JsonObject()
            address.add("address", it.address)
            address.add("mine", it.mine)
            address.add("pluginId", it.pluginId?.toString())
            address.add("pluginDataString", it.pluginDataString)
            jsonArray.add(address)
        }

        return jsonArray
    }

    protected open fun asJsonObject(): JsonObject {
        val jsonObject = JsonObject()

        jsonObject.add("uid", uid)
        jsonObject.add("transactionHash", transactionHash)
        jsonObject.add("transactionIndex", transactionIndex)
        jsonObject.add("from", transactionAddressesToJson(from))
        jsonObject.add("to", transactionAddressesToJson(to))
        jsonObject.add("amount", amount)
        fee?.let { jsonObject.add("fee", it) }
        blockHeight?.let { jsonObject.add("blockHeight", it) }
        jsonObject.add("timestamp", timestamp)
        jsonObject.add("status", status.code)

        return jsonObject
    }

    fun serialize(): String {
        return asJsonObject().toString()
    }

}

enum class TransactionStatus(val code: Int) {
    NEW(Transaction.Status.NEW),
    RELAYED(Transaction.Status.RELAYED),
    INVALID(Transaction.Status.INVALID);

    companion object {
        private val values = values()

        fun getByCode(code: Int): TransactionStatus? = values.firstOrNull { it.code == code }
    }
}

data class TransactionAddress(
        val address: String,
        val mine: Boolean,
        val pluginId: Byte? = null,
        val pluginData: IPluginOutputData? = null,
        internal val pluginDataString: String? = null
)

data class BlockInfo(
        val headerHash: String,
        val height: Int,
        val timestamp: Long
)

data class BalanceInfo(val spendable: Long, val unspendable: Long)
