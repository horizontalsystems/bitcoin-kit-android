package bitcoin.wallet.kit.core

import io.realm.Realm
import io.realm.RealmConfiguration

class RealmFactory(private val configuration: RealmConfiguration) {

    val realm: Realm
        get() = Realm.getInstance(configuration)

}
