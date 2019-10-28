package io.horizontalsystems.bitcoincore.network.messages

import io.horizontalsystems.bitcoincore.extensions.toReversedHex
import io.horizontalsystems.bitcoincore.io.BitcoinOutput

class GetHeadersMessage(var version: Int, var hashes: List<ByteArray>, var hashStop: ByteArray) : IMessage {
    override fun toString(): String {
        return ("GetHeadersMessage(${hashes.size}: hashStop=${hashStop.toReversedHex()})")
    }
}

class GetHeadersMessageSerializer : IMessageSerializer {
    override val command: String = "getheaders"

    override fun serialize(message: IMessage): ByteArray? {
        if (message !is GetHeadersMessage) {
            return null
        }

        val output = BitcoinOutput().also {
            it.writeInt(message.version)
            it.writeVarInt(message.hashes.size.toLong())
        }

        message.hashes.forEach {
            output.write(it)
        }

        output.write(message.hashStop)

        return output.toByteArray()
    }
}
