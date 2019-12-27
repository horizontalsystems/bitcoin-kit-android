package io.horizontalsystems.bitcoincore.models

import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonArray
import com.eclipsesource.json.JsonObject
import io.horizontalsystems.bitcoincore.core.IPluginOutputData

open class TransactionInfo {
    var uid: String = ""
    var transactionHash: String = ""
    var transactionIndex: Int = 0
    var inputs: List<TransactionInputInfo> = listOf()
    var outputs: List<TransactionOutputInfo> = listOf()
    var fee: Long? = null
    var blockHeight: Int? = null
    var timestamp: Long = 0
    var status: TransactionStatus = TransactionStatus.NEW
    var conflictingTxHash: String? = null

    constructor(uid: String,
                transactionHash: String,
                transactionIndex: Int,
                inputs: List<TransactionInputInfo>,
                outputs: List<TransactionOutputInfo>,
                fee: Long?,
                blockHeight: Int?,
                timestamp: Long,
                status: TransactionStatus,
                conflictingTxHash: String? = null) {
        this.uid = uid
        this.transactionHash = transactionHash
        this.transactionIndex = transactionIndex
        this.inputs = inputs
        this.outputs = outputs
        this.fee = fee
        this.blockHeight = blockHeight
        this.timestamp = timestamp
        this.status = status
        this.conflictingTxHash = conflictingTxHash
    }

    @Throws
    constructor(serialized: String) {
        val jsonObject = Json.parse(serialized).asObject()
        uid = jsonObject.get("uid").asString()
        transactionHash = jsonObject.get("transactionHash").asString()
        transactionIndex = jsonObject.get("transactionIndex").asInt()
        inputs = parseInputs(jsonObject.get("inputs").asArray())
        outputs = parseOutputs(jsonObject.get("outputs").asArray())
        fee = jsonObject.get("fee")?.asLong()
        blockHeight = jsonObject.get("blockHeight")?.asInt()
        timestamp = jsonObject.get("timestamp").asLong()
        status = TransactionStatus.getByCode(jsonObject.get("status").asInt())
                ?: TransactionStatus.INVALID
        conflictingTxHash = jsonObject.get("conflictingTxHash")?.asString()
    }

    private fun parseInputs(jsonArray: JsonArray): List<TransactionInputInfo> {
        val inputs = mutableListOf<TransactionInputInfo>()

        for (inputJsonValue in jsonArray) {
            inputJsonValue.asObject().let {
                val input = TransactionInputInfo(
                        mine = it.get("mine").asBoolean(),
                        value = if (it.get("value")?.isNull == false) it.get("value")?.asLong() else null,
                        address = if (it.get("address")?.isNull == false) it.get("address")?.asString() else null)

                inputs.add(input)
            }
        }
        return inputs
    }

    private fun parseOutputs(jsonArray: JsonArray): List<TransactionOutputInfo> {
        val outputs = mutableListOf<TransactionOutputInfo>()

        for (outputJsonValue in jsonArray) {
            outputJsonValue.asObject().let {
                val output = TransactionOutputInfo(
                        mine = it.get("mine").asBoolean(),
                        changeOutput = it.get("changeOutput").asBoolean(),
                        value = it.get("value").asLong(),
                        address = if (it.get("address")?.isNull == false) it.get("address")?.asString() else null,
                        pluginId = if (it.get("pluginId")?.isNull == false) it.get("pluginId")?.asString()?.toByte() else null,
                        pluginDataString = if (it.get("pluginDataString")?.isNull == false) it.get("pluginDataString")?.asString() else null)

                outputs.add(output)
            }
        }
        return outputs
    }

    private fun outputsToJson(outputs: List<TransactionOutputInfo>): JsonArray {
        val jsonArray = JsonArray()
        outputs.forEach {
            val outputObj = JsonObject()
            outputObj.add("mine", it.mine)
            outputObj.add("changeOutput", it.changeOutput)
            outputObj.add("value", it.value)
            outputObj.add("address", it.address)
            outputObj.add("pluginId", it.pluginId?.toString())
            outputObj.add("pluginDataString", it.pluginDataString)
            jsonArray.add(outputObj)
        }
        return jsonArray
    }

    private fun inputsToJson(inputs: List<TransactionInputInfo>): JsonArray {
        val jsonArray = JsonArray()
        inputs.forEach { input ->
            val inputObj = JsonObject()
            inputObj.add("mine", input.mine)
            input.value?.let { inputObj.add("value", it) }
            inputObj.add("address", input.address)
            jsonArray.add(inputObj)
        }
        return jsonArray
    }

    protected open fun asJsonObject(): JsonObject {
        val jsonObject = JsonObject()

        jsonObject.add("uid", uid)
        jsonObject.add("transactionHash", transactionHash)
        jsonObject.add("transactionIndex", transactionIndex)
        jsonObject.add("inputs", inputsToJson(inputs))
        jsonObject.add("outputs", outputsToJson(outputs))
        fee?.let { jsonObject.add("fee", it) }
        blockHeight?.let { jsonObject.add("blockHeight", it) }
        jsonObject.add("timestamp", timestamp)
        jsonObject.add("status", status.code)
        conflictingTxHash?.let { jsonObject.add("conflictingTxHash", it) }

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

data class TransactionInputInfo(val mine: Boolean, val value: Long? = null, val address: String? = null)

data class TransactionOutputInfo(val mine: Boolean,
                                 val changeOutput: Boolean,
                                 val value: Long,
                                 val address: String? = null,
                                 val pluginId: Byte? = null,
                                 val pluginData: IPluginOutputData? = null,
                                 internal val pluginDataString: String? = null)

data class BlockInfo(
        val headerHash: String,
        val height: Int,
        val timestamp: Long
)

data class BalanceInfo(val spendable: Long, val unspendable: Long)
