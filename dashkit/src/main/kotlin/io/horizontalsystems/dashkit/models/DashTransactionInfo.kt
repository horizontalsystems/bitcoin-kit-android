package io.horizontalsystems.dashkit.models

import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonObject
import io.horizontalsystems.bitcoincore.models.TransactionInfo
import io.horizontalsystems.bitcoincore.models.TransactionInputInfo
import io.horizontalsystems.bitcoincore.models.TransactionOutputInfo
import io.horizontalsystems.bitcoincore.models.TransactionStatus

class DashTransactionInfo : TransactionInfo {

    var instantTx: Boolean = false

    constructor(uid: String,
                transactionHash: String,
                transactionIndex: Int,
                inputs: List<TransactionInputInfo>,
                outputs: List<TransactionOutputInfo>,
                fee: Long?,
                blockHeight: Int?,
                timestamp: Long,
                status: TransactionStatus,
                conflictingTxHash: String?,
                instantTx: Boolean
    ) : super(uid, transactionHash, transactionIndex, inputs, outputs, fee, blockHeight, timestamp, status, conflictingTxHash) {
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
