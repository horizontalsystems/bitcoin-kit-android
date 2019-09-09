package io.horizontalsystems.bitcoincore.core

import io.horizontalsystems.bitcoincore.managers.Bip44RestoreKeyConverter
import io.horizontalsystems.bitcoincore.managers.Bip49RestoreKeyConverter
import io.horizontalsystems.bitcoincore.managers.Bip84RestoreKeyConverter
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType
import io.horizontalsystems.bitcoincore.utils.AddressConverterChain
import io.horizontalsystems.hdwalletkit.HDWallet

enum class Bip {
    BIP44,
    BIP49,
    BIP84;

    val scriptType: Int
        get() {
            return when (this) {
                BIP44 -> ScriptType.P2PKH
                BIP49 -> ScriptType.P2WPKHSH
                BIP84 -> ScriptType.P2WPKH
            }
        }

    val purpose: HDWallet.Purpose
        get() {
            return when (this) {
                BIP44 -> HDWallet.Purpose.BIP44
                BIP49 -> HDWallet.Purpose.BIP49
                BIP84 -> HDWallet.Purpose.BIP84
            }
        }

    fun restoreKeyConverter(addressConverter: AddressConverterChain) = when (this) {
        BIP44 -> Bip44RestoreKeyConverter(addressConverter)
        BIP49 -> Bip49RestoreKeyConverter(addressConverter)
        BIP84 -> Bip84RestoreKeyConverter(addressConverter)
    }
}
