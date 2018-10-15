package bitcoin.wallet.kit.models

import bitcoin.wallet.kit.core.toHexString
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class BlockHash() : RealmObject() {

    @PrimaryKey
    var reversedHeaderHashHex = ""

    var headerHash = byteArrayOf()
    var height: Int = 0
    var order: Int = 0

    constructor(headerHash: ByteArray, height: Int, order: Int = 0) : this() {
        this.headerHash = headerHash
        this.reversedHeaderHashHex = headerHash.reversedArray().toHexString()
        this.height = height
        this.order = order
    }

}
