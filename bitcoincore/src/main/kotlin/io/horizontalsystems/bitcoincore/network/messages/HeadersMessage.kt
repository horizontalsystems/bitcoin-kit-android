package io.horizontalsystems.bitcoincore.network.messages

import io.horizontalsystems.bitcoincore.core.IHasher
import io.horizontalsystems.bitcoincore.extensions.toReversedHex
import io.horizontalsystems.bitcoincore.io.BitcoinInputMarkable
import io.horizontalsystems.bitcoincore.io.BitcoinOutput
import io.horizontalsystems.bitcoincore.storage.BlockHeader

class HeadersMessage(val headers: Array<BlockHeader>) : IMessage {
    override fun toString(): String {
        return "HeadersMessage(${headers.size}:[${headers.joinToString { it.hash.toReversedHex() }}])"
    }
}

class HeadersMessageParser(private val hasher: IHasher) : IMessageParser {
    override val command: String = "headers"

    override fun parseMessage(input: BitcoinInputMarkable): IMessage {
        val count = input.readVarInt().toInt()

        val headers = Array(count) {
            val version = input.readInt()
            val prevHash = input.readBytes(32)
            val merkleHash = input.readBytes(32)
            val timestamp = input.readUnsignedInt()
            val bits = input.readUnsignedInt()
            val nonce = input.readUnsignedInt()
            input.readVarInt() // tx count always zero

            val headerPayload = BitcoinOutput().also {
                it.writeInt(version)
                it.write(prevHash)
                it.write(merkleHash)
                it.writeUnsignedInt(timestamp)
                it.writeUnsignedInt(bits)
                it.writeUnsignedInt(nonce)
            }

            BlockHeader(version, prevHash, merkleHash, timestamp, bits, nonce, hasher.hash(headerPayload.toByteArray()))
        }

        return HeadersMessage(headers)
    }
}

class HeadersMessageSerializer : IMessageSerializer {
    override val command: String = "headers"

    override fun serialize(message: IMessage): ByteArray? {
        if (message !is HeadersMessage) {
            return null
        }

        val output = BitcoinOutput().also {
            it.writeInt(message.headers.size)
        }

        message.headers.forEach {
            output.writeInt(it.version)
            output.write(it.previousBlockHeaderHash)
            output.write(it.merkleRoot)
            output.writeUnsignedInt(it.timestamp)
            output.writeUnsignedInt(it.bits)
            output.writeUnsignedInt(it.nonce)
        }

        return output.toByteArray()
    }
}
