package bitcoin.wallet.kit.models

import bitcoin.wallet.kit.core.toHexString
import io.realm.RealmObject
import io.realm.RealmResults
import io.realm.annotations.LinkingObjects
import io.realm.annotations.PrimaryKey

open class PublicKey() : RealmObject() {

    var index = 0
    var external = true

    @PrimaryKey
    var publicKeyHex = ""
    var publicKeyHash: ByteArray = byteArrayOf()
    var publicKey: ByteArray = byteArrayOf()

    @LinkingObjects("publicKey")
    val outputs: RealmResults<TransactionOutput>? = null

    constructor(index: Int, external: Boolean, publicKey: ByteArray, publicKeyHash: ByteArray) : this() {
        this.index = index
        this.external = external
        this.publicKey = publicKey
        this.publicKeyHash = publicKeyHash
        this.publicKeyHex = publicKeyHash.toHexString()
    }

}
