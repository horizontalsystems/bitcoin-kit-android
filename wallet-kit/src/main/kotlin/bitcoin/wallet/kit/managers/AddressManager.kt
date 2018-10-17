package bitcoin.wallet.kit.managers

import bitcoin.wallet.kit.core.RealmFactory
import bitcoin.wallet.kit.core.changePublicKey
import bitcoin.wallet.kit.core.publicKey
import bitcoin.wallet.kit.core.receivePublicKey
import bitcoin.wallet.kit.models.PublicKey
import bitcoin.wallet.kit.utils.AddressConverter
import io.horizontalsystems.hdwalletkit.HDWallet
import io.realm.Realm
import io.realm.Sort

class AddressManager(private val realmFactory: RealmFactory,
                     private val hdWallet: HDWallet,
                     private val bloomFilterManager: BloomFilterManager,
                     private val addressConverter: AddressConverter) {

    @Throws
    fun changePublicKey(): PublicKey {
        return getPublicKey(HDWallet.Chain.INTERNAL)
    }

    @Throws
    fun receiveAddress(): String {
        return addressConverter.convert(getPublicKey(HDWallet.Chain.EXTERNAL).publicKey).toString()
    }

    fun fillGap() {
        fillGap(true)
        fillGap(false)
    }

    fun addKeys(keys: List<PublicKey>) {
        if (keys.isEmpty()) return

        val realm = realmFactory.realm

        realm.executeTransaction {
            realm.insertOrUpdate(keys)
        }

        bloomFilterManager.regenerateBloomFilter()
    }

    fun gapShifts(realm: Realm): Boolean {
        return gapKeysCount(true, realm) < hdWallet.gapLimit || gapKeysCount(false, realm) < hdWallet.gapLimit
    }

    private fun fillGap(external: Boolean) {
        val realm = realmFactory.realm
        val gapKeysCount = gapKeysCount(external, realm)
        val keys = mutableListOf<PublicKey>()
        if (gapKeysCount < hdWallet.gapLimit) {
            val lastIndex = realm.where(PublicKey::class.java).equalTo("external", external)
                    .sort("index", Sort.DESCENDING)
                    .findFirst()?.index ?: -1

            for (i in 1..hdWallet.gapLimit - gapKeysCount) {
                val publicKey = hdWallet.publicKey(lastIndex + i, external)
                keys.add(publicKey)
            }
        }

        addKeys(keys)
    }

    private fun gapKeysCount(external: Boolean, realm: Realm): Int {
        val publicKeys = realm.where(PublicKey::class.java).equalTo("external", external)
        val lastUsedKey = publicKeys.sort("index").findAll().lastOrNull { it.outputs?.size ?: 0 > 0 }

        return when (lastUsedKey) {
            null -> publicKeys.count().toInt()
            else -> publicKeys.greaterThan("index", lastUsedKey.index).count().toInt()
        }
    }

    @Throws
    private fun getPublicKey(chain: HDWallet.Chain): PublicKey {
        val realm = realmFactory.realm
        val existingKeys = realm.where(PublicKey::class.java)
                .equalTo("external", chain == HDWallet.Chain.EXTERNAL)
                .sort("index")
                .findAll()

        val notUsedKey = existingKeys.find { it.outputs?.size == 0 }

        if (notUsedKey != null) {
            return notUsedKey
        }

        val newIndex = (existingKeys.lastOrNull()?.index ?: -1) + 1

        val newPublicKey = when (chain) {
            HDWallet.Chain.EXTERNAL -> hdWallet.receivePublicKey(newIndex)
            else -> hdWallet.changePublicKey(newIndex)
        }

        realm.executeTransaction {
            it.insert(newPublicKey)
        }

        realm.close()

        bloomFilterManager.regenerateBloomFilter()

        return newPublicKey
    }

}
