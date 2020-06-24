package io.horizontalsystems.bitcoincore.network.messages

import io.horizontalsystems.bitcoincore.io.BitcoinInputMarkable
import io.horizontalsystems.bitcoincore.io.BitcoinOutput

class PingMessage(val nonce: Long) : IMessage {
    override fun toString(): String {
        return "PingMessage(nonce=$nonce)"
    }
}

class PingMessageParser : IMessageParser {
    override val command: String = "ping"

    override fun parseMessage(input: BitcoinInputMarkable): IMessage {
        return PingMessage(input.readLong())
    }
}

class PingMessageSerializer : IMessageSerializer {
    override val command: String = "ping"

    override fun serialize(message: IMessage): ByteArray? {
        if (message !is PingMessage) {
            return null
        }

        return BitcoinOutput()
                .writeLong(message.nonce)
                .toByteArray()
    }
}
