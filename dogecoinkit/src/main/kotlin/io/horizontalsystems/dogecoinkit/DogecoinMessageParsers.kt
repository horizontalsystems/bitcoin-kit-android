package io.horizontalsystems.dogecoinkit

import io.horizontalsystems.bitcoincore.core.IHasher
import io.horizontalsystems.bitcoincore.io.BitcoinInputMarkable
import io.horizontalsystems.bitcoincore.network.messages.HeadersMessage
import io.horizontalsystems.bitcoincore.network.messages.IMessage
import io.horizontalsystems.bitcoincore.network.messages.IMessageParser
import io.horizontalsystems.bitcoincore.network.messages.MerkleBlockMessage
import io.horizontalsystems.bitcoincore.storage.BlockHeader

/**
 * Reads a Dogecoin block header, consuming the AuxPoW structure that follows it on merge-mined
 * blocks. The block's identity hash is still the double SHA-256 of the first 80 bytes — scrypt is
 * only used for proof of work, the same split as Litecoin.
 */
class DogecoinBlockHeaderParser(private val hasher: IHasher) {

    fun parse(input: BitcoinInputMarkable): BlockHeader {
        input.mark()
        val payload = input.readBytes(80)
        val hash = hasher.hash(payload)
        input.reset()

        val version = input.readInt()
        val previousBlockHeaderHash = input.readBytes(32)
        val merkleRoot = input.readBytes(32)
        val timestamp = input.readUnsignedInt()
        val bits = input.readUnsignedInt()
        val nonce = input.readUnsignedInt()

        if (AuxPoW.hasAuxPoW(version)) {
            AuxPoW.skip(input)
        }

        return BlockHeader(version, previousBlockHeaderHash, merkleRoot, timestamp, bits, nonce, hash)
    }
}

/**
 * Replaces bitcoincore's `merkleblock` parser, which reads a fixed 80 bytes and would land on the
 * AuxPoW blob instead of the transaction count. Registered after the core defaults, so it wins.
 */
class DogecoinMerkleBlockMessageParser(
    private val blockHeaderParser: DogecoinBlockHeaderParser
) : IMessageParser {
    override val command = "merkleblock"

    override fun parseMessage(input: BitcoinInputMarkable): IMessage {
        val header = blockHeaderParser.parse(input)
        val txCount = input.readInt()

        val hashCount = input.readVarInt().toInt()
        val hashes: MutableList<ByteArray> = mutableListOf()
        repeat(hashCount) {
            hashes.add(input.readBytes(32))
        }

        val flagsCount = input.readVarInt().toInt()
        val flags = input.readBytes(flagsCount)

        return MerkleBlockMessage(header, txCount, hashCount, hashes, flagsCount, flags)
    }
}

/**
 * Replaces bitcoincore's `headers` parser for the same reason.
 *
 * DogecoinKit only supports Blockchair sync, which never sends `getheaders`, so nothing in the
 * wallet reaches this today. The checkpoint generator in `:tools` does, and keeping the pair
 * together means the two parsers cannot drift apart if the sync mode is ever widened.
 */
class DogecoinHeadersMessageParser(
    private val blockHeaderParser: DogecoinBlockHeaderParser
) : IMessageParser {
    override val command = "headers"

    override fun parseMessage(input: BitcoinInputMarkable): IMessage {
        val count = input.readVarInt()
        if (count < 0 || count > MAX_HEADERS) {
            throw InvalidHeadersMessage("Too many headers: $count")
        }

        val headers = Array(count.toInt()) {
            val header = blockHeaderParser.parse(input)
            input.readVarInt() // tx count, always zero in a headers message
            header
        }

        return HeadersMessage(headers)
    }

    class InvalidHeadersMessage(message: String) : RuntimeException(message)

    companion object {
        // Dogecoin Core's MAX_HEADERS_RESULTS. Bounded so a hostile peer cannot make us allocate
        // an arbitrarily large array before the reads run off the end of the message.
        private const val MAX_HEADERS = 2000
    }
}
