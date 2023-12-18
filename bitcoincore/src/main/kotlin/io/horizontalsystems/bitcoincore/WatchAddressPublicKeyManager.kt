package io.horizontalsystems.bitcoincore

import io.horizontalsystems.bitcoincore.apisync.legacy.IPublicKeyFetcher
import io.horizontalsystems.bitcoincore.core.IPublicKeyManager
import io.horizontalsystems.bitcoincore.managers.AccountPublicKeyManager
import io.horizontalsystems.bitcoincore.managers.BloomFilterManager
import io.horizontalsystems.bitcoincore.managers.IBloomFilterProvider
import io.horizontalsystems.bitcoincore.managers.RestoreKeyConverterChain
import io.horizontalsystems.bitcoincore.models.PublicKey
import io.horizontalsystems.bitcoincore.models.WatchAddressPublicKey

class WatchAddressPublicKeyManager(
    private val publicKey: WatchAddressPublicKey,
    private val restoreKeyConverter: RestoreKeyConverterChain
) : IPublicKeyFetcher, IPublicKeyManager, IBloomFilterProvider {

    override fun publicKeys(indices: IntRange, external: Boolean) = listOf(publicKey)

    override fun changePublicKey() = publicKey

    override fun receivePublicKey() = publicKey

    override fun usedPublicKeys(): List<PublicKey> = listOf(publicKey)

    override fun fillGap() {
        bloomFilterManager?.regenerateBloomFilter()
    }

    override fun addKeys(keys: List<PublicKey>) = Unit

    override fun gapShifts(): Boolean = false

    override fun getPublicKeyByPath(path: String): PublicKey {
        throw AccountPublicKeyManager.Error.InvalidPath
    }

    override var bloomFilterManager: BloomFilterManager? = null

    override fun getBloomFilterElements() = restoreKeyConverter.bloomFilterElements(publicKey)
}
