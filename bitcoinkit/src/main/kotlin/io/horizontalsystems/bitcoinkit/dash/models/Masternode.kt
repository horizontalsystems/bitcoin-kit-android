package io.horizontalsystems.bitcoinkit.dash.models

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import io.horizontalsystems.bitcoincore.io.BitcoinInput

@Entity
class Masternode() : Comparable<Masternode> {

    @PrimaryKey
    var proRegTxHash: ByteArray = byteArrayOf()
    var confirmedHash: ByteArray = byteArrayOf()
    var ipAddress: ByteArray = byteArrayOf()
    var port: Int = 0
    var pubKeyOperator: ByteArray = byteArrayOf()
    var keyIDVoting: ByteArray = byteArrayOf()
    var isValid: Boolean = false

    constructor(input: BitcoinInput) : this() {
        this.proRegTxHash = input.readBytes(32)
        this.confirmedHash = input.readBytes(32)
        this.ipAddress = input.readBytes(16)
        this.port = input.readUnsignedShort()
        this.pubKeyOperator = input.readBytes(48)
        this.keyIDVoting = input.readBytes(20)
        this.isValid = input.readByte().toInt() != 0
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
}
