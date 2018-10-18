package io.horizontalsystems.bitcoinkit.models

import io.horizontalsystems.bitcoinkit.core.toHexString
import io.horizontalsystems.bitcoinkit.exceptions.InvalidMerkleBlockException
import io.horizontalsystems.bitcoinkit.io.BitcoinInput
import io.horizontalsystems.bitcoinkit.utils.HashUtils
import io.horizontalsystems.bitcoinkit.utils.MerkleBranch

/**
 * MerkleBlock
 *
 *  Size        Field           Description
 *  ====        =====           ===========
 *  80 bytes    Header          Consists of 6 fields that are hashed to calculate the block hash
 *  VarInt      HashCount       Number of hashes
 *  Variable    Hashes          Hashes in depth-first order
 *  VarInt      FlagsCount      Number of bytes of flag bits
 *  Variable    Flags           Flag bits packed 8 per byte, least significant bit first
 */
class MerkleBlock() {

    lateinit var header: Header
    var hashes: Array<ByteArray> = arrayOf()
    var flags: ByteArray = byteArrayOf()

    var associatedTransactionHexes = listOf<String>()
    val associatedTransactions = mutableListOf<Transaction>()
    val blockHash: ByteArray by lazy {
        HashUtils.doubleSha256(header.toByteArray())
    }

    val reversedHeaderHashHex: String by lazy {
        HashUtils.toHexString(blockHash.reversedArray())
    }

    var txCount: Int = 0
    private var hashCount: Long = 0L
    private var flagsCount: Long = 0L

    private val MAX_BLOCK_SIZE: Int = 1000000

    val complete: Boolean
        get() = associatedTransactionHexes.size == associatedTransactions.size

    constructor(input: BitcoinInput) : this() {
        header = Header(input)

        txCount = input.readInt()
        if (txCount < 1 || txCount > MAX_BLOCK_SIZE / 60) {
            throw InvalidMerkleBlockException(String.format("Transaction count %d is not valid", txCount))
        }

        hashCount = input.readVarInt()
        if (hashCount < 0 || hashCount > txCount) {
            throw InvalidMerkleBlockException(String.format("Hash count %d is not valid", hashCount))
        }

        hashes = Array(hashCount.toInt()) {
            input.readBytes(32)
        }

        flagsCount = input.readVarInt()
        if (flagsCount < 1) {
            throw InvalidMerkleBlockException(String.format("Flag count %d is not valid", flagsCount))
        }

        flags = input.readBytes(flagsCount.toInt())

        val matchedHashes = mutableListOf<ByteArray>()
        val merkleRoot = MerkleBranch(txCount, hashes, flags).calculateMerkleRoot(matchedHashes)
        associatedTransactionHexes = matchedHashes.map { it.toHexString() }

        if (!header.merkleHash.contentEquals(merkleRoot)) {
            throw InvalidMerkleBlockException("Merkle root is not valid")
        }
    }

    fun addTransaction(transaction: Transaction) {
        associatedTransactions.add(transaction)
    }

}
