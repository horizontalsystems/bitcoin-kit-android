package bitcoin.wallet.kit.models

import bitcoin.wallet.kit.core.toHexString
import bitcoin.wallet.kit.models.TransactionOutput
import bitcoin.wallet.kit.scripts.OpCodes
import bitcoin.walllet.kit.utils.Utils
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
    var scriptHashP2WPKH: ByteArray = byteArrayOf()

    @LinkingObjects("publicKey")
    val outputs: RealmResults<TransactionOutput>? = null

    constructor(index: Int, external: Boolean, publicKey: ByteArray, publicKeyHash: ByteArray) : this() {
        this.index = index
        this.external = external
        this.publicKey = publicKey
        this.publicKeyHash = publicKeyHash
        this.publicKeyHex = publicKeyHash.toHexString()

        val version = 0
        val redeemScript = OpCodes.push(version) + OpCodes.push(this.publicKeyHash)
        this.scriptHashP2WPKH = Utils.sha256Hash160(redeemScript)
    }

}
