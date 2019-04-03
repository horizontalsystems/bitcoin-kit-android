package io.horizontalsystems.bitcoinkit.dash.models

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

@Entity
class MasternodeListState(var baseBlockHash: ByteArray) {

    @PrimaryKey
    var primaryKey: String = "primary-key"

}
