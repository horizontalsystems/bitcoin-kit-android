package io.horizontalsystems.bitcoinkit.models

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class KitState() : RealmObject() {

    @PrimaryKey
    var uniqueStubField = ""

    var apiSynced = false

}
