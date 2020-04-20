package io.horizontalsystems.bitcoincore.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class BlockchainState(var initialRestored: Boolean?) {

    @PrimaryKey
    var primaryKey: String = "primary-key"

}
