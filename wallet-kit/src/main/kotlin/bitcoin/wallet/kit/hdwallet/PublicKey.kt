package bitcoin.wallet.kit.hdwallet

import bitcoin.wallet.kit.core.toHexString
import bitcoin.wallet.kit.models.TransactionOutput
import io.realm.RealmObject
import io.realm.RealmResults
import io.realm.annotations.LinkingObjects
import io.realm.annotations.PrimaryKey

open class PublicKey() : RealmObject() {

    var index = 0

    var external = true

    var publicKey: ByteArray = byteArrayOf()

    @PrimaryKey
    var publicKeyHash = ""

    @LinkingObjects("publicKey")
    val outputs: RealmResults<TransactionOutput>? = null

    constructor(index: Int, external: Boolean, publicKey: ByteArray, publicKeyHash: ByteArray) : this() {
        this.index = index
        this.external = external
        this.publicKey = publicKey
        this.publicKeyHash = publicKeyHash.toHexString()
    }

}
