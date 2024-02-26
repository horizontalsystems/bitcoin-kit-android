package io.horizontalsystems.bitcoincore.rbf

import io.horizontalsystems.bitcoincore.models.Address

sealed class ReplacementType {
    object SpeedUp : ReplacementType()
    data class Cancel(val changeAddress: Address) : ReplacementType()
}
