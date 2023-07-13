package io.horizontalsystems.dashkit.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.horizontalsystems.bitcoincore.io.BitcoinInputMarkable
import io.horizontalsystems.bitcoincore.io.BitcoinOutput
import io.horizontalsystems.bitcoincore.utils.HashUtils

@Entity
class Masternode() : Comparable<Masternode> {
    var nVersion = 0
    @PrimaryKey
    var proRegTxHash = byteArrayOf()
    var confirmedHash = byteArrayOf()
    var ipAddress = byteArrayOf()
    var port = 0
    var pubKeyOperator = byteArrayOf()
    var keyIDVoting = byteArrayOf()
    var isValid = false
    var type: Int? = null
    var platformHTTPPort: Int? = null
    var platformNodeID: ByteArray? = null

    var hash = byteArrayOf()

    constructor(input: BitcoinInputMarkable) : this() {
        nVersion = input.readUnsignedShort()
        proRegTxHash = input.readBytes(32)
        confirmedHash = input.readBytes(32)
        ipAddress = input.readBytes(16)
        port = input.readUnsignedShort()
        pubKeyOperator = input.readBytes(48)
        keyIDVoting = input.readBytes(20)
        isValid = input.read() != 0

        if (nVersion >= 2) {
            type = input.readUnsignedShort()
            if (type == 1) {
                platformHTTPPort = input.readUnsignedShort()
                platformNodeID = input.readBytes(20)
            }
        }

        val payload = BitcoinOutput()
            .write(proRegTxHash)
            .write(confirmedHash)
            .write(ipAddress)
            .writeUnsignedShort(port)
            .write(pubKeyOperator)
            .write(keyIDVoting)
            .writeByte(if (isValid) 1 else 0)

        type?.let {
            payload.writeUnsignedShort(it)
        }
        platformHTTPPort?.let {
            payload.writeUnsignedShort(it)
        }
        platformNodeID?.let {
            payload.write(it)
        }

        hash = HashUtils.doubleSha256(payload.toByteArray())
    }

    override fun compareTo(other: Masternode): Int {

        for (i in 0 until 32) {
            val b1: Int = proRegTxHash[i].toInt() and 0xff
            val b2: Int = other.proRegTxHash[i].toInt() and 0xff

            val res = b1.compareTo(b2)
            if (res != 0) return res
        }

        return 0
    }

    override fun equals(other: Any?) = when (other) {
        is Masternode -> proRegTxHash.contentEquals(other.proRegTxHash)
        else -> false
    }

    override fun hashCode(): Int {
        return proRegTxHash.contentHashCode()
    }

}
