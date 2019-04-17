package io.horizontalsystems.bitcoinkit.network.messages

class VerAckMessage : IMessage {
    override val command: String = "verack"

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

    override fun serialize(message: IMessage): ByteArray {
        return ByteArray(0)
    }
}
