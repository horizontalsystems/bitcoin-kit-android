package bitcoin.wallet.kit.managers

import bitcoin.wallet.kit.core.RealmFactory
import bitcoin.wallet.kit.core.changePublicKey
import bitcoin.wallet.kit.core.receivePublicKey
import bitcoin.wallet.kit.hdwallet.PublicKey
import bitcoin.wallet.kit.network.PeerGroup
import bitcoin.wallet.kit.utils.AddressConverter
import io.horizontalsystems.hdwalletkit.HDWallet
import io.realm.Realm

class AddressManager(private val realmFactory: RealmFactory,
                     private val hdWallet: HDWallet,
                     private val peerGroup: PeerGroup,
                     private val addressConverter: AddressConverter) {

    @Throws
    fun changePublicKey(): PublicKey {
        return getPublicKey(HDWallet.Chain.INTERNAL)
    }

    @Throws
    fun receiveAddress(): String {
        return addressConverter.convert(getPublicKey(HDWallet.Chain.EXTERNAL).publicKey).toString()
    }

    @Throws
    fun generateKeys() {
        val realm = realmFactory.realm
        val externalKeys = generateKeys(true, realm)
        val internalKeys = generateKeys(false, realm)

        realm.executeTransaction {
            realm.insert(externalKeys)
            realm.insert(internalKeys)
        }

        realm.close()

        externalKeys.forEach {
            peerGroup.addPublicKeyFilter(it)
        }

        internalKeys.forEach {
            peerGroup.addPublicKeyFilter(it)
        }
    }

    @Throws
    private fun generateKeys(external: Boolean, realm: Realm): List<PublicKey> {
        val keys = mutableListOf<PublicKey>()
        val existingKeys = realm.where(PublicKey::class.java)
                .equalTo("external", external)
                .sort("index")
                .findAll()

        val existingFreshKeys = existingKeys.filter { it.outputs?.size ?: 0 == 0 }

        if (existingFreshKeys.size < hdWallet.gapLimit) {
            val lastIndex = existingKeys.lastOrNull()?.index ?: -1

            repeat(hdWallet.gapLimit - existingFreshKeys.size) {
                val keyIndexToGenerate = lastIndex + it + 1

                val newPublicKey = when {
                    external -> hdWallet.receivePublicKey(keyIndexToGenerate)
                    else -> hdWallet.changePublicKey(keyIndexToGenerate)
                }

                keys.add(newPublicKey)
            }
        }

        return keys
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

        peerGroup.addPublicKeyFilter(newPublicKey)

        return newPublicKey
    }

}
