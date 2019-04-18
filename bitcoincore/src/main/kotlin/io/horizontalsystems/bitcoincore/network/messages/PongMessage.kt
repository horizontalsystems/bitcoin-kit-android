package io.horizontalsystems.bitcoincore.network.messages

import io.horizontalsystems.bitcoincore.io.BitcoinInput
import io.horizontalsystems.bitcoincore.io.BitcoinOutput
import java.io.ByteArrayInputStream

class PongMessage(val nonce: Long) : IMessage {
    override val command: String = "pong"

    override fun toString(): String {
        return "PongMessage(nonce=$nonce)"
    }
}

class PongMessageParser : IMessageParser {
    override val command: String = "pong"

    override fun parseMessage(payload: ByteArray): IMessage {
        BitcoinInput(ByteArrayInputStream(payload)).use { input ->
            val nonce = input.readLong()
            return PongMessage(nonce)
        }
    }
}

class PongMessageSerializer : IMessageSerializer {
    override val command: String = "pong"

    override fun serialize(message: IMessage): ByteArray {
        if (message !is PongMessage) throw WrongSerializer()

        return BitcoinOutput()
                .writeLong(message.nonce)
                .toByteArray()
    }
}
