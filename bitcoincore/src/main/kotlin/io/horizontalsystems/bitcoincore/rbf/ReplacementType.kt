package io.horizontalsystems.bitcoincore.rbf

import io.horizontalsystems.bitcoincore.models.Address
import io.horizontalsystems.bitcoincore.models.PublicKey

sealed class ReplacementType {
    object SpeedUp : ReplacementType()
    data class Cancel(val address: Address, val publicKey: PublicKey) : ReplacementType()
}
