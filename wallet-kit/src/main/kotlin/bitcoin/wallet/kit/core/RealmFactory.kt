package bitcoin.wallet.kit.core

import bitcoin.wallet.kit.WalletKitModule
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
