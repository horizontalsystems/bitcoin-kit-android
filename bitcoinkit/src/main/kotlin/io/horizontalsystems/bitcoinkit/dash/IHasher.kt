package io.horizontalsystems.bitcoinkit.dash

interface IHasher {
    fun hash(data: ByteArray) : ByteArray
}
