package io.horizontalsystems.bitcoincore.managers

import io.horizontalsystems.bitcoincore.core.IStorage
import io.horizontalsystems.bitcoincore.core.Wallet
import io.horizontalsystems.bitcoincore.models.PublicKey
import io.horizontalsystems.bitcoincore.storage.PublicKeyWithUsedState

class PublicKeyManager(
        private val storage: IStorage,
        private val wallet: Wallet,
        private val restoreKeyConverter: RestoreKeyConverterChain
) : IBloomFilterProvider {

    // IBloomFilterProvider

    override var bloomFilterManager: BloomFilterManager? = null

    override fun getBloomFilterElements(): List<ByteArray> {
        val elements = mutableListOf<ByteArray>()

        for (publicKey in storage.getPublicKeys()) {
            elements.addAll(restoreKeyConverter.bloomFilterElements(publicKey))
        }

        return elements
    }

    @Throws
    fun receivePublicKey(): PublicKey {
        return getPublicKey(true)
    }

    @Throws
    fun changePublicKey(): PublicKey {
        return getPublicKey(external = false)
    }

    fun getPublicKeyByPath(path: String): PublicKey {
        val parts = path.split("/").map { it.toInt() }

        return wallet.publicKey(parts[0], parts[2], parts[1] == 1)
    }

    fun fillGap() {
        val lastUsedAccount = storage.getPublicKeysUsed().map { it.account }.max()

        val requiredAccountsCount = if (lastUsedAccount != null) {
            lastUsedAccount + 1 + 1 //  One because account starts from 0, One because we must have n+1 accounts
        } else {
            1
        }

        repeat(requiredAccountsCount) { account ->
            fillGap(account, true)
            fillGap(account, false)
        }

        bloomFilterManager?.regenerateBloomFilter()
    }

    fun addKeys(keys: List<PublicKey>) {
        if (keys.isEmpty()) return

        storage.savePublicKeys(keys)
    }

    fun gapShifts(): Boolean {
        val publicKeys = storage.getPublicKeysWithUsedState()
        val lastAccount = publicKeys.map { it.publicKey.account }.max() ?: return false

        for (i in 0..lastAccount) {
            if (gapKeysCount(publicKeys.filter { it.publicKey.account == i && it.publicKey.external }) < wallet.gapLimit) {
                return true
            }

            if (gapKeysCount(publicKeys.filter { it.publicKey.account == i && !it.publicKey.external }) < wallet.gapLimit) {
                return true
            }
        }

        return false
    }

    private fun fillGap(account: Int, external: Boolean) {
        val publicKeys = storage.getPublicKeysWithUsedState().filter { it.publicKey.account == account && it.publicKey.external == external }
        val keysCount = gapKeysCount(publicKeys)
        val keys = mutableListOf<PublicKey>()

        if (keysCount < wallet.gapLimit) {
            val lastIndex = publicKeys.maxBy { it.publicKey.index }?.publicKey?.index ?: -1

            val newKeysStartIndex = lastIndex + 1
            val indices = newKeysStartIndex until (newKeysStartIndex + wallet.gapLimit - keysCount)
            val newKeys = wallet.publicKeys(account, indices, external)

            keys.addAll(newKeys)
        }

        addKeys(keys)
    }

    private fun gapKeysCount(publicKeys: List<PublicKeyWithUsedState>): Int {
        val lastUsedKey = publicKeys.filter { it.used }.maxBy { it.publicKey.index }

        return when (lastUsedKey) {
            null -> publicKeys.size
            else -> publicKeys.filter { it.publicKey.index > lastUsedKey.publicKey.index }.size
        }
    }

    @Throws
    private fun getPublicKey(external: Boolean): PublicKey {
        return storage.getPublicKeysUnused()
                .filter { it.account == 0 && it.external == external }
                .sortedWith(compareBy { it.index })
                .firstOrNull() ?: throw Error.NoUnusedPublicKey
    }

    companion object {
        fun create(storage: IStorage, wallet: Wallet, restoreKeyConverter: RestoreKeyConverterChain): PublicKeyManager {
            val addressManager = PublicKeyManager(storage, wallet, restoreKeyConverter)
            addressManager.fillGap()
            return addressManager
        }
    }

    object Error {
        object NoUnusedPublicKey : Exception()
    }

}
