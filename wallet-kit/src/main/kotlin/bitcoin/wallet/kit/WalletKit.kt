package bitcoin.wallet.kit

import android.content.Context
import bitcoin.wallet.kit.core.RealmFactory
import bitcoin.wallet.kit.crypto.BloomFilter
import bitcoin.wallet.kit.hdwallet.HDWallet
import bitcoin.wallet.kit.hdwallet.Mnemonic
import bitcoin.wallet.kit.hdwallet.PublicKey
import bitcoin.wallet.kit.managers.*
import bitcoin.wallet.kit.network.MainNet
import bitcoin.wallet.kit.network.PeerGroup
import bitcoin.wallet.kit.network.PeerManager
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.annotations.RealmModule

@RealmModule(library = true, allClasses = true)
class WalletKitModule

class WalletKit(words: List<String>) {

    private val initialSyncer: InitialSyncer

    init {
        val realmFactory = RealmFactory(getRealmConfig())
        val realm = realmFactory.realm

        val network = MainNet()
        val wallet = HDWallet(Mnemonic().toSeed(words), network)
        val pubKeys = realm.where(PublicKey::class.java).findAll()
        val filters = BloomFilter(pubKeys.size)

        pubKeys.forEach {
            filters.insert(it.publicKey)
        }

        val peerManager = PeerManager(network)

        val peerGroup = PeerGroup(peerManager, network, 1)
        peerGroup.setBloomFilter(filters)
        peerGroup.listener = Syncer(realmFactory, peerGroup, network)

        val apiManager = ApiManager("http://ipfs.grouvi.org/ipns/QmVefrf2xrWzGzPpERF6fRHeUTh9uVSyfHHh4cWgUBnXpq/io-hs/data/blockstore")
        val stateManager = StateManager(realmFactory)

        val blockDiscover = BlockDiscover(wallet, apiManager, network)

        initialSyncer = InitialSyncer(realmFactory, blockDiscover, stateManager, peerGroup)
    }

    fun start() {
        initialSyncer.sync()
    }

    private fun getRealmConfig(): RealmConfiguration {
        return RealmConfiguration.Builder()
                .name("kit")
                .deleteRealmIfMigrationNeeded()
                .modules(WalletKitModule())
                .build()
    }

    companion object {
        fun init(context: Context) {
            Realm.init(context)
        }
    }
}
