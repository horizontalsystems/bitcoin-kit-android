package bitcoin.wallet.kit.hdwallet

import bitcoin.wallet.kit.network.NetworkParameters
import bitcoin.walllet.kit.hdwallet.HDKey
import io.realm.RealmObject

open class PublicKey() : RealmObject() {

    var index = 0
    var external = true
    var address = ""

    var publicKey: ByteArray = byteArrayOf()
    var publicKeyHash: ByteArray = byteArrayOf()

    constructor(index: Int, external: Boolean, key: HDKey, network: NetworkParameters) : this() {
        this.index = index
        this.external = external
        this.publicKey = key.pubKey
        this.publicKeyHash = key.pubKeyHash
        this.address = key.toAddress(network).toString()
    }
}
