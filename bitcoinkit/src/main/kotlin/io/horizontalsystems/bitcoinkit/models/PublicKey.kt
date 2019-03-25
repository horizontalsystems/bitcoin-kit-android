package io.horizontalsystems.bitcoinkit.models

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.Ignore
import android.arch.persistence.room.PrimaryKey
import io.horizontalsystems.bitcoinkit.core.IStorage
import io.horizontalsystems.bitcoinkit.core.toHexString
import io.horizontalsystems.bitcoinkit.transactions.scripts.OpCodes
import io.horizontalsystems.bitcoinkit.utils.Utils

@Entity
class PublicKey {

    @PrimaryKey
    var path: String = ""

    var account = 0
    @ColumnInfo(name = "address_index")
    var index = 0
    var external = true

    var publicKeyHex = ""
    var publicKeyHash: ByteArray = byteArrayOf()
    var publicKey: ByteArray = byteArrayOf()
    var scriptHashP2WPKH: ByteArray = byteArrayOf()
    var scriptHashP2WPKHHex: String = ""

    fun used(storage: IStorage): Boolean {
        return storage.getOutputsOfPublicKey(this).isNotEmpty()
    }

    constructor()

    @Ignore
    constructor(account: Int, index: Int, external: Boolean, publicKey: ByteArray, publicKeyHash: ByteArray) {
        this.path = "$account/$index/${if (external) 1 else 0}"
        this.account = account
        this.index = index
        this.external = external
        this.publicKey = publicKey
        this.publicKeyHash = publicKeyHash
        this.publicKeyHex = publicKeyHash.toHexString()

        val version = 0
        val redeemScript = OpCodes.push(version) + OpCodes.push(this.publicKeyHash)
        this.scriptHashP2WPKH = Utils.sha256Hash160(redeemScript)
        this.scriptHashP2WPKHHex = scriptHashP2WPKH.toHexString()
    }

}
