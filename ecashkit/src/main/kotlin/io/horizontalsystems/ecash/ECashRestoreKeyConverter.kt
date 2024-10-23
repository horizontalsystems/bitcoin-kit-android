package io.horizontalsystems.ecash

import io.horizontalsystems.bitcoincore.core.scriptType
import io.horizontalsystems.bitcoincore.extensions.toHexString
import io.horizontalsystems.bitcoincore.managers.IRestoreKeyConverter
import io.horizontalsystems.bitcoincore.models.PublicKey
import io.horizontalsystems.bitcoincore.utils.IAddressConverter
import io.horizontalsystems.hdwalletkit.HDWallet

class ECashRestoreKeyConverter(
    private val addressConverter: IAddressConverter,
    private val purpose: HDWallet.Purpose
) : IRestoreKeyConverter {
    override fun keysForApiRestore(publicKey: PublicKey): List<String> {
        return listOf(
            publicKey.publicKeyHash.toHexString(),
            addressConverter.convert(publicKey, purpose.scriptType).stringValue
        )
    }

    override fun bloomFilterElements(publicKey: PublicKey): List<ByteArray> {
        return listOf(publicKey.publicKeyHash, publicKey.publicKey)
    }
}
