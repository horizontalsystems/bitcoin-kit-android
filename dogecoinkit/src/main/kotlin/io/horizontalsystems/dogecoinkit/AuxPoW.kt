package io.horizontalsystems.dogecoinkit

import io.horizontalsystems.bitcoincore.io.BitcoinInputMarkable
import io.horizontalsystems.bitcoincore.serializers.TransactionSerializer

/**
 * Dogecoin is merge-mined with Litecoin. A merge-mined block carries a CAuxPow structure proving
 * that work done on the parent chain also counts here, and that structure is serialized *inside*
 * the block, between the 80 byte header and the transaction count:
 *
 *     80  header
 *     ??  AuxPoW      (only when version & 0x100 is set)
 *     ..  tx count (varint) + transactions
 *
 * There is no length prefix on the AuxPoW, so the only way to find where it ends is to parse it.
 * Every header parser in bitcoincore assumes headers are exactly 80 bytes, which is why dogecoinkit
 * registers its own `merkleblock` and `headers` parsers over the core ones.
 *
 * Layout, per dogecoin/src/auxpow.h (CAuxPow extends CMerkleTx):
 *
 *     parent coinbase transaction   (full CTransaction, variable length)
 *     parent block hash             (32)
 *     coinbase merkle branch        (varint count, count * 32, uint32 index)
 *     chain merkle branch           (varint count, count * 32, uint32 index)
 *     parent block header           (80)
 *
 * We only skip the structure — we do not verify it. See DogecoinKit for why.
 */
object AuxPoW {

    /**
     * Bit 8 of the block version marks a merge-mined block. Note this must be a bitmask test:
     * the version also carries the chain id in its high bits and BIP9 signalling in its low ones,
     * so an equality check against any single known version value matches nothing for long.
     * Current mainnet blocks are 0x620104.
     */
    private const val VERSION_AUXPOW_FLAG = 0x100

    /**
     * Merge mining activated on Dogecoin mainnet at this height; no block below it sets the flag.
     */
    const val AUXPOW_START_HEIGHT = 371_337

    /**
     * Consensus caps the chain merkle branch at 30. A merkle branch is log2(tx count) long, so no
     * legitimate branch approaches this bound — it exists so a hostile peer cannot make us
     * allocate on a bogus varint before the read runs off the end of the message.
     */
    private const val MAX_MERKLE_BRANCH_LENGTH = 32

    fun hasAuxPoW(version: Int): Boolean = (version and VERSION_AUXPOW_FLAG) != 0

    /**
     * Consumes the CAuxPow structure, leaving [input] positioned on the transaction count.
     * Call only when [hasAuxPoW] is true for the header just read.
     */
    fun skip(input: BitcoinInputMarkable) {
        // The parent coinbase. TransactionSerializer handles both the legacy and the segwit
        // encoding, which matters here: the parent chain is Litecoin, and Litecoin has segwit.
        TransactionSerializer.deserialize(input)

        input.readBytes(32)     // parent block hash

        skipMerkleBranch(input) // coinbase branch, links the coinbase to the parent merkle root
        skipMerkleBranch(input) // chain branch, links this chain into the merged-mining tree

        input.readBytes(80)     // parent block header
    }

    private fun skipMerkleBranch(input: BitcoinInputMarkable) {
        val count = input.readVarInt()
        if (count < 0 || count > MAX_MERKLE_BRANCH_LENGTH) {
            throw InvalidAuxPoW("Merkle branch too long: $count")
        }

        repeat(count.toInt()) {
            input.readBytes(32)
        }

        input.readUnsignedInt()  // branch index / side mask
    }

    class InvalidAuxPoW(message: String) : RuntimeException(message)
}
