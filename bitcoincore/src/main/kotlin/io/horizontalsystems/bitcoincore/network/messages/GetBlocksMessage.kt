package io.horizontalsystems.bitcoincore.network.messages

import io.horizontalsystems.bitcoincore.extensions.toReversedHex
import io.horizontalsystems.bitcoincore.io.BitcoinOutput

class GetBlocksMessage(var hashes: List<ByteArray>, var version: Int, var hashStop: ByteArray) : IMessage {
    override fun toString(): String {
        val list = hashes
                .take(10)
                .map { hash -> hash.toReversedHex() }
                .joinToString()

        return ("GetBlocksMessage(" + hashes.size + ": [" + list + "], hashStop=" + hashStop.toReversedHex() + ")")
    }
}

class GetBlocksMessageSerializer : IMessageSerializer {
    override val command: String = "getblocks"

    override fun serialize(message: IMessage): ByteArray? {
        if (message !is GetBlocksMessage) {
            return null
        }

        val output = BitcoinOutput()
        output.writeInt(message.version).writeVarInt(message.hashes.size.toLong())
        message.hashes.forEach {
            output.write(it)
        }
        output.write(message.hashStop)
        return output.toByteArray()
    }
}
