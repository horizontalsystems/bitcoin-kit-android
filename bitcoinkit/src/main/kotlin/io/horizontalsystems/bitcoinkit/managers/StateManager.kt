package io.horizontalsystems.bitcoinkit.managers

import io.horizontalsystems.bitcoinkit.core.RealmFactory
import io.horizontalsystems.bitcoinkit.models.RestoreState
import io.horizontalsystems.bitcoinkit.network.Network
import io.horizontalsystems.bitcoinkit.network.RegTest
import io.realm.Realm

class StateManager(private val realmFactory: RealmFactory, private val network: Network, newWallet: Boolean) {

    var restored: Boolean
        get() {
            if (network is RegTest) {
                return true
            }

            return realmFactory.realm.use {
                getRestoreState(it).restored
            }
        }
        set(value) {
            setRestoreState { kitState ->
                kitState.restored = value
            }
        }

    init {
        //  No need to restore from API for new wallets
        if (newWallet) {
            restored = true
        }
    }

    private fun getRestoreState(realm: Realm): RestoreState {
        return realm.where(RestoreState::class.java).findFirst() ?: RestoreState()
    }

    private fun setRestoreState(setMethod: (RestoreState) -> Unit) {
        realmFactory.realm.use { realm ->
            val restoreState = realm.where(RestoreState::class.java).findFirst() ?: RestoreState()

            realm.executeTransaction {
                setMethod(restoreState)
                it.insertOrUpdate(restoreState)
            }
        }
    }

}
