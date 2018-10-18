package io.horizontalsystems.bitcoinkit.core

import io.horizontalsystems.bitcoinkit.WalletKitModule
import io.realm.Realm
import io.realm.RealmConfiguration

class RealmFactory(databaseName: String) {

    private val configuration = RealmConfiguration.Builder()
            .name(databaseName)
            .deleteRealmIfMigrationNeeded()
            .modules(WalletKitModule())
            .build()

    val realm: Realm
        get() = Realm.getInstance(configuration)

}
