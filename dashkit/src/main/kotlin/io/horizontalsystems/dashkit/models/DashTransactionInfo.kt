package io.horizontalsystems.dashkit.models

import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonObject
import io.horizontalsystems.bitcoincore.models.TransactionAddress
import io.horizontalsystems.bitcoincore.models.TransactionInfo
import io.horizontalsystems.bitcoincore.models.TransactionStatus

class DashTransactionInfo : TransactionInfo {

    var instantTx: Boolean = false

    constructor(transactionHash: String,
                transactionIndex: Int,
                from: List<TransactionAddress>,
                to: List<TransactionAddress>,
                amount: Long,
                fee: Long?,
                blockHeight: Int?,
                timestamp: Long,
                status: TransactionStatus,
                instantTx: Boolean
    ) : super(transactionHash, transactionIndex, from, to, amount, fee, blockHeight, timestamp, status) {
        this.instantTx = instantTx
    }

    @Throws
    constructor(serialized: String) : super(serialized) {
        val jsonObject = Json.parse(serialized).asObject()
        this.instantTx = jsonObject["instantTx"].asBoolean()
    }

    override fun asJsonObject(): JsonObject {
        val jsonObject = super.asJsonObject()
        jsonObject["instantTx"] = instantTx
        return jsonObject
    }

}
