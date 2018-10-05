package bitcoin.wallet.kit.models

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class KitState() : RealmObject() {

    @PrimaryKey
    var uniqueStubField = ""

    var apiSynced = false

}
