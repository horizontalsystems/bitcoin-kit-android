package io.horizontalsystems.bitcoincore.models

import android.arch.persistence.room.*
import io.horizontalsystems.bitcoincore.core.IStorage
import io.horizontalsystems.bitcoincore.transactions.scripts.OpCodes
import io.horizontalsystems.bitcoincore.utils.Utils

@Entity(indices = [Index("publicKey", "publicKeyHash", "scriptHashP2WPKH")])
class PublicKey {

    @PrimaryKey
    var path: String = ""

    var account = 0
    @ColumnInfo(name = "address_index")
    var index = 0
    var external = true

    var publicKeyHash = byteArrayOf()
    var publicKey = byteArrayOf()
    var scriptHashP2WPKH = byteArrayOf()

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

        val version = 0
        val redeemScript = OpCodes.push(version) + OpCodes.push(this.publicKeyHash)
        this.scriptHashP2WPKH = Utils.sha256Hash160(redeemScript)
    }
}
