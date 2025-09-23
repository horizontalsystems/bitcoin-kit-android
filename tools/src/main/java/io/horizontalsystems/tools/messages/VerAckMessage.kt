package io.horizontalsystems.tools.messages

import io.horizontalsystems.tools.io.BitcoinInputMarkable


class VerAckMessage : IMessage {
    override fun toString(): String {
        return "VerAckMessage()"
    }
}

class VerAckMessageParser : IMessageParser {
    override val command: String = "verack"

    override fun parseMessage(input: BitcoinInputMarkable): IMessage {
        return VerAckMessage()
    }
}

class VerAckMessageSerializer : IMessageSerializer {
    override val command: String = "verack"

    override fun serialize(message: IMessage): ByteArray? {
        if (message !is VerAckMessage) {
            return null
        }

        return ByteArray(0)
    }
}
