package bitcoin.wallet.kit.managers

import bitcoin.wallet.kit.core.RealmFactory

class StateManager(private val realmFactory: RealmFactory) {
    // todo
    var apiSynced: Boolean = false
}
