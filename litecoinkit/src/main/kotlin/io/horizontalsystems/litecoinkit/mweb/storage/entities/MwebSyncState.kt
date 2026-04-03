package io.horizontalsystems.litecoinkit.mweb.storage.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Tracks how far MWEB output scanning has progressed. */
@Entity(tableName = "mweb_sync_state")
data class MwebSyncState(
    @PrimaryKey var id: Int = 1,
    /** Global MWEB leaf index of the last output we have stored. */
    var lastLeafIndex: Long = 0L,
    /** Total raw outputs stored (informational). */
    var storedOutputCount: Long = 0L
)