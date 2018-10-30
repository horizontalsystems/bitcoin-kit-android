package io.horizontalsystems.bitcoinkit.managers

import io.horizontalsystems.bitcoinkit.core.RealmFactory
import io.horizontalsystems.bitcoinkit.models.KitState
import io.realm.Realm

class StateManager(private val realmFactory: RealmFactory) {

    var apiSynced: Boolean
        get() = realmFactory.realm.use {
            getKitState(it).apiSynced
        }
        set(value) {
            setKitState { kitState ->
                kitState.apiSynced = value
            }
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
