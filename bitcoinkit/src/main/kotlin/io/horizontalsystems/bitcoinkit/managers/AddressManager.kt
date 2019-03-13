package io.horizontalsystems.bitcoinkit.managers

import io.horizontalsystems.bitcoinkit.core.RealmFactory
import io.horizontalsystems.bitcoinkit.core.publicKey
import io.horizontalsystems.bitcoinkit.models.PublicKey
import io.horizontalsystems.bitcoinkit.utils.AddressConverter
import io.horizontalsystems.hdwalletkit.HDWallet
import io.realm.Realm
import io.realm.Sort

class AddressManager(private val realmFactory: RealmFactory, private val hdWallet: HDWallet, private val addressConverter: AddressConverter) {

    @Throws
    fun changePublicKey(realm: Realm): PublicKey {
        return getPublicKey(HDWallet.Chain.INTERNAL, realm)
    }

    @Throws
    fun receiveAddress(): String {
        realmFactory.realm.use { realm ->
            val publicKey = getPublicKey(HDWallet.Chain.EXTERNAL, realm)
            val address = addressConverter.convert(publicKey.publicKeyHash)

            return address.string
        }
    }

    fun fillGap() {
        realmFactory.realm.use { realm ->
            val lastUsedAccount = realm.where(PublicKey::class.java)
                    .sort("account", Sort.DESCENDING)
                    .findAll()
                    .find { (it.outputs?.size ?: 0) > 0 }
                    ?.account

            val requiredAccountsCount = lastUsedAccount?.let { it + 4 } ?: 1

            repeat(requiredAccountsCount) { account ->
                fillGap(account, true, realm)
                fillGap(account, false, realm)
            }
        }
    }

    fun addKeys(keys: List<PublicKey>) {
        if (keys.isEmpty()) return

        realmFactory.realm.use { realm ->
            realm.executeTransaction {
                realm.insertOrUpdate(keys)
            }
        }
    }

    fun gapShifts(realm: Realm): Boolean {
        val lastAccount = realm.where(PublicKey::class.java).sort("account", Sort.DESCENDING).findFirst()?.account
                ?: return false

        for (i in 0..lastAccount) {
            if (gapKeysCount(i, true, realm) < hdWallet.gapLimit) {
                return true
            }

            if (gapKeysCount(i, false, realm) < hdWallet.gapLimit) {
                return true
            }
        }

        return false
    }

    private fun fillGap(account: Int, external: Boolean, realm: Realm) {
        val gapKeysCount = gapKeysCount(account, external, realm)
        val keys = mutableListOf<PublicKey>()
        if (gapKeysCount < hdWallet.gapLimit) {
            val lastIndex = realm.where(PublicKey::class.java)
                    .equalTo("account", account)
                    .equalTo("external", external)
                    .sort("index", Sort.DESCENDING)
                    .findFirst()?.index ?: -1

            for (i in 1..hdWallet.gapLimit - gapKeysCount) {
                val publicKey = hdWallet.publicKey(account, lastIndex + i, external)
                keys.add(publicKey)
            }
        }

        addKeys(keys)
    }

    private fun gapKeysCount(account: Int, external: Boolean, realm: Realm): Int {
        val publicKeys = realm.where(PublicKey::class.java).equalTo("account", account).equalTo("external", external)
        val lastUsedKey = publicKeys.sort("index").findAll().lastOrNull { it.outputs?.size ?: 0 > 0 }

        return when (lastUsedKey) {
            null -> publicKeys.count().toInt()
            else -> publicKeys.greaterThan("index", lastUsedKey.index).count().toInt()
        }
    }

    @Throws
    private fun getPublicKey(chain: HDWallet.Chain, realm: Realm): PublicKey {
        val existingKeys = realm.where(PublicKey::class.java)
                .equalTo("account", 0L)
                .equalTo("external", chain == HDWallet.Chain.EXTERNAL)
                .sort("index")
                .findAll()

        return existingKeys.find { it.outputs?.size == 0 } ?: throw Error.NoUnusedPublicKey
    }

    companion object {
        fun create(realmFactory: RealmFactory, hdWallet: HDWallet, addressConverter: AddressConverter): AddressManager {
            val addressManager = AddressManager(realmFactory, hdWallet, addressConverter)
            addressManager.fillGap()
            return addressManager
        }
    }

    object Error {
        object NoUnusedPublicKey : Exception()
    }

}
