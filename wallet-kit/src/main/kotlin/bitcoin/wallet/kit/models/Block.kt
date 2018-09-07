package bitcoin.wallet.kit.models

import bitcoin.walllet.kit.utils.HashUtils
import io.realm.RealmObject
import io.realm.RealmResults
import io.realm.annotations.LinkingObjects
import io.realm.annotations.PrimaryKey

/**
 * Block
 *
 *  Size        Field           Description
 *  ====        =====           ===========
 *  80 bytes    Header          Consists of 6 fields that are hashed to calculate the block hash
 *  VarInt      TxCount         Number of transactions in the block
 *  Variable    Transactions    The transactions in the block
 */
open class Block : RealmObject() {

    var synced = false
    var height: Int = 0
    var header: Header? = null
        set(value) {
            field = value
            value?.let {
                headerHash = value.hash
            }
        }

    var headerHash: ByteArray = byteArrayOf()
        set(value) {
            field = value
            reversedHeaderHashHex = HashUtils.toHexString(value.reversedArray())
        }

    @PrimaryKey
    var reversedHeaderHashHex = ""
    var previousBlock: Block? = null

    @LinkingObjects("block")
    val transactions: RealmResults<Transaction>? = null

}
