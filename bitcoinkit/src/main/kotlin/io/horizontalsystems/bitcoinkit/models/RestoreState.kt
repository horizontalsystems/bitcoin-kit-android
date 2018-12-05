package io.horizontalsystems.bitcoinkit.models

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class RestoreState : RealmObject() {

    @PrimaryKey
    var uniqueStubField = ""

    var restored = false

}
