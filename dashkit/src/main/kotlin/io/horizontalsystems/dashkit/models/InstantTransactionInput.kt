package io.horizontalsystems.dashkit.models

import android.arch.persistence.room.Entity

@Entity(primaryKeys = ["txHash", "inputTxHash"])
class InstantTransactionInput(
        val txHash: ByteArray,
        val inputTxHash: ByteArray,
        val timeCreated: Long,
        val voteCount: Int,
        val blockHeight: Int?)