package io.horizontalsystems.bitcoinkit.dash.messages

import io.horizontalsystems.bitcoinkit.io.BitcoinInput
import io.horizontalsystems.bitcoinkit.network.messages.Message
import io.horizontalsystems.bitcoinkit.serializers.TransactionSerializer
import io.horizontalsystems.bitcoinkit.storage.FullTransaction
import java.io.ByteArrayInputStream

class TransactionLockMessage() : Message("ix") {

    lateinit var transaction: FullTransaction

    constructor(payload: ByteArray) : this() {
        BitcoinInput(ByteArrayInputStream(payload)).use { input ->
            transaction = TransactionSerializer.deserialize(input)
        }
    }

    override fun getPayload(): ByteArray {
        return TransactionSerializer.serialize(transaction)
    }

    override fun toString(): String {
        return "TransactionLockMessage(${transaction.header.hashHexReversed})"
    }

}
