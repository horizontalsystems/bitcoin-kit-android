package io.horizontalsystems.bitcoincore.network.messages

class VerAckMessage : IMessage {
    override fun toString(): String {
        return "VerAckMessage()"
    }
}

class VerAckMessageParser : IMessageParser {
    override val command: String = "verack"

    override fun parseMessage(payload: ByteArray): IMessage {
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
