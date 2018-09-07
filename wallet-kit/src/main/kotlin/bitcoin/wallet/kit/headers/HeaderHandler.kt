package bitcoin.wallet.kit.headers

import bitcoin.wallet.kit.core.RealmFactory
import bitcoin.wallet.kit.models.Header
import bitcoin.wallet.kit.network.NetworkParameters

class HeaderHandler(private val realmFactory: RealmFactory, private val network: NetworkParameters) {

    fun handle(headers: Array<Header>) {

    }
}
