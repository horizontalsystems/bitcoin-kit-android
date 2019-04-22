package io.horizontalsystems.dashkit.models

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

@Entity
class MasternodeListState(var baseBlockHash: ByteArray) {

    @PrimaryKey
    var primaryKey: String = "primary-key"

}
