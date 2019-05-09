package io.horizontalsystems.bitcoincore.managers

import io.horizontalsystems.bitcoincore.core.IStorage
import io.horizontalsystems.bitcoincore.network.Network

class StateManager(private val storage: IStorage, private val network: Network, private val newWallet: Boolean) {

    var restored: Boolean
        get() {
            if (!network.syncableFromApi) {
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
