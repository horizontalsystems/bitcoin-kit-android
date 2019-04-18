package io.horizontalsystems.bitcoincore.network.messages

import io.horizontalsystems.bitcoincore.io.BitcoinOutput
import io.horizontalsystems.bitcoincore.utils.HashUtils

class GetBlocksMessage(var hashes: List<ByteArray>, var version: Int, var hashStop: ByteArray) : IMessage {
    override val command: String = "getblocks"

    override fun toString(): String {
        val list = hashes
                .take(10)
                .map { hash -> HashUtils.toHexStringAsLE(hash) }
                .joinToString()

        return ("GetBlocksMessage(" + hashes.size + ": [" + list + "], hashStop=" + HashUtils.toHexStringAsLE(hashStop) + ")")
    }
}

class GetBlocksMessageSerializer : IMessageSerializer {
    override val command: String = "getblocks"

    override fun serialize(message: IMessage): ByteArray {
        if (message !is GetBlocksMessage) throw WrongSerializer()

        val output = BitcoinOutput()
        output.writeInt(message.version).writeVarInt(message.hashes.size.toLong())
        message.hashes.forEach {
            output.write(it)
        }
        output.write(message.hashStop)
        return output.toByteArray()
    }
}