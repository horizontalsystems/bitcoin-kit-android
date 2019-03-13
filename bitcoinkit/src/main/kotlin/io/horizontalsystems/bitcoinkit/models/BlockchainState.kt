package io.horizontalsystems.bitcoinkit.models

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

@Entity
data class BlockchainState(var initialRestored: Boolean?) {

    @PrimaryKey
    var primaryKey: String = "primary-key"

}
