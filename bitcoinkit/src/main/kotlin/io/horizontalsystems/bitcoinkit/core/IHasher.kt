package io.horizontalsystems.bitcoinkit.core

interface IHasher {
    fun hash(data: ByteArray) : ByteArray
}
