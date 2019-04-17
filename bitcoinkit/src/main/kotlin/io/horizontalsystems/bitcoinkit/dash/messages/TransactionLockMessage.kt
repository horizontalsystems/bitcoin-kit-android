package io.horizontalsystems.bitcoinkit.dash.messages

import io.horizontalsystems.bitcoinkit.io.BitcoinInput
import io.horizontalsystems.bitcoinkit.network.messages.IMessageParser
import io.horizontalsystems.bitcoinkit.network.messages.IMessage
import io.horizontalsystems.bitcoinkit.serializers.TransactionSerializer
import io.horizontalsystems.bitcoinkit.storage.FullTransaction
import java.io.ByteArrayInputStream

class TransactionLockMessage(var transaction: FullTransaction) : IMessage {
    override val command: String = "ix"

    override fun toString(): String {
        return "TransactionLockMessage(${transaction.header.hashHexReversed})"
    }
}

class TransactionLockMessageParser : IMessageParser {
    override val command: String = "ix"

    override fun parseMessage(payload: ByteArray): IMessage {
        BitcoinInput(ByteArrayInputStream(payload)).use { input ->
            val transaction = TransactionSerializer.deserialize(input)
            return TransactionLockMessage(transaction)
        }
    }
}
