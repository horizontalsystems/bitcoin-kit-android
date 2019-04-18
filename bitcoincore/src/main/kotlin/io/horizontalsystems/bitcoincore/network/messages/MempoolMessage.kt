package io.horizontalsystems.bitcoincore.network.messages

class MempoolMessage : IMessage {
    override val command: String = "mempool"

    override fun toString(): String {
        return "MempoolMessage()"
    }
}

class MempoolMessageSerializer : IMessageSerializer {
    override val command: String = "mempool"

    override fun serialize(message: IMessage): ByteArray {
        return ByteArray(0)
    }
}
