package io.horizontalsystems.ecash

import io.horizontalsystems.bitcoincore.core.scriptType
import io.horizontalsystems.bitcoincore.managers.IRestoreKeyConverter
import io.horizontalsystems.bitcoincore.models.PublicKey
import io.horizontalsystems.bitcoincore.utils.IAddressConverter
import io.horizontalsystems.hdwalletkit.HDWallet

class ECashRestoreKeyConverter(
    private val addressConverter: IAddressConverter,
    private val purpose: HDWallet.Purpose
) : IRestoreKeyConverter {
    override fun keysForApiRestore(publicKey: PublicKey): List<String> {
        // Blockchair rejects the whole request with 400 if any key is not an address.
        return listOf(addressConverter.convert(publicKey, purpose.scriptType).stringValue)
    }

    override fun bloomFilterElements(publicKey: PublicKey): List<ByteArray> {
        return listOf(publicKey.publicKeyHash, publicKey.publicKey)
    }
}
