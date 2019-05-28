package io.horizontalsystems.bitcoincore.network.messages

class MempoolMessage : IMessage {
    override fun toString(): String {
        return "MempoolMessage()"
    }
}

class MempoolMessageSerializer : IMessageSerializer {
    override val command: String = "mempool"

    override fun serialize(message: IMessage): ByteArray? {
        if (message !is MempoolMessage) {
            return null
        }

        return ByteArray(0)
    }
}
