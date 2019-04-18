package io.horizontalsystems.bitcoincore.network.messages

import io.horizontalsystems.bitcoincore.io.BitcoinInput
import io.horizontalsystems.bitcoincore.io.BitcoinOutput
import java.io.ByteArrayInputStream

class PingMessage(val nonce: Long) : IMessage {
    override val command: String = "ping"

    override fun toString(): String {
        return "PingMessage(nonce=$nonce)"
    }
}

class PingMessageParser : IMessageParser {
    override val command: String = "ping"

    override fun parseMessage(payload: ByteArray): IMessage {
        BitcoinInput(ByteArrayInputStream(payload)).use { input ->
            val nonce = input.readLong()
            return PingMessage(nonce)
        }
    }
}

class PingMessageSerializer : IMessageSerializer {
    override val command: String = "ping"

    override fun serialize(message: IMessage): ByteArray {
        if (message !is PingMessage) throw WrongSerializer()

        return BitcoinOutput()
                .writeLong(message.nonce)
                .toByteArray()
    }
}
