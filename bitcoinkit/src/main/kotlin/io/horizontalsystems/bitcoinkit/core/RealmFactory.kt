package io.horizontalsystems.bitcoinkit.core

import io.horizontalsystems.bitcoinkit.BitcoinKitModule
import io.realm.Realm
import io.realm.RealmConfiguration

class RealmFactory(databaseName: String) {

    private val configuration = RealmConfiguration.Builder()
            .name(databaseName)
            .deleteRealmIfMigrationNeeded()
            .modules(BitcoinKitModule())
            .build()

    val realm: Realm
        get() = Realm.getInstance(configuration)

}
