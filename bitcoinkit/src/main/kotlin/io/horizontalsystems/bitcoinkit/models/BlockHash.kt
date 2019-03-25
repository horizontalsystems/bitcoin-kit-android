package io.horizontalsystems.bitcoinkit.models

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import io.horizontalsystems.bitcoinkit.extensions.toReversedHex

@Entity
class BlockHash(val headerHash: ByteArray, val height: Int, val sequence: Int = 0) {

    @PrimaryKey
    var headerHashReversedHex: String = headerHash.toReversedHex()

}
