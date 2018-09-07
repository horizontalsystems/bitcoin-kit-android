package bitcoin.wallet.kit.headers

import bitcoin.wallet.kit.core.RealmFactory
import bitcoin.wallet.kit.network.NetworkParameters
import bitcoin.wallet.kit.network.PeerGroup

class HeaderSyncer(private val realmFactory: RealmFactory, private val peerGroup: PeerGroup, val network: NetworkParameters) {

    fun sync() {
    }
}
