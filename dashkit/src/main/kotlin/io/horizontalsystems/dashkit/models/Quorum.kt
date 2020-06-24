package io.horizontalsystems.dashkit.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.horizontalsystems.bitcoincore.io.BitcoinInputMarkable
import io.horizontalsystems.bitcoincore.io.BitcoinOutput
import io.horizontalsystems.bitcoincore.utils.HashUtils

@Entity
class Quorum() : Comparable<Quorum> {

    @PrimaryKey
    var hash = byteArrayOf()
    var version = 0
    var type = 0
    var quorumHash = byteArrayOf()
    var signers = byteArrayOf()
    var validMembers = byteArrayOf()
    var quorumPublicKey = byteArrayOf()
    var quorumVvecHash = byteArrayOf()
    var quorumSig = byteArrayOf()
    var sig = byteArrayOf()

    constructor(input: BitcoinInputMarkable) : this() {
        version = input.readUnsignedShort()
        type = input.read()
        quorumHash = input.readBytes(32)

        val signersSize = input.readVarInt().toInt()

        signers = input.readBytes((signersSize + 7) / 8)

        val validMembersSize = input.readVarInt().toInt()
        validMembers = input.readBytes((validMembersSize + 7) / 8)

        quorumPublicKey = input.readBytes(48)
        quorumVvecHash = input.readBytes(32)
        quorumSig = input.readBytes(96)
        sig = input.readBytes(96)

        val hashPayload = BitcoinOutput()
                .writeUnsignedShort(version)
                .writeByte(type)
                .write(quorumHash)
                .writeVarInt(signersSize.toLong())
                .write(signers)
                .writeVarInt(validMembersSize.toLong())
                .write(validMembers)
                .write(quorumPublicKey)
                .write(quorumVvecHash)
                .write(quorumSig)
                .write(sig)
                .toByteArray()

        hash = HashUtils.doubleSha256(hashPayload)
    }

    override fun compareTo(other: Quorum): Int {
        // todo refactor duplicated code
        for (i in 0 until 32) {
            val b1: Int = hash[i].toInt() and 0xff
            val b2: Int = other.hash[i].toInt() and 0xff

            val res = b1.compareTo(b2)
            if (res != 0) return res
        }

        return 0

    }

    override fun equals(other: Any?) = when (other) {
        is Quorum -> hash.contentEquals(other.hash)
        else -> false
    }

    override fun hashCode(): Int {
        return hash.contentHashCode()
    }
}

enum class QuorumType(val value: Int) {
    LLMQ_50_60(1)
}
