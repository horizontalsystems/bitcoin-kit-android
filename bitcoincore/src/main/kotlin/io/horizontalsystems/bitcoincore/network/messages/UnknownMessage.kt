package io.horizontalsystems.bitcoincore.network.messages

class UnknownMessage(override val command: String, private val payload: ByteArray) : IMessage {
    override fun toString(): String {
        return "UnknownMessage(command=$command)"
    }
}
