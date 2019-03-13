package io.horizontalsystems.bitcoinkit.managers

import io.horizontalsystems.bitcoinkit.core.IStorage
import io.horizontalsystems.bitcoinkit.network.Network
import io.horizontalsystems.bitcoinkit.network.RegTest

class StateManager(private val storage: IStorage, private val network: Network, private val newWallet: Boolean) {

    var restored: Boolean
        get() {
            if (network is RegTest) {
                return true
            }

            if (newWallet) {
                return true
            }

            return storage.initialRestored ?: false
        }
        set(value) {
            storage.setInitialRestored(value)
        }
}
