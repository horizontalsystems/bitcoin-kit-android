package io.horizontalsystems.bitcoinkit.managers

import io.horizontalsystems.bitcoinkit.core.RealmFactory
import io.horizontalsystems.bitcoinkit.models.KitState
import io.horizontalsystems.bitcoinkit.network.Network
import io.horizontalsystems.bitcoinkit.network.RegTest
import io.realm.Realm

class StateManager(private val realmFactory: RealmFactory, private val network: Network, newWallet: Boolean) {

    var apiSynced: Boolean
        get() {
            if (network is RegTest) {
                return true
            }

            return realmFactory.realm.use {
                getKitState(it).apiSynced
            }
        }
        set(value) {
            setKitState { kitState ->
                kitState.apiSynced = value
            }
        }

    init {
        // No need to sync from API for new wallets
        apiSynced = newWallet
    }

    private fun getKitState(realm: Realm): KitState {
        return realm.where(KitState::class.java).findFirst() ?: KitState()
    }

    private fun setKitState(setMethod: (KitState) -> Unit) {
        realmFactory.realm.use { realm ->
            val kitState = realm.where(KitState::class.java).findFirst() ?: KitState()

            realm.executeTransaction {
                setMethod(kitState)
                it.insertOrUpdate(kitState)
            }
        }
    }

}
