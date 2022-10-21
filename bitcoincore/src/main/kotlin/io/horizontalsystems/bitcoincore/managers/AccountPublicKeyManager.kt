package io.horizontalsystems.bitcoincore.managers

import io.horizontalsystems.bitcoincore.core.IAccountWallet
import io.horizontalsystems.bitcoincore.core.IPublicKeyManager
import io.horizontalsystems.bitcoincore.core.IStorage
import io.horizontalsystems.bitcoincore.models.PublicKey
import io.horizontalsystems.bitcoincore.storage.PublicKeyWithUsedState

class AccountPublicKeyManager(
    private val storage: IStorage,
    private val wallet: IAccountWallet,
    private val restoreKeyConverter: RestoreKeyConverterChain
) : IBloomFilterProvider, IPublicKeyManager {

    override var bloomFilterManager: BloomFilterManager? = null

    override fun getBloomFilterElements(): List<ByteArray> {
        val elements = mutableListOf<ByteArray>()

        for (publicKey in storage.getPublicKeys()) {
            elements.addAll(restoreKeyConverter.bloomFilterElements(publicKey))
        }

        return elements
    }

    @Throws
    override fun receivePublicKey(): PublicKey {
        return getPublicKey(external = true)
    }

    @Throws
    override fun changePublicKey(): PublicKey {
        return getPublicKey(external = false)
    }

    override fun getPublicKeyByPath(path: String): PublicKey {
        val parts = path.split("/").map { it.toInt() }

        if (parts.size != 2) throw Error.InvalidPath

        return wallet.publicKey(parts[1], parts[0] == 0)
    }

    override fun fillGap() {
        fillGap(true)
        fillGap(false)

        bloomFilterManager?.regenerateBloomFilter()
    }

    override fun addKeys(keys: List<PublicKey>) {
        if (keys.isEmpty()) return

        storage.savePublicKeys(keys)
    }

    override fun gapShifts(): Boolean {
        val publicKeys = storage.getPublicKeysWithUsedState()

        if (gapKeysCount(publicKeys.filter { it.publicKey.external }) < wallet.gapLimit) {
            return true
        }

        if (gapKeysCount(publicKeys.filter { !it.publicKey.external }) < wallet.gapLimit) {
            return true
        }

        return false
    }

    private fun fillGap(external: Boolean) {
        val publicKeys = storage.getPublicKeysWithUsedState().filter { it.publicKey.external == external }
        val keysCount = gapKeysCount(publicKeys)
        val keys = mutableListOf<PublicKey>()

        if (keysCount < wallet.gapLimit) {
            val lastIndex = publicKeys.maxBy { it.publicKey.index }?.publicKey?.index ?: -1

            val newKeysStartIndex = lastIndex + 1
            val indices = newKeysStartIndex until (newKeysStartIndex + wallet.gapLimit - keysCount)
            val newKeys = wallet.publicKeys(indices, external)

            keys.addAll(newKeys)
        }

        addKeys(keys)
    }

    private fun gapKeysCount(publicKeys: List<PublicKeyWithUsedState>): Int {
        return when (val lastUsedKey = publicKeys.filter { it.used }.maxBy { it.publicKey.index }) {
            null -> publicKeys.size
            else -> publicKeys.filter { it.publicKey.index > lastUsedKey.publicKey.index }.size
        }
    }

    @Throws
    private fun getPublicKey(external: Boolean): PublicKey {
        return storage.getPublicKeysUnused()
            .filter { it.external == external }
            .sortedWith(compareBy { it.index })
            .firstOrNull() ?: throw Error.NoUnusedPublicKey
    }

    companion object {
        fun create(storage: IStorage, wallet: IAccountWallet, restoreKeyConverter: RestoreKeyConverterChain): AccountPublicKeyManager {
            val addressManager = AccountPublicKeyManager(storage, wallet, restoreKeyConverter)
            addressManager.fillGap()
            return addressManager
        }
    }

    object Error {
        object NoUnusedPublicKey : Exception()
        object InvalidPath : Exception()
    }

}
