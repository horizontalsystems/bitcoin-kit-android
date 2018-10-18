package io.horizontalsystems.bitcoinkit.managers

import io.horizontalsystems.bitcoinkit.core.RealmFactory
import io.horizontalsystems.bitcoinkit.models.KitState

class StateManager(private val realmFactory: RealmFactory) {

    var apiSynced: Boolean
        get() {
            return getKitState().apiSynced
        }
        set(value) {
            setKitState { kitState ->
                kitState.apiSynced = value
            }
        }

    private fun getKitState(): KitState {
        return realmFactory.realm.where(KitState::class.java).findFirst() ?: KitState()
    }

    private fun setKitState(setMethod: (KitState) -> Unit) {
        val realm = realmFactory.realm
        val kitState = realmFactory.realm.where(KitState::class.java).findFirst() ?: KitState()

        realm.executeTransaction {
            setMethod(kitState)
            it.insertOrUpdate(kitState)
        }

        realm.close()
    }

}
