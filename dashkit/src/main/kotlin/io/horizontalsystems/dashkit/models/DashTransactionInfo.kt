package io.horizontalsystems.dashkit.models

import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonObject
import io.horizontalsystems.bitcoincore.models.*

class DashTransactionInfo : TransactionInfo {

    var txlock: Boolean = false
    var instantTx: Boolean = false

    constructor(uid: String,
                transactionHash: String,
                transactionIndex: Int,
                inputs: List<TransactionInputInfo>,
                outputs: List<TransactionOutputInfo>,
                amount: Long,
                type: TransactionType,
                fee: Long?,
                blockHeight: Int?,
                timestamp: Long,
                status: TransactionStatus,
                conflictingTxHash: String?,
                instantTx: Boolean,
                txlock: Boolean,
    ) : super(uid, transactionHash, transactionIndex, inputs, outputs, amount, type, fee, blockHeight, timestamp, status, conflictingTxHash) {
        this.instantTx = instantTx
        this.txlock = txlock
    }

    @Throws
    constructor(serialized: String) : super(serialized) {
        val jsonObject = Json.parse(serialized).asObject()
        this.instantTx = jsonObject["instantTx"].asBoolean()
        this.txlock = jsonObject["txlock"].asBoolean()
    }

    override fun asJsonObject(): JsonObject {
        val jsonObject = super.asJsonObject()
        jsonObject["instantTx"] = instantTx
        jsonObject["txlock"] = txlock
        return jsonObject
    }

}
