package io.horizontalsystems.dashkit.models

import io.horizontalsystems.bitcoincore.storage.FullTransaction

class SpecialTransaction(
        val transaction: FullTransaction,
        extraPayload: ByteArray
): FullTransaction(transaction.header, transaction.inputs, transaction.outputs)
