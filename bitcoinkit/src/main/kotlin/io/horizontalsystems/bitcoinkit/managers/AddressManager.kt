package io.horizontalsystems.bitcoinkit.managers

import io.horizontalsystems.bitcoinkit.core.IStorage
import io.horizontalsystems.bitcoinkit.core.publicKey
import io.horizontalsystems.bitcoinkit.models.PublicKey
import io.horizontalsystems.bitcoinkit.utils.AddressConverter
import io.horizontalsystems.hdwalletkit.HDWallet

class AddressManager(private val storage: IStorage, private val hdWallet: HDWallet, private val addressConverter: AddressConverter) {

    @Throws
    fun changePublicKey(): PublicKey {
        return getPublicKey(external = false)
    }

    @Throws
    fun receiveAddress(): String {
        val publicKey = getPublicKey(external = true)
        val address = addressConverter.convert(publicKey.publicKeyHash)

        return address.string
    }

    fun fillGap() {
        val lastUsedAccount = storage.getPublicKeys()
                .sortedByDescending { it.account }
                .lastOrNull { it.used(storage) }
                ?.account

        val requiredAccountsCount = lastUsedAccount?.let { it + 4 } ?: 1

        repeat(requiredAccountsCount) { account ->
            fillGap(account, true)
            fillGap(account, false)
        }
    }

    fun addKeys(keys: List<PublicKey>) {
        if (keys.isEmpty()) return

        storage.savePublicKeys(keys)
    }

    fun gapShifts(): Boolean {
        val publicKeys = storage.getPublicKeys()
        val lastAccount = publicKeys
                .sortedByDescending { it.account }
                .firstOrNull()?.account
                ?: return false

        for (i in 0..lastAccount) {
            if (gapKeysCount(publicKeys.filter { it.account == i && it.external }) < hdWallet.gapLimit) {
                return true
            }

            if (gapKeysCount(publicKeys.filter { it.account == i && !it.external }) < hdWallet.gapLimit) {
                return true
            }
        }

        return false
    }

    private fun fillGap(account: Int, external: Boolean) {
        val publicKeys = storage.getPublicKeys().filter { it.account == account && it.external == external }
        val keysCount = gapKeysCount(publicKeys)
        val keys = mutableListOf<PublicKey>()

        if (keysCount < hdWallet.gapLimit) {
            val allKeys = publicKeys.sortedByDescending { it.index }
            val lastIndex = allKeys.lastOrNull()?.index ?: -1

            for (i in 1..hdWallet.gapLimit - keysCount) {
                val publicKey = hdWallet.publicKey(account, lastIndex + i, external)
                keys.add(publicKey)
            }
        }

        addKeys(keys)
    }

    private fun gapKeysCount(publicKeys: List<PublicKey>): Int {
        val lastUsedKey = publicKeys.filter { it.used(storage) }.sortedByDescending { it.index }.lastOrNull()

        return when (lastUsedKey) {
            null -> publicKeys.size
            else -> publicKeys.filter { it.index > lastUsedKey.index }.size
        }
    }

    @Throws
    private fun getPublicKey(external: Boolean): PublicKey {
        return storage.getPublicKeys()
                .filter { it.external == external && !it.used(storage) }
                .sortedWith(compareByDescending { it.account })
                .sortedWith(compareByDescending { it.index })
                .firstOrNull() ?: throw Error.NoUnusedPublicKey
    }

    companion object {
        fun create(storage: IStorage, hdWallet: HDWallet, addressConverter: AddressConverter): AddressManager {
            val addressManager = AddressManager(storage, hdWallet, addressConverter)
            addressManager.fillGap()
            return addressManager
        }
    }

    object Error {
        object NoUnusedPublicKey : Exception()
    }

}
