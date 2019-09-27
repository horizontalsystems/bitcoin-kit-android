package io.horizontalsystems.groestlcoinkit.network.messages

import io.horizontalsystems.bitcoincore.network.messages.IMessage
import io.horizontalsystems.bitcoincore.network.messages.IMessageParser
import io.horizontalsystems.bitcoincore.network.messages.IMessageSerializer
import io.horizontalsystems.bitcoincore.network.messages.TransactionMessage


import io.horizontalsystems.bitcoincore.io.BitcoinInput
import io.horizontalsystems.groestlcoinkit.serializers.GroestlcoinTransactionSerializer
import java.io.ByteArrayInputStream

class GroestlcoinTransactionMessageParser : IMessageParser {
    override val command: String = "tx"

    override fun parseMessage(payload: ByteArray): IMessage {
        BitcoinInput(ByteArrayInputStream(payload)).use { input ->
            val transaction = GroestlcoinTransactionSerializer.deserialize(input)
            return TransactionMessage(transaction, payload.size)
        }
    }
}

class GroestlcoinTransactionMessageSerializer : IMessageSerializer {
    override val command: String = "tx"

    override fun serialize(message: IMessage): ByteArray? {
        if (message !is TransactionMessage) {
            return null
        }

        return GroestlcoinTransactionSerializer.serialize(message.transaction)
    }
}